"""
监控器 - 实现性能监控、资源监控、调试工具
基于v1.4协议的Python VM监控系统
"""

import logging
import time
import threading
import json
import os
from typing import Dict, Any, List, Optional, Callable
from dataclasses import dataclass, asdict
from collections import deque, defaultdict
from enum import Enum
import statistics

logger = logging.getLogger(__name__)


class MetricType(Enum):
    """指标类型枚举"""
    COUNTER = "COUNTER"         # 计数器（累加）
    GAUGE = "GAUGE"             # 仪表（瞬时值）
    HISTOGRAM = "HISTOGRAM"     # 直方图（分布）
    TIMER = "TIMER"             # 计时器


@dataclass
class MetricPoint:
    """指标数据点"""
    timestamp: float
    value: float
    labels: Dict[str, str] = None
    
    def __post_init__(self):
        if self.labels is None:
            self.labels = {}


@dataclass
class PerformanceMetrics:
    """性能指标数据类"""
    timestamp: float
    cpu_percent: float
    memory_percent: float
    memory_used_mb: float
    disk_percent: float
    network_bytes_sent: int
    network_bytes_recv: int
    active_threads: int
    active_tasks: int
    message_queue_size: int
    websocket_latency_ms: float = 0.0
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class TaskMetrics:
    """任务指标数据类"""
    task_id: str
    status: str
    start_time: float
    last_activity: float
    rounds_completed: int
    total_rounds: int
    training_time_avg: float
    accuracy: float = 0.0
    error_count: int = 0
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class Monitor:
    """监控器 - 性能监控、资源监控、调试工具"""

    def __init__(self, client=None, config: Optional[Dict[str, Any]] = None):
        """
        初始化监控器
        
        Args:
            client: WebSocket客户端实例
            config: 监控配置
        """
        self.client = client
        self.config = config or {}
        
        # 监控配置
        self.monitoring_enabled = self.config.get('enabled', True)
        self.collection_interval = self.config.get('interval', 60)  # 秒
        self.metrics_retention = self.config.get('retention_hours', 24)  # 小时
        self.max_metrics_points = int(self.metrics_retention * 3600 / self.collection_interval)
        
        # 指标存储
        self.performance_metrics: deque = deque(maxlen=self.max_metrics_points)
        self.task_metrics: Dict[str, TaskMetrics] = {}
        self.custom_metrics: Dict[str, deque] = defaultdict(lambda: deque(maxlen=1000))
        
        # 实时统计
        self.message_stats = {
            'total_sent': 0,
            'total_received': 0,
            'errors': 0,
            'last_message_time': 0
        }
        
        self.connection_stats = {
            'connect_time': 0,
            'disconnect_count': 0,
            'reconnect_count': 0,
            'total_uptime': 0
        }
        
        # 性能基线
        self.performance_baseline = {
            'cpu_threshold': 80.0,
            'memory_threshold': 85.0,
            'disk_threshold': 90.0,
            'latency_threshold': 1000.0  # ms
        }
        
        # 警报配置
        self.alert_callbacks: List[Callable] = []
        self.alert_history: deque = deque(maxlen=100)
        
        # 监控线程
        self.monitoring_thread: Optional[threading.Thread] = None
        self.monitoring_active = False
        
        # 线程锁
        self.lock = threading.RLock()
        
        # 调试模式
        self.debug_mode = self.config.get('debug', False)
        self.debug_log: deque = deque(maxlen=1000)
        
        logger.info(f"监控器初始化完成 - 间隔: {self.collection_interval}s, 保留: {self.metrics_retention}h")

    def start_monitoring(self):
        """启动监控"""
        if not self.monitoring_enabled or self.monitoring_active:
            return
        
        self.monitoring_active = True
        self.monitoring_thread = threading.Thread(target=self._monitoring_loop, daemon=True)
        self.monitoring_thread.start()
        
        logger.info("监控系统启动")

    def stop_monitoring(self):
        """停止监控"""
        self.monitoring_active = False
        if self.monitoring_thread:
            self.monitoring_thread.join(timeout=5.0)
        
        logger.info("监控系统停止")

    def _monitoring_loop(self):
        """监控循环"""
        while self.monitoring_active:
            try:
                # 收集性能指标
                self._collect_performance_metrics()
                
                # 收集任务指标
                self._collect_task_metrics()
                
                # 检查警报条件
                self._check_alerts()
                
                # 清理过期数据
                self._cleanup_old_metrics()
                
                time.sleep(self.collection_interval)
                
            except Exception as e:
                logger.error(f"监控循环异常: {e}")
                time.sleep(5.0)

    def _collect_performance_metrics(self):
        """收集性能指标"""
        try:
            metrics = self._get_system_metrics()
            
            with self.lock:
                self.performance_metrics.append(metrics)
            
            # 调试日志
            if self.debug_mode:
                self._add_debug_log(f"性能指标收集: CPU={metrics.cpu_percent:.1f}%, 内存={metrics.memory_percent:.1f}%")
            
        except Exception as e:
            logger.error(f"收集性能指标失败: {e}")

    def _get_system_metrics(self) -> PerformanceMetrics:
        """获取系统指标"""
        try:
            import psutil
            
            # CPU和内存
            cpu_percent = psutil.cpu_percent(interval=1)
            memory = psutil.virtual_memory()
            
            # 磁盘
            disk = psutil.disk_usage('/')
            
            # 网络
            network = psutil.net_io_counters()
            
            # 线程数
            active_threads = threading.active_count()
            
            # 客户端相关指标
            active_tasks = len(self.client.active_tasks) if self.client and hasattr(self.client, 'active_tasks') else 0
            message_queue_size = getattr(self.client, 'message_queue_size', 0) if self.client else 0
            
            # WebSocket延迟
            websocket_latency = self._measure_websocket_latency()
            
            return PerformanceMetrics(
                timestamp=time.time(),
                cpu_percent=cpu_percent,
                memory_percent=memory.percent,
                memory_used_mb=memory.used / (1024 * 1024),
                disk_percent=disk.percent,
                network_bytes_sent=network.bytes_sent,
                network_bytes_recv=network.bytes_recv,
                active_threads=active_threads,
                active_tasks=active_tasks,
                message_queue_size=message_queue_size,
                websocket_latency_ms=websocket_latency
            )
            
        except ImportError:
            # 如果没有psutil，返回基本指标
            return PerformanceMetrics(
                timestamp=time.time(),
                cpu_percent=0.0,
                memory_percent=0.0,
                memory_used_mb=0.0,
                disk_percent=0.0,
                network_bytes_sent=0,
                network_bytes_recv=0,
                active_threads=threading.active_count(),
                active_tasks=len(self.client.active_tasks) if self.client and hasattr(self.client, 'active_tasks') else 0,
                message_queue_size=0
            )
        except Exception as e:
            logger.error(f"获取系统指标失败: {e}")
            return PerformanceMetrics(
                timestamp=time.time(),
                cpu_percent=0.0,
                memory_percent=0.0,
                memory_used_mb=0.0,
                disk_percent=0.0,
                network_bytes_sent=0,
                network_bytes_recv=0,
                active_threads=0,
                active_tasks=0,
                message_queue_size=0
            )

    def _measure_websocket_latency(self) -> float:
        """测量WebSocket延迟"""
        try:
            if not self.client or not hasattr(self.client, 'is_connected') or not self.client.is_connected:
                return 0.0
            
            # 简单的ping测量（如果客户端支持）
            if hasattr(self.client, 'ping'):
                start_time = time.time()
                success = self.client.ping()
                if success:
                    return (time.time() - start_time) * 1000
            
            return 0.0
            
        except Exception as e:
            logger.debug(f"测量WebSocket延迟失败: {e}")
            return 0.0

    def _collect_task_metrics(self):
        """收集任务指标"""
        try:
            if not self.client or not hasattr(self.client, 'active_tasks'):
                return
            
            current_time = time.time()
            
            with self.lock:
                for task_id, task_context in self.client.active_tasks.items():
                    try:
                        # 获取任务统计信息
                        if hasattr(task_context, 'get_statistics'):
                            stats = task_context.get_statistics()
                        else:
                            stats = self._extract_basic_task_stats(task_context)
                        
                        # 创建或更新任务指标
                        task_metrics = TaskMetrics(
                            task_id=task_id,
                            status=getattr(task_context, 'status', 'UNKNOWN').value if hasattr(getattr(task_context, 'status', None), 'value') else str(getattr(task_context, 'status', 'UNKNOWN')),
                            start_time=getattr(task_context, 'start_time', current_time),
                            last_activity=getattr(task_context, 'last_activity', current_time),
                            rounds_completed=stats.get('completed_rounds', 0),
                            total_rounds=getattr(task_context, 'total_rounds', 0),
                            training_time_avg=stats.get('average_training_time', 0.0),
                            accuracy=stats.get('accuracy', 0.0),
                            error_count=stats.get('error_count', 0)
                        )
                        
                        self.task_metrics[task_id] = task_metrics
                        
                    except Exception as e:
                        logger.error(f"收集任务 {task_id} 指标失败: {e}")
            
        except Exception as e:
            logger.error(f"收集任务指标失败: {e}")

    def _extract_basic_task_stats(self, task_context) -> Dict[str, Any]:
        """提取基本任务统计信息"""
        return {
            'completed_rounds': getattr(task_context, 'current_round', 0),
            'average_training_time': 0.0,
            'accuracy': 0.0,
            'error_count': 0
        }

    def _check_alerts(self):
        """检查警报条件"""
        try:
            if not self.performance_metrics:
                return
            
            latest_metrics = self.performance_metrics[-1]
            alerts = []
            
            # CPU警报
            if latest_metrics.cpu_percent > self.performance_baseline['cpu_threshold']:
                alerts.append({
                    'type': 'CPU_HIGH',
                    'level': 'WARNING',
                    'message': f'CPU使用率过高: {latest_metrics.cpu_percent:.1f}%',
                    'value': latest_metrics.cpu_percent,
                    'threshold': self.performance_baseline['cpu_threshold']
                })
            
            # 内存警报
            if latest_metrics.memory_percent > self.performance_baseline['memory_threshold']:
                alerts.append({
                    'type': 'MEMORY_HIGH',
                    'level': 'WARNING',
                    'message': f'内存使用率过高: {latest_metrics.memory_percent:.1f}%',
                    'value': latest_metrics.memory_percent,
                    'threshold': self.performance_baseline['memory_threshold']
                })
            
            # 磁盘警报
            if latest_metrics.disk_percent > self.performance_baseline['disk_threshold']:
                alerts.append({
                    'type': 'DISK_HIGH',
                    'level': 'CRITICAL',
                    'message': f'磁盘使用率过高: {latest_metrics.disk_percent:.1f}%',
                    'value': latest_metrics.disk_percent,
                    'threshold': self.performance_baseline['disk_threshold']
                })
            
            # 延迟警报
            if latest_metrics.websocket_latency_ms > self.performance_baseline['latency_threshold']:
                alerts.append({
                    'type': 'LATENCY_HIGH',
                    'level': 'WARNING',
                    'message': f'WebSocket延迟过高: {latest_metrics.websocket_latency_ms:.1f}ms',
                    'value': latest_metrics.websocket_latency_ms,
                    'threshold': self.performance_baseline['latency_threshold']
                })
            
            # 处理警报
            for alert in alerts:
                self._handle_alert(alert)
            
        except Exception as e:
            logger.error(f"检查警报失败: {e}")

    def _handle_alert(self, alert: Dict[str, Any]):
        """处理警报"""
        try:
            alert['timestamp'] = time.time()
            
            with self.lock:
                self.alert_history.append(alert)
            
            # 记录警报日志
            level = alert['level']
            message = alert['message']
            
            if level == 'CRITICAL':
                logger.critical(f"监控警报: {message}")
            elif level == 'WARNING':
                logger.warning(f"监控警报: {message}")
            else:
                logger.info(f"监控警报: {message}")
            
            # 触发警报回调
            for callback in self.alert_callbacks:
                try:
                    callback(alert)
                except Exception as e:
                    logger.error(f"警报回调执行失败: {e}")
            
            # 调试日志
            if self.debug_mode:
                self._add_debug_log(f"警报触发: {alert['type']} - {message}")
            
        except Exception as e:
            logger.error(f"处理警报失败: {e}")

    def _cleanup_old_metrics(self):
        """清理过期指标"""
        try:
            current_time = time.time()
            retention_seconds = self.metrics_retention * 3600
            
            with self.lock:
                # 清理性能指标（deque会自动限制大小）
                
                # 清理自定义指标
                for metric_name, points in self.custom_metrics.items():
                    while points and (current_time - points[0].timestamp) > retention_seconds:
                        points.popleft()
                
                # 清理已完成任务的指标
                completed_tasks = []
                for task_id, metrics in self.task_metrics.items():
                    if metrics.status in ['COMPLETED', 'ERROR', 'CANCELLED']:
                        if (current_time - metrics.last_activity) > 3600:  # 1小时后清理
                            completed_tasks.append(task_id)
                
                for task_id in completed_tasks:
                    del self.task_metrics[task_id]
            
        except Exception as e:
            logger.error(f"清理过期指标失败: {e}")

    # ========== 指标记录接口 ==========
    def record_message_sent(self, message_type: str, size: int):
        """记录发送的消息"""
        with self.lock:
            self.message_stats['total_sent'] += 1
            self.message_stats['last_message_time'] = time.time()
        
        # 记录自定义指标
        self.record_metric('messages_sent', 1, {'type': message_type})
        self.record_metric('message_size_bytes', size, {'type': message_type, 'direction': 'sent'})

    def record_message_received(self, message_type: str, size: int):
        """记录接收的消息"""
        with self.lock:
            self.message_stats['total_received'] += 1
            self.message_stats['last_message_time'] = time.time()
        
        # 记录自定义指标
        self.record_metric('messages_received', 1, {'type': message_type})
        self.record_metric('message_size_bytes', size, {'type': message_type, 'direction': 'received'})

    def record_message_error(self, error_type: str):
        """记录消息错误"""
        with self.lock:
            self.message_stats['errors'] += 1
        
        self.record_metric('message_errors', 1, {'error_type': error_type})

    def record_connection_event(self, event_type: str):
        """记录连接事件"""
        current_time = time.time()
        
        with self.lock:
            if event_type == 'connect':
                self.connection_stats['connect_time'] = current_time
            elif event_type == 'disconnect':
                self.connection_stats['disconnect_count'] += 1
                if self.connection_stats['connect_time'] > 0:
                    uptime = current_time - self.connection_stats['connect_time']
                    self.connection_stats['total_uptime'] += uptime
            elif event_type == 'reconnect':
                self.connection_stats['reconnect_count'] += 1
        
        self.record_metric('connection_events', 1, {'event_type': event_type})

    def record_metric(self, name: str, value: float, labels: Optional[Dict[str, str]] = None):
        """记录自定义指标"""
        try:
            point = MetricPoint(
                timestamp=time.time(),
                value=value,
                labels=labels or {}
            )
            
            with self.lock:
                self.custom_metrics[name].append(point)
            
            if self.debug_mode:
                self._add_debug_log(f"指标记录: {name}={value} {labels or ''}")
            
        except Exception as e:
            logger.error(f"记录指标失败: {e}")

    def start_timer(self, name: str, labels: Optional[Dict[str, str]] = None) -> Callable:
        """启动计时器"""
        start_time = time.time()
        
        def stop_timer():
            duration = time.time() - start_time
            self.record_metric(f"{name}_duration_seconds", duration, labels)
            return duration
        
        return stop_timer

    # ========== 查询接口 ==========
    def get_performance_summary(self, minutes: int = 60) -> Dict[str, Any]:
        """获取性能摘要"""
        try:
            cutoff_time = time.time() - (minutes * 60)
            
            with self.lock:
                recent_metrics = [m for m in self.performance_metrics if m.timestamp >= cutoff_time]
            
            if not recent_metrics:
                return {}
            
            # 计算统计信息
            cpu_values = [m.cpu_percent for m in recent_metrics]
            memory_values = [m.memory_percent for m in recent_metrics]
            latency_values = [m.websocket_latency_ms for m in recent_metrics if m.websocket_latency_ms > 0]
            
            return {
                'time_range_minutes': minutes,
                'sample_count': len(recent_metrics),
                'cpu': {
                    'avg': statistics.mean(cpu_values) if cpu_values else 0,
                    'max': max(cpu_values) if cpu_values else 0,
                    'min': min(cpu_values) if cpu_values else 0
                },
                'memory': {
                    'avg': statistics.mean(memory_values) if memory_values else 0,
                    'max': max(memory_values) if memory_values else 0,
                    'min': min(memory_values) if memory_values else 0
                },
                'latency': {
                    'avg': statistics.mean(latency_values) if latency_values else 0,
                    'max': max(latency_values) if latency_values else 0,
                    'min': min(latency_values) if latency_values else 0
                } if latency_values else None,
                'latest': recent_metrics[-1].to_dict() if recent_metrics else None
            }
            
        except Exception as e:
            logger.error(f"获取性能摘要失败: {e}")
            return {}

    def get_task_summary(self) -> Dict[str, Any]:
        """获取任务摘要"""
        try:
            with self.lock:
                task_metrics = dict(self.task_metrics)
            
            if not task_metrics:
                return {'total_tasks': 0}
            
            # 按状态分组
            status_counts = defaultdict(int)
            total_rounds = 0
            total_errors = 0
            
            for metrics in task_metrics.values():
                status_counts[metrics.status] += 1
                total_rounds += metrics.rounds_completed
                total_errors += metrics.error_count
            
            return {
                'total_tasks': len(task_metrics),
                'status_distribution': dict(status_counts),
                'total_rounds_completed': total_rounds,
                'total_errors': total_errors,
                'tasks': [metrics.to_dict() for metrics in task_metrics.values()]
            }
            
        except Exception as e:
            logger.error(f"获取任务摘要失败: {e}")
            return {}

    def get_message_stats(self) -> Dict[str, Any]:
        """获取消息统计"""
        with self.lock:
            return self.message_stats.copy()

    def get_connection_stats(self) -> Dict[str, Any]:
        """获取连接统计"""
        with self.lock:
            stats = self.connection_stats.copy()
            
            # 计算平均在线时间
            if stats['disconnect_count'] > 0:
                stats['avg_uptime'] = stats['total_uptime'] / stats['disconnect_count']
            else:
                stats['avg_uptime'] = 0
            
            return stats

    def get_recent_alerts(self, limit: int = 10) -> List[Dict[str, Any]]:
        """获取最近的警报"""
        with self.lock:
            return list(self.alert_history)[-limit:]

    def get_metric_values(self, name: str, minutes: int = 60) -> List[Dict[str, Any]]:
        """获取指标值"""
        try:
            cutoff_time = time.time() - (minutes * 60)
            
            with self.lock:
                if name not in self.custom_metrics:
                    return []
                
                recent_points = [
                    {
                        'timestamp': point.timestamp,
                        'value': point.value,
                        'labels': point.labels
                    }
                    for point in self.custom_metrics[name]
                    if point.timestamp >= cutoff_time
                ]
            
            return recent_points
            
        except Exception as e:
            logger.error(f"获取指标值失败: {e}")
            return []

    def get_health_status(self) -> Dict[str, Any]:
        """获取健康状态"""
        try:
            # 获取最新性能指标
            latest_metrics = self.performance_metrics[-1] if self.performance_metrics else None
            
            # 计算健康分数
            health_score = 100
            issues = []
            
            if latest_metrics:
                if latest_metrics.cpu_percent > self.performance_baseline['cpu_threshold']:
                    health_score -= 20
                    issues.append(f"CPU使用率过高: {latest_metrics.cpu_percent:.1f}%")
                
                if latest_metrics.memory_percent > self.performance_baseline['memory_threshold']:
                    health_score -= 25
                    issues.append(f"内存使用率过高: {latest_metrics.memory_percent:.1f}%")
                
                if latest_metrics.disk_percent > self.performance_baseline['disk_threshold']:
                    health_score -= 30
                    issues.append(f"磁盘使用率过高: {latest_metrics.disk_percent:.1f}%")
                
                if latest_metrics.websocket_latency_ms > self.performance_baseline['latency_threshold']:
                    health_score -= 15
                    issues.append(f"网络延迟过高: {latest_metrics.websocket_latency_ms:.1f}ms")
            
            # 检查连接状态
            is_connected = self.client.is_connected if self.client and hasattr(self.client, 'is_connected') else False
            if not is_connected:
                health_score -= 40
                issues.append("WebSocket连接断开")
            
            # 确定健康状态
            if health_score >= 80:
                status = "HEALTHY"
            elif health_score >= 60:
                status = "WARNING"
            elif health_score >= 40:
                status = "DEGRADED"
            else:
                status = "CRITICAL"
            
            return {
                'status': status,
                'score': max(0, health_score),
                'issues': issues,
                'timestamp': time.time(),
                'monitoring_active': self.monitoring_active,
                'metrics_available': len(self.performance_metrics) > 0
            }
            
        except Exception as e:
            logger.error(f"获取健康状态失败: {e}")
            return {
                'status': 'UNKNOWN',
                'score': 0,
                'issues': [f"健康检查失败: {e}"],
                'timestamp': time.time(),
                'monitoring_active': False,
                'metrics_available': False
            }

    # ========== 调试工具 ==========
    def _add_debug_log(self, message: str):
        """添加调试日志"""
        if self.debug_mode:
            debug_entry = {
                'timestamp': time.time(),
                'message': message
            }
            self.debug_log.append(debug_entry)

    def get_debug_logs(self, limit: int = 100) -> List[Dict[str, Any]]:
        """获取调试日志"""
        with self.lock:
            return list(self.debug_log)[-limit:]

    def enable_debug_mode(self):
        """启用调试模式"""
        self.debug_mode = True
        logger.info("监控调试模式已启用")

    def disable_debug_mode(self):
        """禁用调试模式"""
        self.debug_mode = False
        logger.info("监控调试模式已禁用")

    # ========== 配置管理 ==========
    def update_thresholds(self, thresholds: Dict[str, float]):
        """更新性能阈值"""
        try:
            for key, value in thresholds.items():
                if key in self.performance_baseline:
                    self.performance_baseline[key] = value
                    logger.info(f"更新性能阈值: {key} = {value}")
            
        except Exception as e:
            logger.error(f"更新性能阈值失败: {e}")

    def add_alert_callback(self, callback: Callable):
        """添加警报回调"""
        self.alert_callbacks.append(callback)

    def remove_alert_callback(self, callback: Callable):
        """移除警报回调"""
        if callback in self.alert_callbacks:
            self.alert_callbacks.remove(callback)

    def export_metrics(self, format_type: str = 'json') -> str:
        """导出指标数据"""
        try:
            data = {
                'timestamp': time.time(),
                'performance_metrics': [m.to_dict() for m in self.performance_metrics],
                'task_metrics': {k: v.to_dict() for k, v in self.task_metrics.items()},
                'message_stats': self.message_stats,
                'connection_stats': self.get_connection_stats(),
                'custom_metrics': {
                    name: [{'timestamp': p.timestamp, 'value': p.value, 'labels': p.labels} for p in points]
                    for name, points in self.custom_metrics.items()
                }
            }
            
            if format_type.lower() == 'json':
                return json.dumps(data, indent=2)
            else:
                return str(data)
            
        except Exception as e:
            logger.error(f"导出指标失败: {e}")
            return "{}"

    def get_monitoring_info(self) -> Dict[str, Any]:
        """获取监控信息"""
        return {
            'enabled': self.monitoring_enabled,
            'active': self.monitoring_active,
            'collection_interval': self.collection_interval,
            'retention_hours': self.metrics_retention,
            'debug_mode': self.debug_mode,
            'performance_baseline': self.performance_baseline,
            'metrics_count': {
                'performance': len(self.performance_metrics),
                'tasks': len(self.task_metrics),
                'custom': {name: len(points) for name, points in self.custom_metrics.items()},
                'alerts': len(self.alert_history)
            }
        }
