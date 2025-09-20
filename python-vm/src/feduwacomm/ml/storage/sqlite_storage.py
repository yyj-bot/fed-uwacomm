"""
SQLite本地存储模块，用于存储虚拟机配置和认证信息
"""

import sqlite3
import json
import logging
from pathlib import Path
from typing import Optional, Dict, Any
from datetime import datetime


class VMStorage:
    """虚拟机本地存储管理器"""
    
    def __init__(self, db_path: str = "vm_storage.db"):
        """初始化存储管理器
        
        Args:
            db_path: 数据库文件路径
        """
        self.db_path = db_path
        self.logger = logging.getLogger("VMStorage")
        self._init_database()
    
    def _init_database(self):
        """初始化数据库表结构"""
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                # 创建虚拟机认证信息表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS vm_auth (
                        vm_id TEXT PRIMARY KEY,
                        secret_id TEXT NOT NULL,
                        access_token TEXT,
                        token_expire_time INTEGER,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                
                # 创建虚拟机配置信息表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS vm_config (
                        vm_id TEXT PRIMARY KEY,
                        config_data TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                
                # 创建WebSocket连接信息表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS vm_connection (
                        vm_id TEXT PRIMARY KEY,
                        websocket_url TEXT,
                        connection_status TEXT DEFAULT 'DISCONNECTED',
                        last_heartbeat TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                
                conn.commit()
                self.logger.info("数据库初始化完成")
                
        except Exception as e:
            self.logger.error(f"数据库初始化失败: {e}")
            raise
    
    def save_secret_id(self, vm_id: str, secret_id: str, access_token: str = None, 
                      token_expire_seconds: int = None) -> bool:
        """保存虚拟机的secretId和访问令牌
        
        Args:
            vm_id: 虚拟机ID
            secret_id: 长期刷新凭证
            access_token: 访问令牌
            token_expire_seconds: 令牌过期时间（秒）
            
        Returns:
            bool: 保存是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                # 计算令牌过期时间戳
                token_expire_time = None
                if token_expire_seconds:
                    token_expire_time = int(datetime.now().timestamp()) + token_expire_seconds
                
                cursor.execute("""
                    INSERT OR REPLACE INTO vm_auth 
                    (vm_id, secret_id, access_token, token_expire_time, updated_at)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, (vm_id, secret_id, access_token, token_expire_time))
                
                conn.commit()
                self.logger.info(f"已保存虚拟机 {vm_id} 的认证信息")
                return True
                
        except Exception as e:
            self.logger.error(f"保存认证信息失败: {e}")
            return False
    
    def get_secret_id(self, vm_id: str) -> Optional[str]:
        """获取虚拟机的secretId
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            str: secretId，如果不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT secret_id FROM vm_auth WHERE vm_id = ?", (vm_id,))
                result = cursor.fetchone()
                return result[0] if result else None
                
        except Exception as e:
            self.logger.error(f"获取secretId失败: {e}")
            return None
    
    def get_access_token(self, vm_id: str) -> Optional[Dict[str, Any]]:
        """获取虚拟机的访问令牌信息
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            dict: 包含access_token和过期时间的字典，如果不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT access_token, token_expire_time 
                    FROM vm_auth WHERE vm_id = ?
                """, (vm_id,))
                result = cursor.fetchone()
                
                if result:
                    access_token, expire_time = result
                    return {
                        'access_token': access_token,
                        'expire_time': expire_time,
                        'is_expired': expire_time and expire_time < datetime.now().timestamp()
                    }
                return None
                
        except Exception as e:
            self.logger.error(f"获取访问令牌失败: {e}")
            return None
    
    def update_access_token(self, vm_id: str, access_token: str, 
                           token_expire_seconds: int) -> bool:
        """更新访问令牌
        
        Args:
            vm_id: 虚拟机ID
            access_token: 新的访问令牌
            token_expire_seconds: 令牌过期时间（秒）
            
        Returns:
            bool: 更新是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                token_expire_time = int(datetime.now().timestamp()) + token_expire_seconds
                
                cursor.execute("""
                    UPDATE vm_auth 
                    SET access_token = ?, token_expire_time = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE vm_id = ?
                """, (access_token, token_expire_time, vm_id))
                
                conn.commit()
                self.logger.info(f"已更新虚拟机 {vm_id} 的访问令牌")
                return cursor.rowcount > 0
                
        except Exception as e:
            self.logger.error(f"更新访问令牌失败: {e}")
            return False
    
    def save_vm_config(self, vm_id: str, config_data: Dict[str, Any]) -> bool:
        """保存虚拟机配置信息
        
        Args:
            vm_id: 虚拟机ID
            config_data: 配置数据字典
            
        Returns:
            bool: 保存是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                config_json = json.dumps(config_data, ensure_ascii=False, indent=2)
                
                cursor.execute("""
                    INSERT OR REPLACE INTO vm_config 
                    (vm_id, config_data, updated_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                """, (vm_id, config_json))
                
                conn.commit()
                self.logger.info(f"已保存虚拟机 {vm_id} 的配置信息")
                return True
                
        except Exception as e:
            self.logger.error(f"保存配置信息失败: {e}")
            return False
    
    def get_vm_config(self, vm_id: str) -> Optional[Dict[str, Any]]:
        """获取虚拟机配置信息
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            dict: 配置数据字典，如果不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT config_data FROM vm_config WHERE vm_id = ?", (vm_id,))
                result = cursor.fetchone()
                
                if result:
                    return json.loads(result[0])
                return None
                
        except Exception as e:
            self.logger.error(f"获取配置信息失败: {e}")
            return None
    
    def save_connection_info(self, vm_id: str, websocket_url: str, 
                           connection_status: str = "DISCONNECTED") -> bool:
        """保存WebSocket连接信息
        
        Args:
            vm_id: 虚拟机ID
            websocket_url: WebSocket连接URL
            connection_status: 连接状态
            
        Returns:
            bool: 保存是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                cursor.execute("""
                    INSERT OR REPLACE INTO vm_connection 
                    (vm_id, websocket_url, connection_status, updated_at)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """, (vm_id, websocket_url, connection_status))
                
                conn.commit()
                self.logger.info(f"已保存虚拟机 {vm_id} 的连接信息")
                return True
                
        except Exception as e:
            self.logger.error(f"保存连接信息失败: {e}")
            return False
    
    def update_connection_status(self, vm_id: str, status: str) -> bool:
        """更新连接状态
        
        Args:
            vm_id: 虚拟机ID
            status: 连接状态
            
        Returns:
            bool: 更新是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                cursor.execute("""
                    UPDATE vm_connection 
                    SET connection_status = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE vm_id = ?
                """, (status, vm_id))
                
                conn.commit()
                return cursor.rowcount > 0
                
        except Exception as e:
            self.logger.error(f"更新连接状态失败: {e}")
            return False
    
    def update_heartbeat(self, vm_id: str) -> bool:
        """更新心跳时间
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            bool: 更新是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                cursor.execute("""
                    UPDATE vm_connection 
                    SET last_heartbeat = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                    WHERE vm_id = ?
                """, (vm_id,))
                
                conn.commit()
                return cursor.rowcount > 0
                
        except Exception as e:
            self.logger.error(f"更新心跳时间失败: {e}")
            return False
    
    def get_connection_info(self, vm_id: str) -> Optional[Dict[str, Any]]:
        """获取连接信息
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            dict: 连接信息字典，如果不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT websocket_url, connection_status, last_heartbeat
                    FROM vm_connection WHERE vm_id = ?
                """, (vm_id,))
                result = cursor.fetchone()
                
                if result:
                    websocket_url, connection_status, last_heartbeat = result
                    return {
                        'websocket_url': websocket_url,
                        'connection_status': connection_status,
                        'last_heartbeat': last_heartbeat
                    }
                return None
                
        except Exception as e:
            self.logger.error(f"获取连接信息失败: {e}")
            return None
    
    def cleanup_expired_tokens(self) -> int:
        """清理过期的访问令牌
        
        Returns:
            int: 清理的记录数量
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                current_time = int(datetime.now().timestamp())
                cursor.execute("""
                    UPDATE vm_auth 
                    SET access_token = NULL, token_expire_time = NULL
                    WHERE token_expire_time IS NOT NULL AND token_expire_time < ?
                """, (current_time,))
                
                conn.commit()
                cleaned_count = cursor.rowcount
                
                if cleaned_count > 0:
                    self.logger.info(f"清理了 {cleaned_count} 个过期的访问令牌")
                
                return cleaned_count
                
        except Exception as e:
            self.logger.error(f"清理过期令牌失败: {e}")
            return 0

