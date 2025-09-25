"""
WebSocket消息处理器

定义各种WebSocket消息的处理逻辑，包括：
- 虚拟机控制消息处理
- 联邦学习训练控制
- 模型传输处理
- 状态查询响应
- 训练数据同步处理
"""

import logging
import time
from typing import Dict, Any, Optional, TYPE_CHECKING
import psutil
import platform

if TYPE_CHECKING:
    from .websocket_client import WebSocketClient, MessageType
    from ..federated.client import FederatedLearningClient


class MessageHandler:
    """WebSocket消息处理器"""
    
    def __init__(self, vm_id: str):
        """初始化消息处理器
        
        Args:
            vm_id: 虚拟机ID
        """
        self.vm_id = vm_id
        self.logger = logging.getLogger(f"MessageHandler-{vm_id}")
        
        # 关联的客户端实例
        self.websocket_client: Optional['WebSocketClient'] = None
        self.federated_client: Optional['FederatedLearningClient'] = None
        
        # 训练状态
        self.training_task_id = None
        self.is_training = False
    
    def set_websocket_client(self, client: 'WebSocketClient'):
        """设置WebSocket客户端引用"""
        self.websocket_client = client
    
    def set_federated_client(self, client: 'FederatedLearningClient'):
        """设置联邦学习客户端引用"""
        self.federated_client = client
    
    def handle_connect_ack(self, message: Dict[str, Any]):
        """处理连接确认消息"""
        data = message.get("data", {})
        session_id = data.get("sessionId")
        heartbeat_interval = data.get("heartbeatInterval", 30)
        
        if self.websocket_client:
            self.websocket_client.session_id = session_id
            self.websocket_client.heartbeat_interval = heartbeat_interval
        
        self.logger.info(f"连接确认: sessionId={session_id}, heartbeat={heartbeat_interval}s")
    
    def handle_vm_start(self, message: Dict[str, Any]):
        """处理虚拟机启动命令"""
        data = message.get("data", {})
        timeout = data.get("timeout", 300)
        config = data.get("config", {})
        
        self.logger.info(f"收到虚拟机启动命令, 超时: {timeout}s")
        
        # 模拟虚拟机启动过程
        # 实际实现中这里应该调用相应的系统命令或API
        
        # 发送状态响应
        self._send_status_response("RUNNING")
    
    def handle_vm_stop(self, message: Dict[str, Any]):
        """处理虚拟机停止命令"""
        data = message.get("data", {})
        force = data.get("force", False)
        save_state = data.get("saveState", True)
        
        self.logger.info(f"收到虚拟机停止命令, 强制: {force}, 保存状态: {save_state}")
        
        # 如果正在训练，先停止训练
        if self.is_training and self.federated_client:
            self.logger.info("正在停止联邦学习训练")
            # 这里应该调用停止训练的方法
        
        # 发送状态响应
        self._send_status_response("STOPPED")
    
    def handle_training_start(self, message: Dict[str, Any]):
        """处理训练开始命令"""
        data = message.get("data", {})
        task_id = data.get("taskId")
        ml_algorithm = data.get("mlAlgorithm", "RandomForest")
        hyperparameters = data.get("hyperparameters", {})
        training_config = data.get("trainingConfig", {})
        
        self.logger.info(f"收到训练开始命令: taskId={task_id}, mlAlgorithm={ml_algorithm}")
        
        if not self.federated_client:
            self.logger.error("联邦学习客户端未初始化")
            self._send_error_response("TRAINING_FAILED", "联邦学习客户端未初始化")
            return
        
        try:
            self.training_task_id = task_id
            self.is_training = True
            
            # 合并配置参数
            config = {
                "mlAlgorithm": ml_algorithm,
                "hyperparameters": hyperparameters,
                **training_config
            }
            
            # 开始本地训练（这里应该在后台线程中执行）
            import threading
            training_thread = threading.Thread(
                target=self._execute_training,
                args=(task_id, config)
            )
            training_thread.daemon = True
            training_thread.start()
            
            # 发送训练开始确认
            self._send_training_status("STARTED")
            
        except Exception as e:
            self.logger.error(f"启动训练失败: {e}")
            self._send_error_response("TRAINING_FAILED", str(e))
            self.is_training = False
    
    def handle_training_stop(self, message: Dict[str, Any]):
        """处理训练停止命令"""
        data = message.get("data", {})
        task_id = data.get("taskId")
        reason = data.get("reason", "MANUAL_STOP")
        
        self.logger.info(f"收到训练停止命令: taskId={task_id}, reason={reason}")
        
        self.is_training = False
        self.training_task_id = None
        
        # 发送训练停止确认
        self._send_training_status("STOPPED")
    
    def handle_status_query(self, message: Dict[str, Any]):
        """处理状态查询请求"""
        data = message.get("data", {})
        query_type = data.get("queryType", "FULL")
        
        self.logger.debug(f"收到状态查询: {query_type}")
        
        try:
            status_data = self._collect_system_status(
                include_resources=data.get("includeResources", True),
                include_processes=data.get("includeProcesses", True),
                include_network=data.get("includeNetwork", True)
            )
            
            # 发送状态响应
            if self.websocket_client:
                from .websocket_client import MessageType
                self.websocket_client.send_message(MessageType.STATUS_RESPONSE, status_data)
            
        except Exception as e:
            self.logger.error(f"状态查询失败: {e}")
            self._send_error_response("STATUS_QUERY_FAILED", str(e))
    
    def handle_model_download(self, message: Dict[str, Any]):
        """处理模型下载消息"""
        data = message.get("data", {})
        parameters = data.get("parameters", {})
        model_info = parameters.get("model", {})
        
        self.logger.info(f"收到模型下载: framework={model_info.get('framework')}, format={model_info.get('format')}")
        
        if self.federated_client and parameters:
            # 更新本地模型参数
            weights = model_info.get("weights", {})
            if weights:
                self.federated_client.model_wrapper.set_parameters(weights)
                self.logger.info("全局模型参数已更新")
    
    def handle_dataset_create(self, message: Dict[str, Any]):
        """处理数据集创建消息"""
        data = message.get("data", {})
        dataset_id = data.get("datasetId")
        description = data.get("datasetDescription")
        
        self.logger.info(f"收到数据集创建请求: {dataset_id}")
        
        # 发送创建确认
        ack_data = {
            "datasetId": dataset_id,
            "status": "READY"
        }
        
        if self.websocket_client:
            from .websocket_client import MessageType
            self.websocket_client.send_message(MessageType.DATASET_CREATE_ACK, ack_data)
    
    def _execute_training(self, task_id: str, config: Dict[str, Any]):
        """执行训练过程"""
        try:
            if not self.federated_client:
                return
            
            # 从新的配置格式中获取参数
            ml_algorithm = config.get("mlAlgorithm", "RandomForest")
            hyperparameters = config.get("hyperparameters", {})
            epochs = config.get("epochs", 5)
            batch_size = config.get("batchSize", 32)
            
            self.logger.info(f"开始本地训练: 算法={ml_algorithm}, epochs={epochs}")
            
            for epoch in range(epochs):
                if not self.is_training:
                    break
                
                # 发送训练进度
                progress = (epoch + 1) / epochs * 100
                self._send_training_progress(task_id, epoch + 1, epochs, progress)
                
                # 模拟训练时间
                time.sleep(1)
            
            if self.is_training:
                # 训练完成，发送模型上传
                self._send_model_upload(task_id)
                self.is_training = False
                
        except Exception as e:
            self.logger.error(f"训练执行失败: {e}")
            self._send_error_response("TRAINING_FAILED", str(e))
            self.is_training = False
    
    def _send_training_progress(self, task_id: str, current_epoch: int, total_epochs: int, progress: float):
        """发送训练进度"""
        progress_data = {
            "taskId": task_id,
            "currentEpoch": current_epoch,
            "epochsPerRound": total_epochs,
            "progress": progress,
            "status": "TRAINING",
            "metrics": {
                "accuracy": 0.85,
                "loss": 0.15
            }
        }
        
        if self.websocket_client:
            from .websocket_client import MessageType
            self.websocket_client.send_message(MessageType.TRAINING_PROGRESS, progress_data)
    
    def _send_model_upload(self, task_id: str):
        """发送模型上传"""
        if not self.federated_client:
            return
        
        upload_data = {
            "taskId": task_id,
            "parameters": {
                "training": {
                    "algorithm": "RandomForest",
                    "samples": 1000
                }
            },
            "metrics": {
                "accuracy": 0.88,
                "loss": 0.12
            }
        }
        
        if self.websocket_client:
            from .websocket_client import MessageType
            self.websocket_client.send_message(MessageType.MODEL_UPLOAD, upload_data)
    
    def _send_training_status(self, status: str):
        """发送训练状态"""
        # 这里可以发送训练状态更新消息
        self.logger.info(f"训练状态: {status}")
    
    def _send_status_response(self, vm_status: str):
        """发送虚拟机状态响应"""
        status_data = {
            "status": vm_status,
            "uptime": int(time.time()),
            "resourceUsage": self._get_resource_usage(),
            "systemInfo": self._get_system_info()
        }
        
        if self.websocket_client:
            from .websocket_client import MessageType
            self.websocket_client.send_message(MessageType.STATUS_RESPONSE, status_data)
    
    def _send_error_response(self, error_code: str, error_message: str):
        """发送错误响应"""
        error_data = {
            "errorCode": error_code,
            "errorMessage": error_message,
            "severity": "HIGH",
            "vmId": self.vm_id
        }
        
        if self.websocket_client:
            from .websocket_client import MessageType
            self.websocket_client.send_message(MessageType.ERROR, error_data)
    
    def _collect_system_status(self, include_resources: bool = True, 
                              include_processes: bool = True,
                              include_network: bool = True) -> Dict[str, Any]:
        """收集系统状态信息"""
        status = {
            "status": "RUNNING",
            "uptime": int(time.time())
        }
        
        if include_resources:
            status["resourceUsage"] = self._get_resource_usage()
        
        if include_processes:
            status["processes"] = self._get_process_info()
        
        if include_network:
            status["network"] = self._get_network_info()
        
        status["systemInfo"] = self._get_system_info()
        
        return status
    
    def _get_resource_usage(self) -> Dict[str, float]:
        """获取资源使用情况"""
        try:
            return {
                "cpu": psutil.cpu_percent(interval=0.1),
                "memory": psutil.virtual_memory().percent,
                "disk": psutil.disk_usage('/').percent,
                "gpu": 0.0  # GPU使用率需要额外的库支持
            }
        except Exception:
            return {"cpu": 0.0, "memory": 0.0, "disk": 0.0, "gpu": 0.0}
    
    def _get_process_info(self) -> Dict[str, int]:
        """获取进程信息"""
        try:
            processes = list(psutil.process_iter())
            return {
                "total": len(processes),
                "active": len([p for p in processes if p.status() == psutil.STATUS_RUNNING]),
                "system": 0,  # 系统进程数需要进一步统计
                "user": 0     # 用户进程数需要进一步统计
            }
        except Exception:
            return {"total": 0, "active": 0, "system": 0, "user": 0}
    
    def _get_network_info(self) -> Dict[str, Any]:
        """获取网络信息"""
        try:
            # 这里应该获取实际的网络信息
            return {
                "ipAddress": "127.0.0.1",
                "macAddress": "00:00:00:00:00:00",
                "port": 22,
                "uploadSpeed": 1024,
                "downloadSpeed": 2048,
                "latency": 50
            }
        except Exception:
            return {
                "ipAddress": "unknown",
                "macAddress": "unknown",
                "port": 0,
                "uploadSpeed": 0,
                "downloadSpeed": 0,
                "latency": 0
            }
    
    def _get_system_info(self) -> Dict[str, str]:
        """获取系统信息"""
        try:
            return {
                "os": platform.system() + " " + platform.release(),
                "kernel": platform.version(),
                "loadAverage": [0.0, 0.0, 0.0],  # Linux特有，Windows需要其他方式
                "lastBoot": time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
            }
        except Exception:
            return {
                "os": "Unknown",
                "kernel": "Unknown",
                "loadAverage": [0.0, 0.0, 0.0],
                "lastBoot": "Unknown"
            }
