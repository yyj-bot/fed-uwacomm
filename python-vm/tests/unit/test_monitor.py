"""
监控系统单元测试
"""

import pytest
import time
import threading
from unittest.mock import Mock, patch, MagicMock
import psutil

from feduwacomm.ml.utils.monitor import Monitor, MetricType, HealthStatus


@pytest.mark.unit
class TestMonitor:
    """监控器测试类"""

    @pytest.fixture
    def mock_client(self):
        """模拟客户端fixture"""
        client = Mock()
        client.vm_id = "test-vm-001"
        client.is_connected = True
        client.active_tasks = {}
        return client

    @pytest.fixture
    def monitor_config(self):
        """监控配置fixture"""
        return {
            'enabled': True,
            'interval': 1,
            'retention_hours': 24,
            'metrics': {
                'system': True,
                'performance': True,
                'tasks': True,
                'network': True
            },
            'thresholds': {
                'cpu_percent': 80.0,
                'memory_percent': 85.0,
                'disk_percent': 90.0,
                'response_time_ms': 1000
            }
        }

    def test_monitor_initialization(self, mock_client, monitor_config):
        """测试监控器初始化"""
        monitor = Monitor(mock_client, monitor_config)
        
        assert monitor.client == mock_client
        assert monitor.config == monitor_config
        assert monitor.is_running == False
        assert len(monitor.metrics_history) == 0

    def test_metric_type_enum(self):
        """测试指标类型枚举"""
        required_types = [
            'SYSTEM', 'PERFORMANCE', 'TASK', 'NETWORK', 'CUSTOM'
        ]
        
        for metric_type in required_types:
            assert hasattr(MetricType, metric_type)

    def test_health_status_enum(self):
        """测试健康状态枚举"""
        required_statuses = [
            'HEALTHY', 'WARNING', 'CRITICAL', 'UNKNOWN'
        ]
        
        for status in required_statuses:
            assert hasattr(HealthStatus, status)

    @patch('psutil.cpu_percent')
    @patch('psutil.virtual_memory')
    @patch('psutil.disk_usage')
    def test_collect_system_metrics(self, mock_disk, mock_memory, mock_cpu, mock_client, monitor_config):
        """测试收集系统指标"""
        # 模拟系统指标
        mock_cpu.return_value = 45.5
        mock_memory.return_value.percent = 62.3
        mock_disk.return_value.percent = 35.8
        
        monitor = Monitor(mock_client, monitor_config)
        
        metrics = monitor.collect_system_metrics()
        
        assert metrics is not None
        assert metrics['cpu_percent'] == 45.5
        assert metrics['memory_percent'] == 62.3
        assert metrics['disk_percent'] == 35.8
        assert 'timestamp' in metrics

    def test_collect_performance_metrics(self, mock_client, monitor_config):
        """测试收集性能指标"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 模拟一些性能数据
        monitor.response_times = [100, 150, 200, 120, 180]
        monitor.throughput_counter = 50
        
        metrics = monitor.collect_performance_metrics()
        
        assert metrics is not None
        assert 'avg_response_time' in metrics
        assert 'throughput' in metrics
        assert 'timestamp' in metrics

    def test_collect_task_metrics(self, mock_client, monitor_config):
        """测试收集任务指标"""
        # 模拟活跃任务
        mock_task1 = Mock()
        mock_task1.get_statistics.return_value = {
            'status': 'TRAINING',
            'progress': 0.6,
            'cpu_usage': 30.0
        }
        
        mock_task2 = Mock()
        mock_task2.get_statistics.return_value = {
            'status': 'WAITING',
            'progress': 0.0,
            'cpu_usage': 5.0
        }
        
        mock_client.active_tasks = {
            'task-001': mock_task1,
            'task-002': mock_task2
        }
        
        monitor = Monitor(mock_client, monitor_config)
        
        metrics = monitor.collect_task_metrics()
        
        assert metrics is not None
        assert metrics['active_tasks'] == 2
        assert metrics['training_tasks'] == 1
        assert metrics['waiting_tasks'] == 1
        assert 'timestamp' in metrics

    def test_collect_network_metrics(self, mock_client, monitor_config):
        """测试收集网络指标"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 模拟网络统计
        monitor.bytes_sent = 1024000
        monitor.bytes_received = 2048000
        monitor.messages_sent = 100
        monitor.messages_received = 150
        
        metrics = monitor.collect_network_metrics()
        
        assert metrics is not None
        assert metrics['bytes_sent'] == 1024000
        assert metrics['bytes_received'] == 2048000
        assert metrics['messages_sent'] == 100
        assert metrics['messages_received'] == 150
        assert 'timestamp' in metrics

    def test_start_stop_monitoring(self, mock_client, monitor_config):
        """测试启动和停止监控"""
        monitor_config['interval'] = 0.1  # 快速测试
        monitor = Monitor(mock_client, monitor_config)
        
        # 启动监控
        monitor.start()
        assert monitor.is_running == True
        
        # 等待一些指标收集
        time.sleep(0.3)
        
        # 停止监控
        monitor.stop()
        assert monitor.is_running == False
        
        # 验证指标被收集
        assert len(monitor.metrics_history) > 0

    def test_health_check(self, mock_client, monitor_config):
        """测试健康检查"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 模拟正常系统状态
        with patch.object(monitor, 'collect_system_metrics') as mock_collect:
            mock_collect.return_value = {
                'cpu_percent': 50.0,
                'memory_percent': 60.0,
                'disk_percent': 40.0,
                'timestamp': time.time()
            }
            
            health = monitor.check_health()
            
            assert health['status'] == HealthStatus.HEALTHY
            assert health['score'] > 0.7

    def test_health_check_warning(self, mock_client, monitor_config):
        """测试健康检查警告状态"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 模拟高CPU使用率
        with patch.object(monitor, 'collect_system_metrics') as mock_collect:
            mock_collect.return_value = {
                'cpu_percent': 85.0,  # 超过阈值
                'memory_percent': 60.0,
                'disk_percent': 40.0,
                'timestamp': time.time()
            }
            
            health = monitor.check_health()
            
            assert health['status'] == HealthStatus.WARNING

    def test_health_check_critical(self, mock_client, monitor_config):
        """测试健康检查危险状态"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 模拟多个指标超过阈值
        with patch.object(monitor, 'collect_system_metrics') as mock_collect:
            mock_collect.return_value = {
                'cpu_percent': 95.0,  # 超过阈值
                'memory_percent': 90.0,  # 超过阈值
                'disk_percent': 95.0,  # 超过阈值
                'timestamp': time.time()
            }
            
            health = monitor.check_health()
            
            assert health['status'] == HealthStatus.CRITICAL

    def test_alert_generation(self, mock_client, monitor_config):
        """测试告警生成"""
        monitor = Monitor(mock_client, monitor_config)
        
        alerts = []
        
        def alert_handler(alert):
            alerts.append(alert)
        
        monitor.register_alert_handler(alert_handler)
        
        # 模拟超过阈值的指标
        metrics = {
            'cpu_percent': 90.0,  # 超过80%阈值
            'memory_percent': 60.0,
            'disk_percent': 40.0,
            'timestamp': time.time()
        }
        
        monitor._check_thresholds(metrics)
        
        # 验证告警被生成
        assert len(alerts) == 1
        assert alerts[0]['metric'] == 'cpu_percent'
        assert alerts[0]['severity'] == 'WARNING'

    def test_metrics_aggregation(self, mock_client, monitor_config):
        """测试指标聚合"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 添加一些历史指标
        timestamps = [time.time() - i * 60 for i in range(10)]  # 10分钟的数据
        for i, ts in enumerate(timestamps):
            metrics = {
                'cpu_percent': 50.0 + i,
                'memory_percent': 60.0 + i,
                'timestamp': ts
            }
            monitor.metrics_history.append(metrics)
        
        # 聚合最近5分钟的数据
        aggregated = monitor.aggregate_metrics(duration_minutes=5)
        
        assert aggregated is not None
        assert 'avg_cpu_percent' in aggregated
        assert 'max_memory_percent' in aggregated
        assert 'min_cpu_percent' in aggregated

    def test_metrics_retention(self, mock_client, monitor_config):
        """测试指标保留策略"""
        monitor_config['retention_hours'] = 1  # 1小时保留
        monitor = Monitor(mock_client, monitor_config)
        
        # 添加过期的指标
        old_timestamp = time.time() - 2 * 3600  # 2小时前
        old_metrics = {
            'cpu_percent': 50.0,
            'timestamp': old_timestamp
        }
        monitor.metrics_history.append(old_metrics)
        
        # 添加新的指标
        new_metrics = {
            'cpu_percent': 60.0,
            'timestamp': time.time()
        }
        monitor.metrics_history.append(new_metrics)
        
        # 执行清理
        monitor._cleanup_old_metrics()
        
        # 验证过期指标被清理
        assert len(monitor.metrics_history) == 1
        assert monitor.metrics_history[0]['timestamp'] == new_metrics['timestamp']

    def test_custom_metrics(self, mock_client, monitor_config):
        """测试自定义指标"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 注册自定义指标收集器
        def custom_collector():
            return {
                'custom_metric': 42.0,
                'another_metric': 'test_value'
            }
        
        monitor.register_custom_collector('custom', custom_collector)
        
        # 收集指标
        metrics = monitor.collect_all_metrics()
        
        assert 'custom' in metrics
        assert metrics['custom']['custom_metric'] == 42.0

    def test_performance_tracking(self, mock_client, monitor_config):
        """测试性能跟踪"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 记录一些响应时间
        response_times = [100, 150, 200, 120, 180]
        for rt in response_times:
            monitor.record_response_time(rt)
        
        # 记录吞吐量
        for i in range(10):
            monitor.record_throughput()
        
        metrics = monitor.collect_performance_metrics()
        
        assert metrics['avg_response_time'] == sum(response_times) / len(response_times)
        assert metrics['throughput'] == 10

    def test_resource_monitoring(self, mock_client, monitor_config):
        """测试资源监控"""
        monitor = Monitor(mock_client, monitor_config)
        
        with patch('psutil.Process') as mock_process:
            mock_proc = Mock()
            mock_proc.memory_info.return_value.rss = 1024 * 1024 * 100  # 100MB
            mock_proc.cpu_percent.return_value = 25.5
            mock_process.return_value = mock_proc
            
            resources = monitor.get_process_resources()
            
            assert resources['memory_mb'] == 100
            assert resources['cpu_percent'] == 25.5

    def test_trend_analysis(self, mock_client, monitor_config):
        """测试趋势分析"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 添加趋势数据（CPU使用率递增）
        for i in range(10):
            metrics = {
                'cpu_percent': 50.0 + i * 2,  # 递增趋势
                'timestamp': time.time() - (10 - i) * 60
            }
            monitor.metrics_history.append(metrics)
        
        trends = monitor.analyze_trends('cpu_percent')
        
        assert trends is not None
        assert trends['direction'] == 'increasing'
        assert trends['slope'] > 0

    def test_anomaly_detection(self, mock_client, monitor_config):
        """测试异常检测"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 添加正常数据
        for i in range(20):
            metrics = {
                'cpu_percent': 50.0 + (i % 5),  # 正常波动
                'timestamp': time.time() - (20 - i) * 60
            }
            monitor.metrics_history.append(metrics)
        
        # 添加异常数据
        anomaly_metrics = {
            'cpu_percent': 95.0,  # 异常高值
            'timestamp': time.time()
        }
        monitor.metrics_history.append(anomaly_metrics)
        
        anomalies = monitor.detect_anomalies('cpu_percent')
        
        assert len(anomalies) > 0
        assert anomalies[0]['value'] == 95.0

    def test_monitoring_dashboard_data(self, mock_client, monitor_config):
        """测试监控仪表板数据"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 模拟一些数据
        monitor.metrics_history = [
            {
                'cpu_percent': 50.0,
                'memory_percent': 60.0,
                'timestamp': time.time() - 300
            },
            {
                'cpu_percent': 55.0,
                'memory_percent': 65.0,
                'timestamp': time.time()
            }
        ]
        
        dashboard_data = monitor.get_dashboard_data()
        
        assert dashboard_data is not None
        assert 'current_metrics' in dashboard_data
        assert 'health_status' in dashboard_data
        assert 'alerts' in dashboard_data
        assert 'trends' in dashboard_data

    def test_export_metrics(self, mock_client, monitor_config, tmp_path):
        """测试导出指标"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 添加一些指标数据
        for i in range(5):
            metrics = {
                'cpu_percent': 50.0 + i,
                'memory_percent': 60.0 + i,
                'timestamp': time.time() - (5 - i) * 60
            }
            monitor.metrics_history.append(metrics)
        
        # 导出到文件
        export_file = tmp_path / "metrics_export.json"
        result = monitor.export_metrics(str(export_file))
        
        assert result == True
        assert export_file.exists()
        
        # 验证导出内容
        import json
        with open(export_file, 'r') as f:
            exported_data = json.load(f)
        
        assert len(exported_data) == 5

    def test_concurrent_monitoring(self, mock_client, monitor_config):
        """测试并发监控"""
        monitor_config['interval'] = 0.05  # 快速测试
        monitor = Monitor(mock_client, monitor_config)
        
        # 启动监控
        monitor.start()
        
        # 同时进行其他操作
        results = []
        
        def concurrent_operation():
            for i in range(10):
                health = monitor.check_health()
                results.append(health)
                time.sleep(0.01)
        
        thread = threading.Thread(target=concurrent_operation)
        thread.start()
        
        time.sleep(0.2)
        
        thread.join()
        monitor.stop()
        
        # 验证并发操作成功
        assert len(results) == 10
        assert all(result is not None for result in results)

    def test_memory_usage_optimization(self, mock_client, monitor_config):
        """测试内存使用优化"""
        monitor_config['retention_hours'] = 0.1  # 短保留时间
        monitor = Monitor(mock_client, monitor_config)
        
        # 添加大量历史数据
        for i in range(1000):
            metrics = {
                'cpu_percent': 50.0,
                'memory_percent': 60.0,
                'large_data': list(range(100)),  # 模拟大数据
                'timestamp': time.time() - i * 60
            }
            monitor.metrics_history.append(metrics)
        
        initial_count = len(monitor.metrics_history)
        
        # 执行内存优化
        monitor._optimize_memory_usage()
        
        # 验证内存被优化
        assert len(monitor.metrics_history) < initial_count

    @pytest.mark.slow
    def test_monitoring_performance(self, mock_client, monitor_config):
        """测试监控性能"""
        monitor_config['interval'] = 0.01  # 高频监控
        monitor = Monitor(mock_client, monitor_config)
        
        start_time = time.time()
        
        # 运行监控一段时间
        monitor.start()
        time.sleep(1.0)
        monitor.stop()
        
        end_time = time.time()
        
        # 验证监控开销合理
        overhead = (end_time - start_time) - 1.0
        assert overhead < 0.1  # 开销应该小于100ms

    def test_alert_throttling(self, mock_client, monitor_config):
        """测试告警限流"""
        monitor = Monitor(mock_client, monitor_config)
        monitor.alert_throttle_seconds = 60  # 1分钟限流
        
        alerts = []
        
        def alert_handler(alert):
            alerts.append(alert)
        
        monitor.register_alert_handler(alert_handler)
        
        # 快速触发多个相同告警
        for i in range(5):
            monitor._trigger_alert('cpu_high', 'CPU usage high', 'WARNING')
        
        # 验证告警被限流
        assert len(alerts) == 1  # 只有第一个告警被发送

    def test_monitoring_configuration_update(self, mock_client, monitor_config):
        """测试监控配置更新"""
        monitor = Monitor(mock_client, monitor_config)
        
        # 更新配置
        new_config = monitor_config.copy()
        new_config['interval'] = 5
        new_config['thresholds']['cpu_percent'] = 70.0
        
        result = monitor.update_config(new_config)
        
        assert result == True
        assert monitor.config['interval'] == 5
        assert monitor.config['thresholds']['cpu_percent'] == 70.0
