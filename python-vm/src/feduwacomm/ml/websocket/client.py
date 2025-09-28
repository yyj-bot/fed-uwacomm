"""
FedUWAComm WebSocket 客户端 v1.4
基于中心化架构的被动响应式设计
"""

import json
import time
import threading
import logging
import base64
from typing import Dict, Any, Optional, Callable, List
from enum import Enum

# 可选依赖：websocket-client
try:
    import websocket
    WEBSOCKET_AVAILABLE = True
except ImportError:
    WEBSOCKET_AVAILABLE = False

logger = logging.getLogger(__name__)


class ConnectionStatus(Enum):
    """连接状态枚举"""
    DISCONNECTED = "DISCONNECTED"
    CONNECTING = "CONNECTING"
    CONNECTED = "CONNECTED"
    RECONNECTING = "RECONNECTING"
    ERROR = "ERROR"


class FederatedLearningClient:
    """
    联邦学习WebSocket客户端
    实现完全被动响应式的v1.4协议
    """

    def __init__(self, server_url: str, access_token: str, vm_id: str):
        """初始化客户端

        Args:
            server_url: WebSocket服务器URL
            access_token: 访问令牌
            vm_id: 虚拟机ID
        """
        if not WEBSOCKET_AVAILABLE:
            raise ImportError("WebSocket客户端需要安装websocket-client库: pip install websocket-client")
        
        self.server_url = server_url
        self.access_token = access_token
        self.vm_id = vm_id

        # 连接管理
        self.websocket = None
        self.status = ConnectionStatus.DISCONNECTED
        self.is_connected = False
        self.reconnect_attempts = 0
        self.max_reconnect_attempts = 5

        # 多任务管理
        self.active_tasks = {}  # taskId -> TaskContext
        self.max_concurrent_tasks = 3

        # 消息处理
        self.message_id_counter = 0
        self.message_lock = threading.Lock()
        self.message_router = None

        # 心跳管理
        self.heartbeat_interval = 30  # 秒
        self.heartbeat_thread = None
        self.heartbeat_stop_event = threading.Event()

        # 性能监控
        self.metrics = {
            "messages_sent": 0,
            "messages_received": 0,
            "connection_errors": 0,
            "task_failures": 0
        }

        # 启动时间
        self.start_time = time.time()

        logger.info(f"初始化FederatedLearningClient，VM ID: {vm_id}")

    def initialize(self):
        """初始化客户端组件"""
        from .message_router import MessageRouter
        self.message_router = MessageRouter(self)

    def connect(self) -> bool:
        """建立WebSocket连接"""
        try:
            self.status = ConnectionStatus.CONNECTING
            logger.info(f"正在连接到WebSocket服务器: {self.server_url}")

            # 构建连接URL
            url = f"{self.server_url}?token={self.access_token}&vmId={self.vm_id}"

            # 创建WebSocket连接
            self.websocket = websocket.WebSocketApp(
                url,
                on_open=self._on_open,
                on_message=self._on_message,
                on_error=self._on_error,
                on_close=self._on_close
            )

            # 启动连接（阻塞方式）
            self.websocket.run_forever()

            return self.is_connected

        except Exception as e:
            logger.error(f"连接WebSocket服务器失败: {e}")
            self.status = ConnectionStatus.ERROR
            self.metrics["connection_errors"] += 1
            return False

    def disconnect(self):
        """断开WebSocket连接"""
        logger.info("正在断开WebSocket连接")

        # 停止心跳
        if self.heartbeat_thread:
            self.heartbeat_stop_event.set()
            self.heartbeat_thread.join(timeout=5)

        # 清理所有任务
        for task_id in list(self.active_tasks.keys()):
            self._cleanup_task(task_id)

        # 关闭连接
        if self.websocket:
            self.websocket.close()

        self.is_connected = False
        self.status = ConnectionStatus.DISCONNECTED

    def _on_open(self, ws):
        """连接建立回调"""
        logger.info("WebSocket连接已建立")
        self.is_connected = True
        self.status = ConnectionStatus.CONNECTED
        self.reconnect_attempts = 0

        # 发送连接确认消息
        self._send_connect_message()

        # 启动心跳
        self._start_heartbeat()

    def _on_message(self, ws, message):
        """消息接收回调"""
        try:
            msg = json.loads(message)
            self.metrics["messages_received"] += 1
            self._handle_message(msg)
        except Exception as e:
            logger.error(f"处理消息失败: {e}")

    def _on_error(self, ws, error):
        """错误回调"""
        logger.error(f"WebSocket错误: {error}")
        self.status = ConnectionStatus.ERROR
        self.metrics["connection_errors"] += 1

    def _on_close(self, ws, close_status_code, close_msg):
        """连接关闭回调"""
        logger.warning(f"WebSocket连接已关闭: {close_status_code} - {close_msg}")
        self.is_connected = False
        self.status = ConnectionStatus.DISCONNECTED

        # 停止心跳
        if self.heartbeat_thread:
            self.heartbeat_stop_event.set()

        # 尝试重连
        self._attempt_reconnect()

    def _handle_message(self, message: Dict[str, Any]):
        """消息处理统一入口"""
        if self.message_router:
            self.message_router.route_message(message)
        else:
            logger.warning("消息路由器未初始化")

    def _send_connect_message(self):
        """发送连接确认消息"""
        connect_message = {
            "type": "CONNECT",
            "id": self._generate_message_id(),
            "timestamp": self._get_current_timestamp(),
            "vmId": self.vm_id,
            "data": {
                "vmId": self.vm_id,
                "capabilities": self._get_vm_capabilities()
            }
        }

        self._send_message(connect_message)

    def _get_vm_capabilities(self) -> Dict[str, Any]:
        """获取虚拟机能力信息"""
        import platform
        try:
            import psutil
            memory_gb = psutil.virtual_memory().total // (1024**3)
            cpu_cores = psutil.cpu_count()
        except ImportError:
            memory_gb = 8  # 默认值
            cpu_cores = 4  # 默认值

        return {
            "systemInfo": {
                "os": platform.system() + " " + platform.release(),
                "python": platform.python_version(),
                "memory": f"{memory_gb}GB",
                "cpu_cores": cpu_cores
            },
            "maxConcurrentTasks": self.max_concurrent_tasks,
            "supportedAlgorithms": [
                "FEDERATED_AVERAGING",
                "FEDERATED_PROXIMAL",
                "FEDERATED_NOVA",
                "SCAFFOLD"
            ],
            "supportedDataTypes": [
                "ACOUSTIC",
                "ENVIRONMENT",
                "MODEL",
                "OTHER",
                "TEST_DATA",
                "SPECIAL_CHARS",
                "LONG_TEXT"
            ]
        }

    def _send_message(self, message: Dict[str, Any]) -> bool:
        """发送WebSocket消息"""
        if not self.is_connected or not self.websocket:
            logger.warning("WebSocket未连接，无法发送消息")
            return False

        try:
            message_str = json.dumps(message, ensure_ascii=False)
            self.websocket.send(message_str)
            self.metrics["messages_sent"] += 1
            logger.debug(f"发送消息: {message['type']}")
            return True
        except Exception as e:
            logger.error(f"发送消息失败: {e}")
            return False

    def _generate_message_id(self) -> str:
        """生成消息ID"""
        with self.message_lock:
            self.message_id_counter += 1
            return f"{self.vm_id}_{int(time.time())}_{self.message_id_counter}"

    def _get_current_timestamp(self) -> int:
        """获取当前时间戳（毫秒）"""
        return int(time.time() * 1000)

    def _start_heartbeat(self):
        """启动心跳机制"""
        self.heartbeat_stop_event.clear()
        self.heartbeat_thread = threading.Thread(
            target=self._heartbeat_loop,
            daemon=True
        )
        self.heartbeat_thread.start()
        logger.info("心跳机制已启动")

    def _heartbeat_loop(self):
        """心跳循环"""
        while not self.heartbeat_stop_event.wait(self.heartbeat_interval):
            if self.is_connected:
                try:
                    heartbeat_message = {
                        "type": "HEARTBEAT",
                        "id": self._generate_message_id(),
                        "timestamp": self._get_current_timestamp(),
                        "vmId": self.vm_id,
                        "data": {
                            "status": "ACTIVE",
                            "resourceUsage": self._get_resource_usage(),
                            "activeTasks": self._get_active_tasks_status(),
                            "metrics": self._get_performance_metrics()
                        }
                    }

                    self._send_message(heartbeat_message)
                    logger.debug("发送心跳消息")

                except Exception as e:
                    logger.error(f"发送心跳失败: {e}")

        logger.info("心跳循环已停止")

    def _get_resource_usage(self) -> Dict[str, Any]:
        """获取资源使用情况"""
        try:
            import psutil
            return {
                "cpu_percent": psutil.cpu_percent(interval=1),
                "memory_percent": psutil.virtual_memory().percent,
                "disk_usage": psutil.disk_usage('/').percent,
                "network_io": {
                    "bytes_sent": psutil.net_io_counters().bytes_sent,
                    "bytes_recv": psutil.net_io_counters().bytes_recv
                },
                "load_average": psutil.getloadavg() if hasattr(psutil, 'getloadavg') else [0, 0, 0]
            }
        except ImportError:
            return {
                "cpu_percent": 0.0,
                "memory_percent": 0.0,
                "disk_usage": 0.0,
                "network_io": {"bytes_sent": 0, "bytes_recv": 0},
                "load_average": [0, 0, 0]
            }

    def _get_active_tasks_status(self) -> List[Dict[str, Any]]:
        """获取所有活跃任务状态"""
        task_status = []
        for task_id, task_context in self.active_tasks.items():
            task_status.append({
                "taskId": task_id,
                "status": task_context.status,
                "currentRound": task_context.current_round,
                "progress": task_context.progress,
                "lastActivity": task_context.last_activity,
                "errorCount": getattr(task_context, 'error_count', 0),
                "resourceUsage": task_context.get_resource_usage() if hasattr(task_context, 'get_resource_usage') else {}
            })
        return task_status

    def _get_performance_metrics(self) -> Dict[str, Any]:
        """获取性能指标"""
        return {
            "messages_sent": self.metrics["messages_sent"],
            "messages_received": self.metrics["messages_received"],
            "connection_errors": self.metrics["connection_errors"],
            "task_failures": self.metrics["task_failures"],
            "uptime": time.time() - self.start_time
        }

    def _attempt_reconnect(self):
        """尝试重连"""
        if self.reconnect_attempts >= self.max_reconnect_attempts:
            logger.error("重连次数已达上限，停止重连")
            return

        self.reconnect_attempts += 1
        self.status = ConnectionStatus.RECONNECTING

        # 指数退避重连
        delay = min(2 ** self.reconnect_attempts, 60)
        logger.info(f"第{self.reconnect_attempts}次重连，等待{delay}秒")

        threading.Timer(delay, self._reconnect).start()

    def _reconnect(self):
        """执行重连"""
        logger.info("正在重连WebSocket服务器")
        try:
            # 暂停所有活跃任务
            self._pause_all_tasks()

            # 重新连接
            self.connect()

            # 如果重连成功，恢复任务
            if self.is_connected:
                self._resume_all_tasks()

        except Exception as e:
            logger.error(f"重连失败: {e}")
            self._attempt_reconnect()

    def _pause_all_tasks(self):
        """暂停所有任务"""
        for task_id, task_context in self.active_tasks.items():
            try:
                if hasattr(task_context, 'pause'):
                    task_context.pause()
                    logger.info(f"任务 {task_id} 已暂停")
            except Exception as e:
                logger.error(f"暂停任务 {task_id} 失败: {e}")

    def _resume_all_tasks(self):
        """恢复所有任务"""
        for task_id, task_context in self.active_tasks.items():
            try:
                if hasattr(task_context, 'resume'):
                    task_context.resume()
                    logger.info(f"任务 {task_id} 已恢复")
            except Exception as e:
                logger.error(f"恢复任务 {task_id} 失败: {e}")

    def _cleanup_task(self, task_id: str):
        """清理任务"""
        if task_id in self.active_tasks:
            task_context = self.active_tasks[task_id]
            try:
                if hasattr(task_context, 'cleanup'):
                    task_context.cleanup()
                del self.active_tasks[task_id]
                logger.info(f"任务 {task_id} 已清理")
            except Exception as e:
                logger.error(f"清理任务 {task_id} 失败: {e}")

    def get_status(self) -> Dict[str, Any]:
        """获取客户端状态"""
        return {
            "vm_id": self.vm_id,
            "status": self.status.value,
            "is_connected": self.is_connected,
            "active_tasks_count": len(self.active_tasks),
            "reconnect_attempts": self.reconnect_attempts,
            "metrics": self.metrics,
            "uptime": time.time() - self.start_time
        }
