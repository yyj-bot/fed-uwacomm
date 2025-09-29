"""
虚拟机API模块

提供HTTP和WebSocket API客户端功能
"""

from .vm_api_client import VMApiClient
from .exceptions import VMApiError, VMApiErrorCode, parse_vm_api_response
from ..websocket.client import WebSocketClient, ConnectionStatus

__all__ = [
    'VMApiClient',
    'VMApiError', 
    'VMApiErrorCode',
    'parse_vm_api_response',
    'WebSocketClient',
    'ConnectionStatus'
]

