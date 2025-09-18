"""
虚拟机HTTP API客户端
"""

import requests
from dataclasses import asdict
from .exceptions import parse_vm_api_response
from ..federated.config import VMRegisterRequest


class VMApiClient:
    """虚拟机API客户端"""
    
    def __init__(self, base_url: str, jwt_token: str):
        """初始化API客户端
        
        Args:
            base_url: API基础URL
            jwt_token: JWT认证令牌
        """
        self.base_url = base_url.rstrip("/")
        self.jwt_token = jwt_token

    def _headers(self):
        """获取请求头"""
        return {
            "Authorization": f"Bearer {self.jwt_token}",
            "Content-Type": "application/json"
        }

    def register_vm(self, vm: VMRegisterRequest):
        """注册虚拟机
        
        Args:
            vm: 虚拟机注册请求对象
            
        Returns:
            dict: 注册响应数据
            
        Raises:
            VMApiError: API调用失败时抛出
        """
        url = f"{self.base_url}/api/v1/vm/register"
        resp = requests.post(url, json=asdict(vm), headers=self._headers())
        return parse_vm_api_response(resp.json())

    def refresh_token(self, vm_id: str, secret_id: str):
        """刷新Token
        
        Args:
            vm_id: 虚拟机ID
            secret_id: 长期刷新凭证
            
        Returns:
            dict: Token刷新响应数据
            
        Raises:
            VMApiError: API调用失败时抛出
        """
        url = f"{self.base_url}/api/v1/vm/token/refresh"
        data = {"vmId": vm_id, "secretId": secret_id}
        resp = requests.post(url, json=data, headers=self._headers())
        return parse_vm_api_response(resp.json())

