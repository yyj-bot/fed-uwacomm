"""
错误处理器 - 实现分级错误处理、自动恢复、连接重试
基于v1.4协议的Python VM错误处理系统
"""

import logging
import time
import threading
import traceback
from typing import Dict, Any, Optional, Callable, List
from enum import Enum
from dataclasses import dataclass
from collections import defaultdict, deque
import json

logger = logging.getLogger(__name__)


class ErrorLevel(Enum):
    """错误级别枚举"""
    LOW = "LOW"           # 轻微错误，可忽略
    MEDIUM = "MEDIUM"     # 中等错误，需要处理但不影响主要功能
    HIGH = "HIGH"         # 严重错误，影响功能但可恢复
    CRITICAL = "CRITICAL" # 致命错误，需要立即处理


class ErrorType(Enum):
    """错误类型枚举"""
    CONNECTION_ERROR = "CONNECTION_ERROR"
    MESSAGE_ERROR = "MESSAGE_ERROR"
    TASK_ERROR = "TASK_ERROR"
    RESOURCE_ERROR = "RESOURCE_ERROR"
    VALIDATION_ERROR = "VALIDATION_ERROR"
    TIMEOUT_ERROR = "TIMEOUT_ERROR"
    SYSTEM_ERROR = "SYSTEM_ERROR"
    UNKNOWN_ERROR = "UNKNOWN_ERROR"


class RecoveryStrategy(Enum):
    """恢复策略枚举"""
    IGNORE = "IGNORE"           # 忽略错误
    RETRY = "RETRY"             # 重试操作
    RECONNECT = "RECONNECT"     # 重新连接
    RESTART = "RESTART"         # 重启组件
    FALLBACK = "FALLBACK"       # 降级处理
    ESCALATE = "ESCALATE"       # 升级处理


@dataclass
class ErrorInfo:
    """错误信息数据类"""
    error_id: str
    error_type: ErrorType
    error_level: ErrorLevel
    message: str
    timestamp: float
    context: Dict[str, Any]
    traceback_info: Optional[str] = None
    recovery_strategy: Optional[RecoveryStrategy] = None
    retry_count: int = 0
    max_retries: int = 3
    resolved: bool = False


class ErrorHandler:
    """错误处理器 - 分级错误处理和自动恢复"""

    def __init__(self, client=None):
        """
        初始化错误处理器
        
        Args:
            client: WebSocket客户端实例
        """
        self.client = client
        
        # 错误存储
        self.error_history: deque = deque(maxlen=1000)  # 最近1000个错误
        self.active_errors: Dict[str, ErrorInfo] = {}   # 活跃错误
        self.error_stats: Dict[ErrorType, int] = defaultdict(int)
        
        # 恢复策略映射
        self.recovery_strategies = self._initialize_recovery_strategies()
        
        # 错误处理器映射
        self.error_handlers = self._initialize_error_handlers()
        
        # 重试配置
        self.retry_configs = self._initialize_retry_configs()
        
        # 降级模式
        self.degraded_mode = False
        self.degraded_features = set()
        
        # 错误回调
        self.error_callbacks: List[Callable] = []
        
        # 线程锁
        self.lock = threading.RLock()
        
        # 错误处理线程
        self.error_queue: deque = deque()
        self.processing_thread: Optional[threading.Thread] = None
        self.processing = False
        
        logger.info("错误处理器初始化完成")

    def _initialize_recovery_strategies(self) -> Dict[ErrorType, RecoveryStrategy]:
        """初始化恢复策略映射"""
        return {
            ErrorType.CONNECTION_ERROR: RecoveryStrategy.RECONNECT,
            ErrorType.MESSAGE_ERROR: RecoveryStrategy.RETRY,
            ErrorType.TASK_ERROR: RecoveryStrategy.RESTART,
            ErrorType.RESOURCE_ERROR: RecoveryStrategy.FALLBACK,
            ErrorType.VALIDATION_ERROR: RecoveryStrategy.IGNORE,
            ErrorType.TIMEOUT_ERROR: RecoveryStrategy.RETRY,
            ErrorType.SYSTEM_ERROR: RecoveryStrategy.ESCALATE,
            ErrorType.UNKNOWN_ERROR: RecoveryStrategy.ESCALATE
        }

    def _initialize_error_handlers(self) -> Dict[ErrorType, Callable]:
        """初始化错误处理器映射"""
        return {
            ErrorType.CONNECTION_ERROR: self._handle_connection_error,
            ErrorType.MESSAGE_ERROR: self._handle_message_error,
            ErrorType.TASK_ERROR: self._handle_task_error,
            ErrorType.RESOURCE_ERROR: self._handle_resource_error,
            ErrorType.VALIDATION_ERROR: self._handle_validation_error,
            ErrorType.TIMEOUT_ERROR: self._handle_timeout_error,
            ErrorType.SYSTEM_ERROR: self._handle_system_error,
            ErrorType.UNKNOWN_ERROR: self._handle_unknown_error
        }

    def _initialize_retry_configs(self) -> Dict[ErrorType, Dict[str, Any]]:
        """初始化重试配置"""
        return {
            ErrorType.CONNECTION_ERROR: {
                'max_retries': 5,
                'base_delay': 2.0,
                'max_delay': 60.0,
                'backoff_factor': 2.0
            },
            ErrorType.MESSAGE_ERROR: {
                'max_retries': 3,
                'base_delay': 1.0,
                'max_delay': 10.0,
                'backoff_factor': 1.5
            },
            ErrorType.TASK_ERROR: {
                'max_retries': 2,
                'base_delay': 5.0,
                'max_delay': 30.0,
                'backoff_factor': 2.0
            },
            ErrorType.TIMEOUT_ERROR: {
                'max_retries': 3,
                'base_delay': 2.0,
                'max_delay': 20.0,
                'backoff_factor': 2.0
            }
        }

    def handle_error(self, 
                    error_type: ErrorType, 
                    message: str, 
                    context: Optional[Dict[str, Any]] = None,
                    exception: Optional[Exception] = None,
                    level: Optional[ErrorLevel] = None) -> str:
        """
        处理错误
        
        Args:
            error_type: 错误类型
            message: 错误消息
            context: 错误上下文
            exception: 异常对象
            level: 错误级别
        
        Returns:
            错误ID
        """
        # 生成错误ID
        error_id = self._generate_error_id()
        
        # 确定错误级别
        if level is None:
            level = self._determine_error_level(error_type, message, exception)
        
        # 创建错误信息
        error_info = ErrorInfo(
            error_id=error_id,
            error_type=error_type,
            error_level=level,
            message=message,
            timestamp=time.time(),
            context=context or {},
            traceback_info=traceback.format_exc() if exception else None,
            recovery_strategy=self.recovery_strategies.get(error_type, RecoveryStrategy.ESCALATE)
        )
        
        # 记录错误
        self._record_error(error_info)
        
        # 异步处理错误
        self._queue_error_for_processing(error_info)
        
        logger.error(f"错误处理 [{error_id}] {error_type.value}: {message}")
        
        return error_id

    def _generate_error_id(self) -> str:
        """生成错误ID"""
        import uuid
        return f"ERR_{int(time.time())}_{str(uuid.uuid4())[:8]}"

    def _determine_error_level(self, 
                              error_type: ErrorType, 
                              message: str, 
                              exception: Optional[Exception]) -> ErrorLevel:
        """确定错误级别"""
        # 基于错误类型的默认级别
        type_level_map = {
            ErrorType.CONNECTION_ERROR: ErrorLevel.HIGH,
            ErrorType.MESSAGE_ERROR: ErrorLevel.MEDIUM,
            ErrorType.TASK_ERROR: ErrorLevel.HIGH,
            ErrorType.RESOURCE_ERROR: ErrorLevel.CRITICAL,
            ErrorType.VALIDATION_ERROR: ErrorLevel.LOW,
            ErrorType.TIMEOUT_ERROR: ErrorLevel.MEDIUM,
            ErrorType.SYSTEM_ERROR: ErrorLevel.CRITICAL,
            ErrorType.UNKNOWN_ERROR: ErrorLevel.MEDIUM
        }
        
        base_level = type_level_map.get(error_type, ErrorLevel.MEDIUM)
        
        # 基于消息内容调整级别
        if any(keyword in message.lower() for keyword in ['critical', 'fatal', 'crash']):
            return ErrorLevel.CRITICAL
        elif any(keyword in message.lower() for keyword in ['warning', 'minor']):
            return ErrorLevel.LOW
        
        return base_level

    def _record_error(self, error_info: ErrorInfo):
        """记录错误"""
        with self.lock:
            # 添加到历史记录
            self.error_history.append(error_info)
            
            # 添加到活跃错误
            self.active_errors[error_info.error_id] = error_info
            
            # 更新统计
            self.error_stats[error_info.error_type] += 1
            
            # 触发错误回调
            self._notify_error_callbacks(error_info)

    def _queue_error_for_processing(self, error_info: ErrorInfo):
        """将错误加入处理队列"""
        self.error_queue.append(error_info)
        
        # 启动处理线程（如果未启动）
        if not self.processing:
            self.start_error_processing()

    def start_error_processing(self):
        """启动错误处理线程"""
        if self.processing:
            return
        
        self.processing = True
        self.processing_thread = threading.Thread(target=self._error_processing_loop, daemon=True)
        self.processing_thread.start()
        logger.info("启动错误处理线程")

    def stop_error_processing(self):
        """停止错误处理线程"""
        self.processing = False
        if self.processing_thread:
            self.processing_thread.join(timeout=5.0)
        logger.info("停止错误处理线程")

    def _error_processing_loop(self):
        """错误处理循环"""
        while self.processing:
            try:
                if self.error_queue:
                    error_info = self.error_queue.popleft()
                    self._process_error(error_info)
                else:
                    time.sleep(0.1)  # 短暂等待
            except Exception as e:
                logger.error(f"错误处理循环异常: {e}")
                time.sleep(1.0)

    def _process_error(self, error_info: ErrorInfo):
        """处理单个错误"""
        try:
            logger.debug(f"处理错误 [{error_info.error_id}] {error_info.error_type.value}")
            
            # 获取错误处理器
            handler = self.error_handlers.get(error_info.error_type, self._handle_unknown_error)
            
            # 执行错误处理
            success = handler(error_info)
            
            if success:
                self._mark_error_resolved(error_info.error_id)
                logger.info(f"错误处理成功 [{error_info.error_id}]")
            else:
                self._handle_recovery_failure(error_info)
                
        except Exception as e:
            logger.error(f"处理错误时发生异常 [{error_info.error_id}]: {e}")
            self._handle_recovery_failure(error_info)

    def _mark_error_resolved(self, error_id: str):
        """标记错误已解决"""
        with self.lock:
            if error_id in self.active_errors:
                self.active_errors[error_id].resolved = True
                del self.active_errors[error_id]

    def _handle_recovery_failure(self, error_info: ErrorInfo):
        """处理恢复失败"""
        error_info.retry_count += 1
        
        if error_info.retry_count < error_info.max_retries:
            # 计算重试延迟
            delay = self._calculate_retry_delay(error_info)
            
            logger.warning(f"错误恢复失败，{delay}秒后重试 [{error_info.error_id}] ({error_info.retry_count}/{error_info.max_retries})")
            
            # 延迟重试
            threading.Timer(delay, lambda: self._queue_error_for_processing(error_info)).start()
        else:
            logger.error(f"错误恢复失败，已达最大重试次数 [{error_info.error_id}]")
            self._escalate_error(error_info)

    def _calculate_retry_delay(self, error_info: ErrorInfo) -> float:
        """计算重试延迟"""
        config = self.retry_configs.get(error_info.error_type, {})
        base_delay = config.get('base_delay', 2.0)
        max_delay = config.get('max_delay', 60.0)
        backoff_factor = config.get('backoff_factor', 2.0)
        
        delay = base_delay * (backoff_factor ** (error_info.retry_count - 1))
        return min(delay, max_delay)

    def _escalate_error(self, error_info: ErrorInfo):
        """升级错误处理"""
        logger.critical(f"错误升级处理 [{error_info.error_id}] {error_info.error_type.value}")
        
        # 进入降级模式
        if error_info.error_level in [ErrorLevel.HIGH, ErrorLevel.CRITICAL]:
            self._enter_degraded_mode(error_info.error_type)
        
        # 发送错误报告到服务器
        if self.client:
            self._send_error_report(error_info)

    def _enter_degraded_mode(self, error_type: ErrorType):
        """进入降级模式"""
        if not self.degraded_mode:
            self.degraded_mode = True
            logger.warning("系统进入降级模式")
        
        # 根据错误类型禁用相关功能
        if error_type == ErrorType.CONNECTION_ERROR:
            self.degraded_features.add('websocket_communication')
        elif error_type == ErrorType.TASK_ERROR:
            self.degraded_features.add('task_execution')
        elif error_type == ErrorType.RESOURCE_ERROR:
            self.degraded_features.add('resource_intensive_operations')

    def _exit_degraded_mode(self):
        """退出降级模式"""
        if self.degraded_mode:
            self.degraded_mode = False
            self.degraded_features.clear()
            logger.info("系统退出降级模式")

    def _send_error_report(self, error_info: ErrorInfo):
        """发送错误报告到服务器"""
        try:
            if not self.client or not hasattr(self.client, '_send_message'):
                return
            
            error_report = {
                "type": "ERROR",
                "id": self.client._generate_message_id(),
                "timestamp": self.client._get_current_timestamp(),
                "vmId": self.client.vm_id,
                "data": {
                    "errorId": error_info.error_id,
                    "errorType": error_info.error_type.value,
                    "errorLevel": error_info.error_level.value,
                    "errorMessage": error_info.message,
                    "context": error_info.context,
                    "retryCount": error_info.retry_count,
                    "severity": "CRITICAL" if error_info.error_level == ErrorLevel.CRITICAL else "ERROR"
                }
            }
            
            self.client._send_message(error_report)
            logger.debug(f"发送错误报告 [{error_info.error_id}]")
            
        except Exception as e:
            logger.error(f"发送错误报告失败: {e}")

    # ========== 具体错误处理器 ==========
    def _handle_connection_error(self, error_info: ErrorInfo) -> bool:
        """处理连接错误"""
        try:
            if not self.client:
                return False
            
            logger.info(f"处理连接错误 [{error_info.error_id}]")
            
            # 尝试重新连接
            if hasattr(self.client, 'reconnect'):
                success = self.client.reconnect()
                if success:
                    logger.info("连接恢复成功")
                    return True
            
            # 如果重连失败，等待一段时间后再试
            return False
            
        except Exception as e:
            logger.error(f"处理连接错误异常: {e}")
            return False

    def _handle_message_error(self, error_info: ErrorInfo) -> bool:
        """处理消息错误"""
        try:
            logger.info(f"处理消息错误 [{error_info.error_id}]")
            
            # 检查消息格式
            message_data = error_info.context.get('message')
            if message_data:
                # 尝试修复消息格式
                fixed_message = self._try_fix_message(message_data)
                if fixed_message:
                    # 重新发送修复后的消息
                    if self.client and hasattr(self.client, '_send_message'):
                        return self.client._send_message(fixed_message)
            
            return False
            
        except Exception as e:
            logger.error(f"处理消息错误异常: {e}")
            return False

    def _handle_task_error(self, error_info: ErrorInfo) -> bool:
        """处理任务错误"""
        try:
            logger.info(f"处理任务错误 [{error_info.error_id}]")
            
            task_id = error_info.context.get('task_id')
            if task_id and self.client and hasattr(self.client, 'active_tasks'):
                # 尝试重启任务
                if task_id in self.client.active_tasks:
                    task_context = self.client.active_tasks[task_id]
                    if hasattr(task_context, 'restart'):
                        return task_context.restart()
            
            return False
            
        except Exception as e:
            logger.error(f"处理任务错误异常: {e}")
            return False

    def _handle_resource_error(self, error_info: ErrorInfo) -> bool:
        """处理资源错误"""
        try:
            logger.info(f"处理资源错误 [{error_info.error_id}]")
            
            # 尝试清理资源
            self._cleanup_resources()
            
            # 检查资源状态
            if self._check_resource_availability():
                return True
            
            # 进入资源节约模式
            self._enter_resource_saving_mode()
            return False
            
        except Exception as e:
            logger.error(f"处理资源错误异常: {e}")
            return False

    def _handle_validation_error(self, error_info: ErrorInfo) -> bool:
        """处理验证错误"""
        try:
            logger.info(f"处理验证错误 [{error_info.error_id}]")
            
            # 验证错误通常不需要恢复，只需记录
            validation_data = error_info.context.get('validation_data')
            if validation_data:
                logger.warning(f"验证失败的数据: {validation_data}")
            
            return True  # 验证错误标记为已处理
            
        except Exception as e:
            logger.error(f"处理验证错误异常: {e}")
            return False

    def _handle_timeout_error(self, error_info: ErrorInfo) -> bool:
        """处理超时错误"""
        try:
            logger.info(f"处理超时错误 [{error_info.error_id}]")
            
            # 检查网络连接
            if self.client and hasattr(self.client, 'is_connected'):
                if not self.client.is_connected:
                    # 网络问题，尝试重连
                    return self._handle_connection_error(error_info)
            
            # 增加超时时间并重试
            operation = error_info.context.get('operation')
            if operation:
                # 这里可以实现具体的重试逻辑
                pass
            
            return False
            
        except Exception as e:
            logger.error(f"处理超时错误异常: {e}")
            return False

    def _handle_system_error(self, error_info: ErrorInfo) -> bool:
        """处理系统错误"""
        try:
            logger.critical(f"处理系统错误 [{error_info.error_id}]")
            
            # 系统错误通常需要人工干预
            self._create_system_alert(error_info)
            
            # 尝试基本的系统检查
            system_status = self._check_system_status()
            if system_status.get('critical_issues'):
                return False
            
            return True
            
        except Exception as e:
            logger.error(f"处理系统错误异常: {e}")
            return False

    def _handle_unknown_error(self, error_info: ErrorInfo) -> bool:
        """处理未知错误"""
        try:
            logger.warning(f"处理未知错误 [{error_info.error_id}]")
            
            # 未知错误需要更多信息
            self._collect_debug_info(error_info)
            
            # 尝试通用恢复策略
            return self._apply_generic_recovery(error_info)
            
        except Exception as e:
            logger.error(f"处理未知错误异常: {e}")
            return False

    # ========== 辅助方法 ==========
    def _try_fix_message(self, message_data: Any) -> Optional[Dict[str, Any]]:
        """尝试修复消息格式"""
        try:
            if isinstance(message_data, str):
                # 尝试解析JSON
                return json.loads(message_data)
            elif isinstance(message_data, dict):
                # 检查必需字段
                if 'type' not in message_data:
                    return None
                return message_data
            return None
        except:
            return None

    def _cleanup_resources(self):
        """清理资源"""
        try:
            import gc
            gc.collect()  # 强制垃圾回收
            logger.debug("执行资源清理")
        except Exception as e:
            logger.error(f"资源清理失败: {e}")

    def _check_resource_availability(self) -> bool:
        """检查资源可用性"""
        try:
            import psutil
            
            # 检查内存使用率
            memory = psutil.virtual_memory()
            if memory.percent > 90:
                return False
            
            # 检查CPU使用率
            cpu_percent = psutil.cpu_percent(interval=1)
            if cpu_percent > 95:
                return False
            
            return True
        except ImportError:
            # 如果没有psutil，假设资源可用
            return True
        except Exception as e:
            logger.error(f"检查资源可用性失败: {e}")
            return False

    def _enter_resource_saving_mode(self):
        """进入资源节约模式"""
        logger.warning("进入资源节约模式")
        self.degraded_features.add('resource_intensive_operations')

    def _create_system_alert(self, error_info: ErrorInfo):
        """创建系统警报"""
        alert = {
            'timestamp': time.time(),
            'error_id': error_info.error_id,
            'level': 'CRITICAL',
            'message': f"系统错误需要人工干预: {error_info.message}"
        }
        logger.critical(f"系统警报: {alert}")

    def _check_system_status(self) -> Dict[str, Any]:
        """检查系统状态"""
        try:
            import psutil
            
            return {
                'cpu_percent': psutil.cpu_percent(),
                'memory_percent': psutil.virtual_memory().percent,
                'disk_percent': psutil.disk_usage('/').percent,
                'critical_issues': []
            }
        except ImportError:
            return {'critical_issues': []}
        except Exception as e:
            return {'critical_issues': [f"系统状态检查失败: {e}"]}

    def _collect_debug_info(self, error_info: ErrorInfo):
        """收集调试信息"""
        debug_info = {
            'timestamp': time.time(),
            'error_info': {
                'id': error_info.error_id,
                'type': error_info.error_type.value,
                'message': error_info.message,
                'context': error_info.context
            },
            'system_info': self._get_system_info(),
            'client_info': self._get_client_info()
        }
        
        logger.debug(f"调试信息收集 [{error_info.error_id}]: {debug_info}")

    def _apply_generic_recovery(self, error_info: ErrorInfo) -> bool:
        """应用通用恢复策略"""
        try:
            # 通用恢复策略：等待一段时间
            time.sleep(1.0)
            return True
        except Exception as e:
            logger.error(f"通用恢复策略失败: {e}")
            return False

    def _get_system_info(self) -> Dict[str, Any]:
        """获取系统信息"""
        try:
            import platform
            return {
                'platform': platform.platform(),
                'python_version': platform.python_version(),
                'processor': platform.processor()
            }
        except Exception as e:
            return {'error': str(e)}

    def _get_client_info(self) -> Dict[str, Any]:
        """获取客户端信息"""
        if not self.client:
            return {}
        
        try:
            return {
                'vm_id': getattr(self.client, 'vm_id', 'unknown'),
                'status': getattr(self.client, 'status', 'unknown'),
                'is_connected': getattr(self.client, 'is_connected', False),
                'active_tasks': len(getattr(self.client, 'active_tasks', {}))
            }
        except Exception as e:
            return {'error': str(e)}

    def _notify_error_callbacks(self, error_info: ErrorInfo):
        """通知错误回调"""
        for callback in self.error_callbacks:
            try:
                callback(error_info)
            except Exception as e:
                logger.error(f"错误回调执行失败: {e}")

    # ========== 公共接口 ==========
    def add_error_callback(self, callback: Callable):
        """添加错误回调"""
        self.error_callbacks.append(callback)

    def remove_error_callback(self, callback: Callable):
        """移除错误回调"""
        if callback in self.error_callbacks:
            self.error_callbacks.remove(callback)

    def get_error_stats(self) -> Dict[str, Any]:
        """获取错误统计"""
        with self.lock:
            return {
                'total_errors': len(self.error_history),
                'active_errors': len(self.active_errors),
                'error_by_type': dict(self.error_stats),
                'degraded_mode': self.degraded_mode,
                'degraded_features': list(self.degraded_features)
            }

    def get_recent_errors(self, limit: int = 10) -> List[Dict[str, Any]]:
        """获取最近的错误"""
        with self.lock:
            recent = list(self.error_history)[-limit:]
            return [
                {
                    'error_id': error.error_id,
                    'error_type': error.error_type.value,
                    'error_level': error.error_level.value,
                    'message': error.message,
                    'timestamp': error.timestamp,
                    'resolved': error.resolved
                }
                for error in recent
            ]

    def clear_resolved_errors(self):
        """清理已解决的错误"""
        with self.lock:
            # 清理历史记录中的已解决错误（保留最近100个）
            unresolved = [e for e in self.error_history if not e.resolved]
            resolved = [e for e in self.error_history if e.resolved]
            
            # 保留最近的100个已解决错误
            if len(resolved) > 100:
                resolved = resolved[-100:]
            
            self.error_history.clear()
            self.error_history.extend(unresolved + resolved)

    def is_feature_available(self, feature: str) -> bool:
        """检查功能是否可用"""
        return not self.degraded_mode or feature not in self.degraded_features

    def recover_from_degraded_mode(self):
        """尝试从降级模式恢复"""
        if not self.degraded_mode:
            return True
        
        # 检查系统状态
        if self._check_resource_availability():
            self._exit_degraded_mode()
            return True
        
        return False
