"""
WebSocket客户端实现

支持STOMP协议的WebSocket通信，用于与服务器进行实时双向通信。
包括连接管理、消息处理、心跳机制等功能。

基于WebSocket协议文档-中心化实现.md的规范实现。
"""

import json
import time
import threading
import logging
from typing import Dict, Any, Optional, Callable
from enum import Enum

# 可选依赖：websocket-client
try:
    import websocket
    WEBSOCKET_AVAILABLE = True
except ImportError:
    WEBSOCKET_AVAILABLE = False


class ConnectionStatus(Enum):
    """连接状态枚举"""
    DISCONNECTED = "DISCONNECTED"
    CONNECTING = "CONNECTING"
    CONNECTED = "CONNECTED"
    RECONNECTING = "RECONNECTING"
    ERROR = "ERROR"


class MessageType(Enum):
    """消息类型枚举"""
    # 连接管理
    CONNECT = "CONNECT"
    CONNECT_ACK = "CONNECT_ACK"
    
    # 心跳
    HEARTBEAT = "HEARTBEAT"
    HEARTBEAT_ACK = "HEARTBEAT_ACK"
    
    # 虚拟机控制
    VM_START = "VM_START"
    VM_STOP = "VM_STOP"
    
    # 学习控制
    TRAINING_START = "TRAINING_START"
    TRAINING_STOP = "TRAINING_STOP"
    TRAINING_PROGRESS = "TRAINING_PROGRESS"
    
    # 模型传输
    MODEL_UPLOAD = "MODEL_UPLOAD"
    MODEL_DOWNLOAD = "MODEL_DOWNLOAD"
    
    # 状态查询
    STATUS_QUERY = "STATUS_QUERY"
    STATUS_RESPONSE = "STATUS_RESPONSE"
    
    # 训练数据同步
    DATASET_CREATE = "DATASET_CREATE"
    DATASET_APPEND_ROWS = "DATASET_APPEND_ROWS"
    DATASET_COMPLETE = "DATASET_COMPLETE"
    DATASET_DELETE = "DATASET_DELETE"
    
    # 错误处理
    ERROR = "ERROR"


class WebSocketClient:
    """
    WebSocket客户端
    
    实现STOMP协议的WebSocket通信，支持：
    - 自动连接和重连
    - 心跳机制
    - 消息路由和处理
    - 错误处理和恢复
    """
    
    def __init__(self, vm_id: str, websocket_url: str, access_token: str):
        """初始化WebSocket客户端
        
        Args:
            vm_id: 虚拟机ID
            websocket_url: WebSocket连接URL
            access_token: 访问令牌
        """
        if not WEBSOCKET_AVAILABLE:
            raise ImportError("WebSocket客户端需要安装websocket-client库: pip install websocket-client")
        
        self.vm_id = vm_id
        self.websocket_url = websocket_url
        self.access_token = access_token
        
        # 连接状态
        self.status = ConnectionStatus.DISCONNECTED
        self.ws = None
        self.session_id = None
        
        # 消息处理
        self.message_handlers: Dict[MessageType, Callable] = {}
        self.message_id_counter = 0
        
        # 心跳机制
        self.heartbeat_interval = 30  # 秒
        self.heartbeat_thread = None
        self.last_heartbeat_time = 0
        
        # 重连机制
        self.max_reconnect_attempts = 10
        self.reconnect_delay = 1  # 初始重连延迟
        self.reconnect_count = 0
        
        # 日志
        self.logger = logging.getLogger(f"WSClient-{vm_id}")
        
        # 线程锁
        self._lock = threading.Lock()
    
    def connect(self) -> bool:
        """建立WebSocket连接
        
        Returns:
            bool: 连接是否成功
        """
        if self.status == ConnectionStatus.CONNECTED:
            self.logger.warning("WebSocket已经连接")
            return True
        
        try:
            self.status = ConnectionStatus.CONNECTING
            self.logger.info(f"正在连接WebSocket: {self.websocket_url}")
            
            # 创建WebSocket连接
            self.ws = websocket.WebSocketApp(
                self.websocket_url,
                on_open=self._on_open,
                on_message=self._on_message,
                on_error=self._on_error,
                on_close=self._on_close
            )
            
            # 在后台线程中运行连接
            self.ws_thread = threading.Thread(
                target=self.ws.run_forever,
                kwargs={"ping_interval": 25, "ping_timeout": 10}
            )
            self.ws_thread.daemon = True
            self.ws_thread.start()
            
            # 等待连接建立（最多等待10秒）
            for _ in range(100):
                if self.status == ConnectionStatus.CONNECTED:
                    return True
                elif self.status == ConnectionStatus.ERROR:
                    return False
                time.sleep(0.1)
            
            self.logger.error("WebSocket连接超时")
            return False
            
        except Exception as e:
            self.logger.error(f"WebSocket连接失败: {e}")
            self.status = ConnectionStatus.ERROR
            return False
    
    def disconnect(self):
        """断开WebSocket连接"""
        with self._lock:
            if self.ws:
                self.ws.close()
            
            if self.heartbeat_thread:
                self.heartbeat_thread = None
            
            self.status = ConnectionStatus.DISCONNECTED
            self.logger.info("WebSocket连接已断开")
    
    def send_message(self, message_type: MessageType, data: Dict[str, Any]) -> bool:
        """发送消息
        
        Args:
            message_type: 消息类型
            data: 消息数据
            
        Returns:
            bool: 发送是否成功
        """
        if self.status != ConnectionStatus.CONNECTED:
            self.logger.error("WebSocket未连接，无法发送消息")
            return False
        
        try:
            message = {
                "type": message_type.value,
                "id": self._generate_message_id(),
                "timestamp": self._get_timestamp(),
                "vmId": self.vm_id,
                "data": data,
                "signature": ""  # TODO: 实现消息签名
            }
            
            message_json = json.dumps(message, ensure_ascii=False)
            self.ws.send(message_json)
            
            self.logger.debug(f"发送消息: {message_type.value}")
            return True
            
        except Exception as e:
            self.logger.error(f"发送消息失败: {e}")
            return False
    
    def register_message_handler(self, message_type: MessageType, handler: Callable):
        """注册消息处理器
        
        Args:
            message_type: 消息类型
            handler: 处理函数
        """
        self.message_handlers[message_type] = handler
        self.logger.debug(f"注册消息处理器: {message_type.value}")
    
    def _on_open(self, ws):
        """WebSocket连接打开回调"""
        self.logger.info("WebSocket连接已建立")
        
        # 发送STOMP CONNECT帧
        connect_frame = self._build_stomp_connect_frame()
        ws.send(connect_frame)
    
    def _on_message(self, ws, message):
        """WebSocket消息接收回调"""
        try:
            # 检查是否是STOMP帧
            if message.startswith("CONNECTED"):
                self._handle_stomp_connected(message)
                return
            
            # 解析JSON消息
            try:
                msg_data = json.loads(message)
                self._handle_json_message(msg_data)
            except json.JSONDecodeError:
                self.logger.warning(f"收到非JSON消息: {message}")
                
        except Exception as e:
            self.logger.error(f"处理消息失败: {e}")
    
    def _on_error(self, ws, error):
        """WebSocket错误回调"""
        self.logger.error(f"WebSocket错误: {error}")
        self.status = ConnectionStatus.ERROR
    
    def _on_close(self, ws, close_status_code, close_msg):
        """WebSocket连接关闭回调"""
        self.logger.info(f"WebSocket连接关闭: {close_status_code} - {close_msg}")
        self.status = ConnectionStatus.DISCONNECTED
        
        # 启动重连
        if self.reconnect_count < self.max_reconnect_attempts:
            self._schedule_reconnect()
    
    def _build_stomp_connect_frame(self) -> str:
        """构建STOMP CONNECT帧"""
        headers = [
            "accept-version:1.2",
            "host:localhost",
            f"Authorization:Bearer {self.access_token}",
            f"vmId:{self.vm_id}"
        ]
        return "CONNECT\n" + "\n".join(headers) + "\n\n\0"
    
    def _handle_stomp_connected(self, message: str):
        """处理STOMP CONNECTED响应"""
        self.logger.info("STOMP连接已建立")
        self.status = ConnectionStatus.CONNECTED
        self.reconnect_count = 0  # 重置重连计数
        
        # 发送应用层CONNECT消息
        self._send_application_connect()
        
        # 启动心跳
        self._start_heartbeat()
    
    def _handle_json_message(self, message: Dict[str, Any]):
        """处理JSON格式的应用层消息"""
        msg_type_str = message.get("type")
        if not msg_type_str:
            self.logger.warning("消息缺少type字段")
            return
        
        try:
            msg_type = MessageType(msg_type_str)
            
            # 特殊处理心跳ACK
            if msg_type == MessageType.HEARTBEAT_ACK:
                self.last_heartbeat_time = time.time()
                return
            
            # 调用注册的处理器
            if msg_type in self.message_handlers:
                self.message_handlers[msg_type](message)
            else:
                self.logger.warning(f"未注册的消息类型: {msg_type_str}")
                
        except ValueError:
            self.logger.warning(f"未知的消息类型: {msg_type_str}")
    
    def _send_application_connect(self):
        """发送应用层CONNECT消息"""
        connect_data = {
            "version": "1.0.0",
            "capabilities": ["FEDAVG", "FEDPROX", "FEDNOVA", "SCAFFOLD"],
            "systemInfo": {
                "os": "Unknown",
                "python": "3.8+",
                "memory": "Unknown",
                "cpu": "Unknown"
            }
        }
        
        self.send_message(MessageType.CONNECT, connect_data)
    
    def _start_heartbeat(self):
        """启动心跳线程"""
        if self.heartbeat_thread and self.heartbeat_thread.is_alive():
            return
        
        self.heartbeat_thread = threading.Thread(target=self._heartbeat_loop)
        self.heartbeat_thread.daemon = True
        self.heartbeat_thread.start()
        self.logger.debug("心跳线程已启动")
    
    def _heartbeat_loop(self):
        """心跳循环"""
        while self.status == ConnectionStatus.CONNECTED:
            try:
                # 发送心跳
                heartbeat_data = {
                    "status": "IDLE",
                    "resourceUsage": {
                        "cpu": 0.0,
                        "memory": 0.0,
                        "disk": 0.0,
                        "gpu": 0.0
                    }
                }
                
                if self.send_message(MessageType.HEARTBEAT, heartbeat_data):
                    self.logger.debug("发送心跳")
                
                # 等待心跳间隔
                time.sleep(self.heartbeat_interval)
                
            except Exception as e:
                self.logger.error(f"心跳发送失败: {e}")
                break
    
    def _schedule_reconnect(self):
        """安排重连"""
        if self.reconnect_count >= self.max_reconnect_attempts:
            self.logger.error("达到最大重连次数，停止重连")
            return
        
        self.status = ConnectionStatus.RECONNECTING
        self.reconnect_count += 1
        
        # 指数退避
        delay = min(self.reconnect_delay * (2 ** (self.reconnect_count - 1)), 60)
        
        self.logger.info(f"将在{delay}秒后进行第{self.reconnect_count}次重连")
        
        def reconnect():
            time.sleep(delay)
            if self.status == ConnectionStatus.RECONNECTING:
                self.connect()
        
        reconnect_thread = threading.Thread(target=reconnect)
        reconnect_thread.daemon = True
        reconnect_thread.start()
    
    def _generate_message_id(self) -> str:
        """生成消息ID"""
        self.message_id_counter += 1
        timestamp = int(time.time() * 1000)
        return f"client-{timestamp}-{self.message_id_counter}"
    
    def _get_timestamp(self) -> str:
        """获取当前时间戳"""
        return time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
    
    def get_status(self) -> Dict[str, Any]:
        """获取客户端状态
        
        Returns:
            dict: 状态信息
        """
        return {
            "vm_id": self.vm_id,
            "status": self.status.value,
            "session_id": self.session_id,
            "reconnect_count": self.reconnect_count,
            "last_heartbeat": self.last_heartbeat_time,
            "websocket_url": self.websocket_url
        }
