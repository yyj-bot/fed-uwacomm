"""
消息路由器 - v1.4协议消息分发
"""

import logging
import time
import base64
import json
from typing import Dict, Any, Callable, Optional
from enum import Enum

logger = logging.getLogger(__name__)


class MessageType(Enum):
    """消息类型枚举 - 对应v1.4协议的34个消息类型"""
    # 连接管理
    CONNECT = "CONNECT"
    CONNECT_ACK = "CONNECT_ACK"
    DISCONNECT = "DISCONNECT"

    # 心跳机制
    HEARTBEAT = "HEARTBEAT"
    HEARTBEAT_ACK = "HEARTBEAT_ACK"

    # 任务管理
    FEDERATED_TASK_START = "FEDERATED_TASK_START"
    FEDERATED_TASK_START_ACK = "FEDERATED_TASK_START_ACK"
    FEDERATED_TASK_STOP = "FEDERATED_TASK_STOP"
    FEDERATED_TASK_STOP_ACK = "FEDERATED_TASK_STOP_ACK"
    FEDERATED_TASK_RESUME = "FEDERATED_TASK_RESUME"
    FEDERATED_TASK_RESUME_ACK = "FEDERATED_TASK_RESUME_ACK"
    FEDERATED_TASK_DELETE = "FEDERATED_TASK_DELETE"
    FEDERATED_TASK_DELETE_ACK = "FEDERATED_TASK_DELETE_ACK"

    # 轮次控制
    ROUND_START = "ROUND_START"
    ROUND_START_ACK = "ROUND_START_ACK"
    ROUND_COMPLETE = "ROUND_COMPLETE"
    ROUND_COMPLETE_ACK = "ROUND_COMPLETE_ACK"

    # 模型传输
    GLOBAL_MODEL_BROADCAST = "GLOBAL_MODEL_BROADCAST"
    GLOBAL_MODEL_BROADCAST_ACK = "GLOBAL_MODEL_BROADCAST_ACK"
    GRADIENT_UPLOAD = "GRADIENT_UPLOAD"
    GRADIENT_UPLOAD_ACK = "GRADIENT_UPLOAD_ACK"

    # 训练数据管理
    TRAINING_DATA_QUERY = "TRAINING_DATA_QUERY"
    TRAINING_DATA_RESPONSE = "TRAINING_DATA_RESPONSE"
    DATASET_CREATE = "DATASET_CREATE"
    DATASET_CREATE_ACK = "DATASET_CREATE_ACK"
    DATASET_APPEND_ROWS = "DATASET_APPEND_ROWS"
    DATASET_APPEND_ROWS_ACK = "DATASET_APPEND_ROWS_ACK"
    DATASET_COMPLETE = "DATASET_COMPLETE"
    DATASET_COMPLETE_ACK = "DATASET_COMPLETE_ACK"
    DATASET_DELETE = "DATASET_DELETE"
    DATASET_DELETE_ACK = "DATASET_DELETE_ACK"

    # 状态查询
    STATUS_QUERY = "STATUS_QUERY"
    STATUS_RESPONSE = "STATUS_RESPONSE"

    # 错误处理
    ERROR = "ERROR"


class MessageRouter:
    """消息路由器 - 负责分发v1.4协议消息"""

    def __init__(self, client):
        self.client = client
        self.message_handlers = self._initialize_handlers()
        self.routing_stats = {
            "total_messages": 0,
            "successful_routes": 0,
            "failed_routes": 0,
            "unknown_types": 0
        }

    def _initialize_handlers(self) -> Dict[str, Callable]:
        """初始化消息处理器映射"""
        return {
            # 连接管理
            MessageType.CONNECT_ACK.value: self._handle_connect_ack,
            MessageType.HEARTBEAT_ACK.value: self._handle_heartbeat_ack,

            # 任务管理
            MessageType.FEDERATED_TASK_START.value: self._handle_task_start,
            MessageType.FEDERATED_TASK_STOP.value: self._handle_task_stop,
            MessageType.FEDERATED_TASK_RESUME.value: self._handle_task_resume,
            MessageType.FEDERATED_TASK_DELETE.value: self._handle_task_delete,

            # 轮次控制
            MessageType.ROUND_START.value: self._handle_round_start,
            MessageType.ROUND_COMPLETE.value: self._handle_round_complete,

            # 模型传输
            MessageType.GLOBAL_MODEL_BROADCAST.value: self._handle_global_model_broadcast,
            MessageType.GRADIENT_UPLOAD_ACK.value: self._handle_gradient_upload_ack,

            # 训练数据管理
            MessageType.TRAINING_DATA_QUERY.value: self._handle_training_data_query,
            MessageType.DATASET_CREATE.value: self._handle_dataset_create,
            MessageType.DATASET_APPEND_ROWS.value: self._handle_dataset_append_rows,
            MessageType.DATASET_COMPLETE.value: self._handle_dataset_complete,
            MessageType.DATASET_DELETE.value: self._handle_dataset_delete,

            # 状态查询
            MessageType.STATUS_QUERY.value: self._handle_status_query,

            # 错误处理
            MessageType.ERROR.value: self._handle_error_message
        }

    def route_message(self, message: Dict[str, Any]) -> bool:
        """路由消息到对应处理器"""
        self.routing_stats["total_messages"] += 1

        try:
            msg_type = message.get("type")
            if not msg_type:
                logger.error("消息缺少type字段")
                self.routing_stats["failed_routes"] += 1
                return False

            # 记录消息接收
            logger.debug(f"路由消息: {msg_type}")

            # 检查是否为任务级消息
            task_id = self._extract_task_id(message)
            if task_id:
                return self._route_to_task(task_id, message)
            else:
                return self._route_global_message(message)

        except Exception as e:
            logger.error(f"消息路由失败: {e}")
            self.routing_stats["failed_routes"] += 1
            return False

    def _extract_task_id(self, message: Dict[str, Any]) -> Optional[str]:
        """提取消息中的taskId"""
        data = message.get("data", {})
        return data.get("taskId")

    def _route_to_task(self, task_id: str, message: Dict[str, Any]) -> bool:
        """路由到特定任务"""
        if task_id not in self.client.active_tasks:
            logger.warning(f"任务 {task_id} 不存在，忽略消息")
            return False

        task_context = self.client.active_tasks[task_id]
        msg_type = message.get("type")

        # 根据消息类型调用对应的任务处理方法
        if msg_type in self.message_handlers:
            try:
                success = self.message_handlers[msg_type](message)
                if success:
                    self.routing_stats["successful_routes"] += 1
                else:
                    self.routing_stats["failed_routes"] += 1
                return success
            except Exception as e:
                logger.error(f"任务消息处理失败: {e}")
                self.routing_stats["failed_routes"] += 1
                return False
        else:
            logger.warning(f"未知的任务消息类型: {msg_type}")
            self.routing_stats["unknown_types"] += 1
            return False

    def _route_global_message(self, message: Dict[str, Any]) -> bool:
        """路由全局消息"""
        msg_type = message.get("type")

        if msg_type in self.message_handlers:
            try:
                success = self.message_handlers[msg_type](message)
                if success:
                    self.routing_stats["successful_routes"] += 1
                else:
                    self.routing_stats["failed_routes"] += 1
                return success
            except Exception as e:
                logger.error(f"全局消息处理失败: {e}")
                self.routing_stats["failed_routes"] += 1
                return False
        else:
            logger.warning(f"未知的全局消息类型: {msg_type}")
            self.routing_stats["unknown_types"] += 1
            return False

    # ========== 连接管理消息处理 ==========
    def _handle_connect_ack(self, message: Dict[str, Any]) -> bool:
        """处理连接确认"""
        try:
            data = message.get("data", {})
            status = data.get("status")

            if status == "SUCCESS":
                logger.info("服务器连接确认成功")

                # 更新客户端状态
                from .client import ConnectionStatus
                self.client.status = ConnectionStatus.CONNECTED

                # 处理服务器返回的配置信息
                server_config = data.get("serverConfig", {})
                self._apply_server_config(server_config)

                return True
            else:
                error_msg = data.get("message", "连接失败")
                logger.error(f"服务器连接确认失败: {error_msg}")
                from .client import ConnectionStatus
                self.client.status = ConnectionStatus.ERROR
                return False

        except Exception as e:
            logger.error(f"处理连接确认失败: {e}")
            return False

    def _handle_heartbeat_ack(self, message: Dict[str, Any]) -> bool:
        """处理心跳确认"""
        try:
            data = message.get("data", {})
            server_time = data.get("serverTime")

            if server_time:
                # 计算时间差（用于调试）
                client_time = self.client._get_current_timestamp()
                time_diff = abs(client_time - server_time)
                if time_diff > 5000:  # 5秒差异
                    logger.warning(f"客户端与服务器时间差异较大: {time_diff}ms")

            logger.debug("收到心跳确认")
            return True

        except Exception as e:
            logger.error(f"处理心跳确认失败: {e}")
            return False

    def _apply_server_config(self, server_config: Dict[str, Any]):
        """应用服务器配置"""
        try:
            # 更新心跳间隔
            if "heartbeatInterval" in server_config:
                new_interval = server_config["heartbeatInterval"]
                if 10 <= new_interval <= 300:  # 10秒到5分钟之间
                    self.client.heartbeat_interval = new_interval
                    logger.info(f"更新心跳间隔为 {new_interval} 秒")

            # 更新最大并发任务数
            if "maxConcurrentTasks" in server_config:
                new_max = server_config["maxConcurrentTasks"]
                if 1 <= new_max <= 10:  # 合理范围
                    self.client.max_concurrent_tasks = new_max
                    logger.info(f"更新最大并发任务数为 {new_max}")

        except Exception as e:
            logger.error(f"应用服务器配置失败: {e}")

    # ========== 任务管理消息处理 ==========
    def _handle_task_start(self, message: Dict[str, Any]) -> bool:
        """处理任务启动指令"""
        try:
            task_data = message.get("data", {})
            task_id = task_data.get("taskId")

            if not task_id:
                logger.error("任务启动消息缺少taskId")
                return False

            logger.info(f"收到任务启动指令: {task_id}")

            # 检查并发限制
            if len(self.client.active_tasks) >= self.client.max_concurrent_tasks:
                self._send_task_start_ack(task_id, "FAILED", "已达最大并发任务数")
                return False

            # 验证任务配置
            validation_result = self._validate_task_config(task_data)
            if not validation_result["valid"]:
                self._send_task_start_ack(task_id, "FAILED", validation_result["error"])
                return False

            # 创建任务上下文
            from .task_context import TaskContext
            task_context = TaskContext(task_id, task_data)
            
            # 设置梯度上传回调
            task_context.set_gradient_ready_callback(self.client.upload_gradients)

            # 初始化任务
            success = task_context.initialize()

            if success:
                self.client.active_tasks[task_id] = task_context
                self._send_task_start_ack(task_id, "SUCCESS", "任务初始化完成")
                logger.info(f"任务 {task_id} 启动成功")
            else:
                self._send_task_start_ack(task_id, "FAILED", "任务初始化失败")
                logger.error(f"任务 {task_id} 启动失败")

            return success

        except Exception as e:
            logger.error(f"处理任务启动失败: {e}")
            if 'task_id' in locals():
                self._send_task_start_ack(task_id, "FAILED", f"异常: {str(e)}")
            return False

    def _handle_task_stop(self, message: Dict[str, Any]) -> bool:
        """处理任务停止指令"""
        try:
            task_data = message.get("data", {})
            task_id = task_data.get("taskId")
            reason = task_data.get("reason", "MANUAL_STOP")
            graceful = task_data.get("graceful", True)

            if task_id in self.client.active_tasks:
                task_context = self.client.active_tasks[task_id]

                logger.info(f"停止任务 {task_id}，原因: {reason}")

                if graceful:
                    # 优雅停止：等待当前训练完成
                    task_context.stop()
                else:
                    # 强制停止：立即终止
                    from .task_context import TaskStatus
                    task_context.status = TaskStatus.COMPLETED

                # 发送确认
                self._send_task_stop_ack(task_id, "SUCCESS", "任务停止成功")

                return True
            else:
                self._send_task_stop_ack(task_id, "FAILED", "任务不存在")
                return False

        except Exception as e:
            logger.error(f"处理任务停止失败: {e}")
            return False

    def _handle_task_resume(self, message: Dict[str, Any]) -> bool:
        """处理任务恢复指令"""
        try:
            task_data = message.get("data", {})
            task_id = task_data.get("taskId")
            resume_info = task_data.get("resumeFrom", {})

            if task_id in self.client.active_tasks:
                task_context = self.client.active_tasks[task_id]

                # 恢复任务到指定状态
                from .task_context import TaskStatus
                if task_context.status == TaskStatus.PAUSED:
                    task_context.resume()

                    # 如果指定了恢复轮次，更新当前轮次
                    resume_round = resume_info.get("roundNumber")
                    if resume_round is not None:
                        task_context.current_round = resume_round

                    self._send_task_resume_ack(task_id, "SUCCESS", "任务恢复成功")
                    logger.info(f"任务 {task_id} 已恢复")
                    return True
                else:
                    self._send_task_resume_ack(task_id, "FAILED", f"任务状态不正确: {task_context.status}")
                    return False
            else:
                self._send_task_resume_ack(task_id, "FAILED", "任务不存在")
                return False

        except Exception as e:
            logger.error(f"处理任务恢复失败: {e}")
            return False

    def _handle_task_delete(self, message: Dict[str, Any]) -> bool:
        """处理任务删除指令"""
        try:
            task_data = message.get("data", {})
            task_id = task_data.get("taskId")

            if task_id in self.client.active_tasks:
                task_context = self.client.active_tasks[task_id]
                
                # 停止并清理任务
                task_context.cleanup()
                del self.client.active_tasks[task_id]

                self._send_task_delete_ack(task_id, "SUCCESS", "任务删除成功")
                logger.info(f"任务 {task_id} 已删除")
                return True
            else:
                self._send_task_delete_ack(task_id, "FAILED", "任务不存在")
                return False

        except Exception as e:
            logger.error(f"处理任务删除失败: {e}")
            return False

    def _validate_task_config(self, task_data: Dict[str, Any]) -> Dict[str, Any]:
        """验证任务配置"""
        try:
            # 必需字段检查
            required_fields = ["taskId", "federatedAlgorithm", "totalRounds"]
            for field in required_fields:
                if field not in task_data:
                    return {"valid": False, "error": f"缺少必需字段: {field}"}

            # 算法支持检查
            algorithm = task_data.get("federatedAlgorithm")
            supported_algorithms = ["FEDERATED_AVERAGING", "FEDERATED_PROXIMAL", "FEDERATED_NOVA", "SCAFFOLD"]
            if algorithm not in supported_algorithms:
                return {"valid": False, "error": f"不支持的算法: {algorithm}"}

            # 轮次数检查
            total_rounds = task_data.get("totalRounds")
            if not isinstance(total_rounds, int) or total_rounds < 1 or total_rounds > 1000:
                return {"valid": False, "error": "轮次数必须在1-1000之间"}

            # 配置参数检查
            local_config = task_data.get("localTrainingConfig", {})
            if "datasetId" not in local_config:
                return {"valid": False, "error": "缺少数据集ID"}

            return {"valid": True, "error": None}

        except Exception as e:
            return {"valid": False, "error": f"配置验证异常: {str(e)}"}

    # ========== 轮次控制消息处理 ==========
    def _handle_round_start(self, message: Dict[str, Any]) -> bool:
        """处理轮次开始指令"""
        try:
            round_data = message.get("data", {})
            task_id = round_data.get("taskId")
            round_number = round_data.get("roundNumber")

            if task_id not in self.client.active_tasks:
                logger.error(f"任务 {task_id} 不存在")
                return False

            task_context = self.client.active_tasks[task_id]

            logger.info(f"任务 {task_id} 开始第 {round_number} 轮")

            # 发送轮次开始确认
            self._send_round_start_ack(task_id, round_number, "SUCCESS", "轮次开始确认")

            # 启动训练
            success = task_context.start_round_training(round_number, round_data)

            if success:
                logger.info(f"任务 {task_id} 第 {round_number} 轮训练启动成功")
            else:
                logger.error(f"任务 {task_id} 第 {round_number} 轮训练启动失败")
                # 发送错误报告
                self._report_round_error(task_id, round_number, "轮次启动失败")

            return success

        except Exception as e:
            logger.error(f"处理轮次开始失败: {e}")
            return False

    def _handle_round_complete(self, message: Dict[str, Any]) -> bool:
        """处理轮次完成通知"""
        try:
            round_data = message.get("data", {})
            task_id = round_data.get("taskId")
            round_number = round_data.get("roundNumber")
            round_results = round_data.get("roundResults", {})

            if task_id in self.client.active_tasks:
                task_context = self.client.active_tasks[task_id]

                # 更新任务统计信息
                task_context.last_activity = time.time()

                # 记录轮次结果
                if hasattr(task_context, 'round_results'):
                    task_context.round_results[round_number] = round_results
                else:
                    task_context.round_results = {round_number: round_results}

                # 发送确认
                self._send_round_complete_ack(task_id, round_number, "SUCCESS", "轮次完成确认")

                logger.info(f"任务 {task_id} 第 {round_number} 轮完成")
                return True
            else:
                logger.warning(f"收到未知任务的轮次完成通知: {task_id}")
                return False

        except Exception as e:
            logger.error(f"处理轮次完成失败: {e}")
            return False

    # ========== 模型传输处理 ==========
    def _handle_global_model_broadcast(self, message: Dict[str, Any]) -> bool:
        """处理全局模型广播"""
        try:
            model_data = message.get("data", {})
            task_id = model_data.get("taskId")
            round_number = model_data.get("roundNumber")

            if task_id not in self.client.active_tasks:
                logger.error(f"任务 {task_id} 不存在")
                return False

            task_context = self.client.active_tasks[task_id]

            logger.info(f"任务 {task_id} 收到第 {round_number} 轮全局模型")

            # 解码全局模型数据
            global_model = self._decode_global_model(model_data)
            if not global_model:
                logger.error("全局模型解码失败")
                self._send_global_model_broadcast_ack(task_id, round_number, "FAILED", "模型解码失败")
                return False

            # 更新本地模型
            success = task_context.update_global_model(global_model)

            # 发送确认
            status = "SUCCESS" if success else "FAILED"
            message_text = "全局模型更新成功" if success else "全局模型更新失败"
            self._send_global_model_broadcast_ack(task_id, round_number, status, message_text)

            if success:
                logger.info(f"任务 {task_id} 全局模型更新完成")

                # 处理聚合信息
                aggregation_info = model_data.get("aggregationInfo", {})
                self._process_aggregation_info(task_context, aggregation_info)
            else:
                logger.error(f"任务 {task_id} 全局模型更新失败")

            return success

        except Exception as e:
            logger.error(f"处理全局模型广播失败: {e}")
            return False

    def _decode_global_model(self, model_data: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        """解码全局模型数据"""
        try:
            # 检查模型数据格式
            if "globalModel" in model_data:
                # Base64编码的模型数据
                global_model_b64 = model_data["globalModel"]
                global_model_bytes = base64.b64decode(global_model_b64)
                global_model = json.loads(global_model_bytes.decode('utf-8'))
            elif "globalModelData" in model_data:
                # 直接的JSON数据
                global_model = model_data["globalModelData"]
            else:
                logger.error("未找到全局模型数据")
                return None

            # 验证模型数据完整性
            if not self._validate_model_data(global_model):
                logger.error("全局模型数据验证失败")
                return None

            return global_model

        except Exception as e:
            logger.error(f"解码全局模型失败: {e}")
            return None

    def _validate_model_data(self, model_data: Dict[str, Any]) -> bool:
        """验证模型数据完整性"""
        try:
            # 基本字段检查
            required_fields = ["model_type"]
            for field in required_fields:
                if field not in model_data:
                    logger.error(f"模型数据缺少字段: {field}")
                    return False

            # 模型类型检查
            model_type = model_data.get("model_type")
            supported_types = ["RandomForest", "DecisionTree", "SVM", "NeuralNetwork"]
            if model_type not in supported_types:
                logger.warning(f"未知模型类型: {model_type}")
                # 不阻止处理，只是警告

            return True

        except Exception as e:
            logger.error(f"模型数据验证异常: {e}")
            return False

    def _process_aggregation_info(self, task_context, aggregation_info: Dict[str, Any]):
        """处理聚合信息"""
        try:
            # 记录聚合统计信息
            if "globalAccuracy" in aggregation_info:
                global_accuracy = aggregation_info["globalAccuracy"]
                logger.info(f"全局准确率: {global_accuracy:.4f}")

            if "participantCount" in aggregation_info:
                participant_count = aggregation_info["participantCount"]
                logger.info(f"参与聚合的VM数量: {participant_count}")

            if "convergenceInfo" in aggregation_info:
                convergence = aggregation_info["convergenceInfo"]
                logger.info(f"收敛信息: {convergence}")

            # 更新任务统计
            if hasattr(task_context, 'aggregation_history'):
                task_context.aggregation_history.append(aggregation_info)
            else:
                task_context.aggregation_history = [aggregation_info]

        except Exception as e:
            logger.error(f"处理聚合信息失败: {e}")

    def _handle_gradient_upload_ack(self, message: Dict[str, Any]) -> bool:
        """处理梯度上传确认"""
        try:
            data = message.get("data", {})
            task_id = data.get("taskId")
            round_number = data.get("roundNumber")
            status = data.get("status")

            if status == "SUCCESS":
                logger.info(f"任务 {task_id} 第 {round_number} 轮梯度上传确认成功")
                return True
            else:
                error_msg = data.get("message", "上传失败")
                logger.error(f"任务 {task_id} 第 {round_number} 轮梯度上传失败: {error_msg}")

                # 可以尝试重新上传
                if task_id in self.client.active_tasks:
                    task_context = self.client.active_tasks[task_id]
                    if hasattr(task_context, 'gradient_data') and task_context.gradient_data:
                        # 重新上传梯度
                        self._retry_gradient_upload(task_id, round_number)

                return False

        except Exception as e:
            logger.error(f"处理梯度上传确认失败: {e}")
            return False

    def _retry_gradient_upload(self, task_id: str, round_number: int, max_retries: int = 3):
        """重试梯度上传"""
        if task_id in self.client.active_tasks:
            task_context = self.client.active_tasks[task_id]
            retry_count = getattr(task_context, 'upload_retry_count', 0)

            if retry_count < max_retries:
                task_context.upload_retry_count = retry_count + 1
                logger.info(f"重试上传任务 {task_id} 梯度，第 {retry_count + 1} 次")

                # 延迟重试
                import threading
                threading.Timer(2.0, lambda: self.upload_gradients(
                    task_id, round_number, task_context.gradient_data, {}
                )).start()
            else:
                logger.error(f"任务 {task_id} 梯度上传重试次数已达上限")

    # ========== 状态查询处理 ==========
    def _handle_status_query(self, message: Dict[str, Any]) -> bool:
        """处理状态查询"""
        try:
            query_data = message.get("data", {})
            query_type = query_data.get("queryType", "VM_STATUS")
            query_id = message.get("id")

            logger.debug(f"处理状态查询: {query_type}")

            if query_type == "VM_STATUS":
                # 返回VM整体状态
                status_data = self._get_vm_status()
            elif query_type == "TASK_STATUS":
                # 返回特定任务状态
                task_id = query_data.get("taskId")
                status_data = self._get_task_status(task_id)
            elif query_type == "PERFORMANCE_METRICS":
                # 返回性能指标
                status_data = self._get_performance_metrics()
            elif query_type == "RESOURCE_USAGE":
                # 返回资源使用情况
                status_data = self._get_resource_status()
            else:
                status_data = {"error": f"未知查询类型: {query_type}"}

            # 发送状态响应
            response_message = {
                "type": "STATUS_RESPONSE",
                "id": self.client._generate_message_id(),
                "timestamp": self.client._get_current_timestamp(),
                "vmId": self.client.vm_id,
                "replyTo": query_id,
                "data": {
                    "queryType": query_type,
                    "status": status_data,
                    "timestamp": time.time()
                }
            }

            return self.client._send_message(response_message)

        except Exception as e:
            logger.error(f"处理状态查询失败: {e}")
            return False

    def _get_vm_status(self) -> Dict[str, Any]:
        """获取VM整体状态"""
        return {
            "vmId": self.client.vm_id,
            "status": self.client.status.value,
            "connectionStatus": "CONNECTED" if self.client.is_connected else "DISCONNECTED",
            "resourceUsage": self.client._get_resource_usage(),
            "activeTasks": self.client._get_active_tasks_status(),
            "capabilities": self.client._get_vm_capabilities(),
            "routingStats": self.get_routing_stats(),
            "uptime": time.time() - getattr(self.client, 'start_time', time.time())
        }

    def _get_task_status(self, task_id: str) -> Dict[str, Any]:
        """获取特定任务状态"""
        if task_id and task_id in self.client.active_tasks:
            task_context = self.client.active_tasks[task_id]
            return task_context.get_statistics() if hasattr(task_context, 'get_statistics') else {}
        elif task_id:
            return {"error": f"任务 {task_id} 不存在"}
        else:
            # 返回所有任务状态
            return {
                "tasks": {
                    task_id: task_context.get_statistics() if hasattr(task_context, 'get_statistics') else {}
                    for task_id, task_context in self.client.active_tasks.items()
                }
            }

    def _get_performance_metrics(self) -> Dict[str, Any]:
        """获取性能指标"""
        return {
            "messageStats": self.get_routing_stats(),
            "clientMetrics": self.client._get_performance_metrics(),
            "taskMetrics": self._get_aggregated_task_metrics(),
            "systemMetrics": self._get_system_metrics()
        }

    def _get_resource_status(self) -> Dict[str, Any]:
        """获取资源状态"""
        return self.client._get_resource_usage()

    def _get_aggregated_task_metrics(self) -> Dict[str, Any]:
        """获取聚合任务指标"""
        if not self.client.active_tasks:
            return {"totalTasks": 0}

        total_rounds = 0
        total_training_time = 0.0
        error_count = 0

        for task_context in self.client.active_tasks.values():
            if hasattr(task_context, 'get_statistics'):
                stats = task_context.get_statistics()
                total_rounds += stats.get("completed_rounds", 0)
                total_training_time += stats.get("average_training_time", 0.0) * stats.get("completed_rounds", 0)
                error_count += stats.get("error_count", 0)

        avg_training_time = total_training_time / max(total_rounds, 1)

        return {
            "totalTasks": len(self.client.active_tasks),
            "totalCompletedRounds": total_rounds,
            "averageTrainingTime": avg_training_time,
            "totalErrors": error_count,
            "successRate": (total_rounds - error_count) / max(total_rounds, 1)
        }

    def _get_system_metrics(self) -> Dict[str, Any]:
        """获取系统指标"""
        try:
            import psutil
            import threading

            return {
                "cpu": {
                    "percent": psutil.cpu_percent(),
                    "count": psutil.cpu_count(),
                    "load_avg": psutil.getloadavg() if hasattr(psutil, 'getloadavg') else [0, 0, 0]
                },
                "memory": {
                    "total": psutil.virtual_memory().total,
                    "available": psutil.virtual_memory().available,
                    "percent": psutil.virtual_memory().percent
                },
                "disk": {
                    "total": psutil.disk_usage('/').total,
                    "free": psutil.disk_usage('/').free,
                    "percent": psutil.disk_usage('/').percent
                },
                "network": {
                    "bytes_sent": psutil.net_io_counters().bytes_sent,
                    "bytes_recv": psutil.net_io_counters().bytes_recv
                },
                "threads": {
                    "active": threading.active_count(),
                    "main": threading.main_thread().is_alive()
                }
            }
        except ImportError:
            return {"error": "psutil未安装，无法获取系统指标"}

    # ========== 数据管理消息处理 ==========
    def _handle_training_data_query(self, message: Dict[str, Any]) -> bool:
        """处理训练数据查询"""
        # 实现训练数据查询逻辑
        logger.info("处理训练数据查询")
        return True

    def _handle_dataset_create(self, message: Dict[str, Any]) -> bool:
        """处理数据集创建"""
        # 实现数据集创建逻辑
        logger.info("处理数据集创建")
        return True

    def _handle_dataset_append_rows(self, message: Dict[str, Any]) -> bool:
        """处理数据集行追加"""
        # 实现数据集行追加逻辑
        logger.info("处理数据集行追加")
        return True

    def _handle_dataset_complete(self, message: Dict[str, Any]) -> bool:
        """处理数据集完成"""
        # 实现数据集完成逻辑
        logger.info("处理数据集完成")
        return True

    def _handle_dataset_delete(self, message: Dict[str, Any]) -> bool:
        """处理数据集删除"""
        # 实现数据集删除逻辑
        logger.info("处理数据集删除")
        return True

    def _handle_error_message(self, message: Dict[str, Any]) -> bool:
        """处理错误消息"""
        try:
            data = message.get("data", {})
            error_type = data.get("errorType", "UNKNOWN")
            error_message = data.get("errorMessage", "")
            
            logger.error(f"收到错误消息 - 类型: {error_type}, 消息: {error_message}")
            
            # 根据错误类型进行相应处理
            if error_type == "TASK_ERROR":
                task_id = data.get("taskId")
                if task_id and task_id in self.client.active_tasks:
                    task_context = self.client.active_tasks[task_id]
                    # 处理任务级错误
                    if hasattr(task_context, 'handle_error'):
                        task_context.handle_error(error_message)
            
            return True
        except Exception as e:
            logger.error(f"处理错误消息失败: {e}")
            return False

    # ========== 确认消息发送方法 ==========
    def _send_task_start_ack(self, task_id: str, status: str, message: str):
        """发送任务启动确认"""
        ack_message = {
            "type": "FEDERATED_TASK_START_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _send_task_stop_ack(self, task_id: str, status: str, message: str):
        """发送任务停止确认"""
        ack_message = {
            "type": "FEDERATED_TASK_STOP_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _send_task_resume_ack(self, task_id: str, status: str, message: str):
        """发送任务恢复确认"""
        ack_message = {
            "type": "FEDERATED_TASK_RESUME_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _send_task_delete_ack(self, task_id: str, status: str, message: str):
        """发送任务删除确认"""
        ack_message = {
            "type": "FEDERATED_TASK_DELETE_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _send_round_start_ack(self, task_id: str, round_number: int, status: str, message: str):
        """发送轮次开始确认"""
        ack_message = {
            "type": "ROUND_START_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "roundNumber": round_number,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _send_round_complete_ack(self, task_id: str, round_number: int, status: str, message: str):
        """发送轮次完成确认"""
        ack_message = {
            "type": "ROUND_COMPLETE_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "roundNumber": round_number,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _send_global_model_broadcast_ack(self, task_id: str, round_number: int, status: str, message: str):
        """发送全局模型广播确认"""
        ack_message = {
            "type": "GLOBAL_MODEL_BROADCAST_ACK",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "roundNumber": round_number,
                "status": status,
                "message": message
            }
        }
        self.client._send_message(ack_message)

    def _report_round_error(self, task_id: str, round_number: int, error_message: str):
        """报告轮次错误"""
        error_msg = {
            "type": "ERROR",
            "id": self.client._generate_message_id(),
            "timestamp": self.client._get_current_timestamp(),
            "vmId": self.client.vm_id,
            "data": {
                "taskId": task_id,
                "roundNumber": round_number,
                "errorType": "ROUND_ERROR",
                "errorMessage": error_message,
                "severity": "ERROR"
            }
        }
        self.client._send_message(error_msg)

    def get_routing_stats(self) -> Dict[str, Any]:
        """获取路由统计信息"""
        return self.routing_stats.copy()
