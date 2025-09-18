"""
主虚拟机客户端 - 整合所有功能的统一入口
"""

import logging
from typing import Optional, Dict, Any
from pathlib import Path

from .api.vm_api_client import VMApiClient
from .api.websocket_client import WebSocketClient
from .api.exceptions import VMApiError
from .federated.config import VMRegisterRequest
from .federated.client import FederatedLearningClient
from .storage.sqlite_storage import VMStorage


class VMClient:
    """主虚拟机客户端
    
    整合HTTP API客户端、WebSocket客户端、联邦学习客户端和本地存储功能。
    提供统一的接口来管理虚拟机的注册、认证、通信和学习功能。
    """
    
    def __init__(self, vm_id: str, base_url: str, storage_path: str = None):
        """初始化虚拟机客户端
        
        Args:
            vm_id: 虚拟机唯一标识
            base_url: API服务器基础URL
            storage_path: 本地存储数据库路径
        """
        self.vm_id = vm_id
        self.base_url = base_url
        
        # 初始化存储
        if storage_path is None:
            storage_path = f"vm_{vm_id}_storage.db"
        self.storage = VMStorage(storage_path)
        
        # 初始化日志
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(f"VMClient-{vm_id}")
        
        # API客户端（需要token才能初始化）
        self.api_client = None
        
        # 联邦学习客户端（按需创建）
        self.federated_client = None
        
        # WebSocket客户端（按需创建）
        self.websocket_client = None
        
        self.logger.info(f"虚拟机客户端初始化完成: {vm_id}")
    
    def register(self, register_request: VMRegisterRequest) -> bool:
        """注册虚拟机到服务器
        
        Args:
            register_request: 虚拟机注册请求
            
        Returns:
            bool: 注册是否成功
        """
        try:
            # 创建临时API客户端用于注册（无需token）
            temp_client = VMApiClient(self.base_url, "")
            temp_client.jwt_token = ""  # 注册接口不需要token
            
            # 发起注册请求
            response = temp_client.register_vm(register_request)
            
            # 提取注册响应中的重要信息
            access_token = response.get('accessToken')
            secret_id = response.get('secretId')
            token_expire_seconds = response.get('tokenExpireSeconds', 86400)
            websocket_info = response.get('websocket', {})
            
            if not access_token or not secret_id:
                self.logger.error("注册响应中缺少必要的认证信息")
                return False
            
            # 保存认证信息到本地存储
            if not self.storage.save_secret_id(
                self.vm_id, secret_id, access_token, token_expire_seconds
            ):
                self.logger.error("保存认证信息失败")
                return False
            
            # 保存WebSocket连接信息
            websocket_url = websocket_info.get('native', '')
            if websocket_url:
                self.storage.save_connection_info(self.vm_id, websocket_url)
            
            # 保存完整的注册响应作为配置
            self.storage.save_vm_config(self.vm_id, response)
            
            # 初始化API客户端
            self.api_client = VMApiClient(self.base_url, access_token)
            
            self.logger.info(f"虚拟机注册成功: {self.vm_id}")
            return True
            
        except VMApiError as e:
            self.logger.error(f"虚拟机注册失败: {e}")
            return False
        except Exception as e:
            self.logger.error(f"虚拟机注册异常: {e}")
            return False
    
    def refresh_token(self) -> bool:
        """刷新访问令牌
        
        Returns:
            bool: 刷新是否成功
        """
        try:
            # 获取存储的secret_id
            secret_id = self.storage.get_secret_id(self.vm_id)
            if not secret_id:
                self.logger.error("未找到存储的secretId，请先注册")
                return False
            
            # 创建临时API客户端用于刷新token
            temp_client = VMApiClient(self.base_url, "")
            
            # 发起token刷新请求
            response = temp_client.refresh_token(self.vm_id, secret_id)
            
            # 提取新的认证信息
            new_access_token = response.get('accessToken')
            new_secret_id = response.get('secretId')  # 可能有新的secretId
            token_expire_seconds = response.get('tokenExpireSeconds', 86400)
            
            if not new_access_token:
                self.logger.error("Token刷新响应中缺少accessToken")
                return False
            
            # 更新存储的认证信息
            secret_to_save = new_secret_id if new_secret_id else secret_id
            if not self.storage.save_secret_id(
                self.vm_id, secret_to_save, new_access_token, token_expire_seconds
            ):
                self.logger.error("保存新的认证信息失败")
                return False
            
            # 更新API客户端的token
            if self.api_client:
                self.api_client.jwt_token = new_access_token
            else:
                self.api_client = VMApiClient(self.base_url, new_access_token)
            
            self.logger.info("Token刷新成功")
            return True
            
        except VMApiError as e:
            self.logger.error(f"Token刷新失败: {e}")
            return False
        except Exception as e:
            self.logger.error(f"Token刷新异常: {e}")
            return False
    
    def ensure_valid_token(self) -> bool:
        """确保拥有有效的访问令牌
        
        Returns:
            bool: 是否拥有有效令牌
        """
        # 检查当前token是否存在且未过期
        token_info = self.storage.get_access_token(self.vm_id)
        
        if not token_info or not token_info.get('access_token'):
            self.logger.info("未找到访问令牌，尝试刷新")
            return self.refresh_token()
        
        if token_info.get('is_expired', False):
            self.logger.info("访问令牌已过期，尝试刷新")
            return self.refresh_token()
        
        # Token有效，确保API客户端已初始化
        if not self.api_client:
            self.api_client = VMApiClient(self.base_url, token_info['access_token'])
        
        return True
    
    def create_federated_client(self, model, model_type: str = "pytorch", 
                               config=None) -> bool:
        """创建联邦学习客户端
        
        Args:
            model: 机器学习模型
            model_type: 模型类型
            config: 联邦学习配置
            
        Returns:
            bool: 创建是否成功
        """
        try:
            self.federated_client = FederatedLearningClient(
                client_id=self.vm_id,
                model=model,
                model_type=model_type,
                config=config
            )
            self.logger.info("联邦学习客户端创建成功")
            return True
            
        except Exception as e:
            self.logger.error(f"创建联邦学习客户端失败: {e}")
            return False
    
    def create_websocket_client(self) -> bool:
        """创建WebSocket客户端
        
        Returns:
            bool: 创建是否成功
        """
        try:
            # 确保有有效的token
            if not self.ensure_valid_token():
                self.logger.error("无法获取有效的访问令牌")
                return False
            
            # 获取WebSocket连接信息
            connection_info = self.get_connection_info()
            if not connection_info or not connection_info.get('websocket_url'):
                self.logger.error("未找到WebSocket连接信息")
                return False
            
            # 获取当前的访问令牌
            token_info = self.storage.get_access_token(self.vm_id)
            if not token_info:
                self.logger.error("无法获取访问令牌")
                return False
            
            # 创建WebSocket客户端
            self.websocket_client = WebSocketClient(
                vm_id=self.vm_id,
                websocket_url=connection_info['websocket_url'],
                access_token=token_info['access_token']
            )
            
            self.logger.info("WebSocket客户端创建成功")
            return True
            
        except Exception as e:
            self.logger.error(f"创建WebSocket客户端失败: {e}")
            return False
    
    def connect_websocket(self) -> bool:
        """连接WebSocket服务器
        
        Returns:
            bool: 连接是否成功
        """
        try:
            if not self.websocket_client:
                if not self.create_websocket_client():
                    return False
            
            return self.websocket_client.connect()
            
        except Exception as e:
            self.logger.error(f"WebSocket连接失败: {e}")
            return False
    
    def disconnect_websocket(self) -> bool:
        """断开WebSocket连接
        
        Returns:
            bool: 断开是否成功
        """
        try:
            if self.websocket_client:
                self.websocket_client.disconnect()
                return True
            return True
            
        except Exception as e:
            self.logger.error(f"WebSocket断开失败: {e}")
            return False
    
    def get_stored_config(self) -> Optional[Dict[str, Any]]:
        """获取存储的虚拟机配置
        
        Returns:
            dict: 配置信息，如果不存在返回None
        """
        return self.storage.get_vm_config(self.vm_id)
    
    def get_connection_info(self) -> Optional[Dict[str, Any]]:
        """获取WebSocket连接信息
        
        Returns:
            dict: 连接信息，如果不存在返回None
        """
        return self.storage.get_connection_info(self.vm_id)
    
    def cleanup_expired_tokens(self) -> int:
        """清理过期的令牌
        
        Returns:
            int: 清理的记录数量
        """
        return self.storage.cleanup_expired_tokens()
    
    def get_status(self) -> Dict[str, Any]:
        """获取虚拟机客户端状态
        
        Returns:
            dict: 状态信息
        """
        token_info = self.storage.get_access_token(self.vm_id)
        connection_info = self.storage.get_connection_info(self.vm_id)
        
        status = {
            'vm_id': self.vm_id,
            'has_secret_id': self.storage.get_secret_id(self.vm_id) is not None,
            'has_valid_token': token_info and not token_info.get('is_expired', True),
            'api_client_ready': self.api_client is not None,
            'federated_client_ready': self.federated_client is not None,
            'websocket_client_ready': self.websocket_client is not None,
            'connection_status': connection_info.get('connection_status', 'UNKNOWN') if connection_info else 'UNKNOWN'
        }
        
        return status
    
    def __str__(self) -> str:
        """字符串表示"""
        status = self.get_status()
        return f"VMClient(vm_id={self.vm_id}, token_valid={status['has_valid_token']}, api_ready={status['api_client_ready']})"

