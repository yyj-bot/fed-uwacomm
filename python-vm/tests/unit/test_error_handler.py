"""
错误处理机制单元测试
"""

import pytest
import time
import threading
from unittest.mock import Mock, patch, MagicMock
from enum import Enum

from feduwacomm.ml.utils.error_handler import ErrorHandler, ErrorType, ErrorSeverity


@pytest.mark.unit
class TestErrorHandler:
    """错误处理器测试类"""

    @pytest.fixture
    def mock_client(self):
        """模拟客户端fixture"""
        client = Mock()
        client.vm_id = "test-vm-001"
        client.is_connected = True
        client.reconnect = Mock(return_value=True)
        client.disconnect = Mock()
        return client

    def test_error_handler_initialization(self, mock_client):
        """测试错误处理器初始化"""
        handler = ErrorHandler(mock_client)
        
        assert handler.client == mock_client
        assert handler.error_count == 0
        assert handler.is_enabled == True
        assert len(handler.error_history) == 0

    def test_error_type_enum(self):
        """测试错误类型枚举"""
        required_types = [
            'CONNECTION_ERROR', 'AUTHENTICATION_ERROR', 'PROTOCOL_ERROR',
            'TASK_ERROR', 'RESOURCE_ERROR', 'TIMEOUT_ERROR',
            'VALIDATION_ERROR', 'SYSTEM_ERROR', 'UNKNOWN_ERROR'
        ]
        
        for error_type in required_types:
            assert hasattr(ErrorType, error_type)

    def test_error_severity_enum(self):
        """测试错误严重性枚举"""
        required_severities = [
            'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
        ]
        
        for severity in required_severities:
            assert hasattr(ErrorSeverity, severity)

    def test_handle_connection_error(self, mock_client):
        """测试处理连接错误"""
        handler = ErrorHandler(mock_client)
        
        error = ConnectionError("Connection lost")
        
        result = handler.handle_error(error, ErrorType.CONNECTION_ERROR)
        
        assert result == True
        assert handler.error_count == 1
        # 验证重连被调用
        mock_client.reconnect.assert_called_once()

    def test_handle_authentication_error(self, mock_client):
        """测试处理认证错误"""
        handler = ErrorHandler(mock_client)
        
        error = PermissionError("Authentication failed")
        
        result = handler.handle_error(error, ErrorType.AUTHENTICATION_ERROR)
        
        assert result == True
        assert handler.error_count == 1
        # 认证错误应该断开连接
        mock_client.disconnect.assert_called_once()

    def test_handle_task_error_recoverable(self, mock_client):
        """测试处理可恢复的任务错误"""
        handler = ErrorHandler(mock_client)
        
        error = RuntimeError("Task execution failed")
        
        result = handler.handle_error(error, ErrorType.TASK_ERROR, recoverable=True)
        
        assert result == True
        assert handler.error_count == 1

    def test_handle_task_error_non_recoverable(self, mock_client):
        """测试处理不可恢复的任务错误"""
        handler = ErrorHandler(mock_client)
        
        error = RuntimeError("Critical task failure")
        
        result = handler.handle_error(error, ErrorType.TASK_ERROR, recoverable=False)
        
        assert result == True
        assert handler.error_count == 1

    def test_error_severity_classification(self, mock_client):
        """测试错误严重性分类"""
        handler = ErrorHandler(mock_client)
        
        # 低严重性错误
        low_error = ValueError("Invalid parameter")
        handler.handle_error(low_error, ErrorType.VALIDATION_ERROR)
        
        # 高严重性错误
        critical_error = SystemError("System failure")
        handler.handle_error(critical_error, ErrorType.SYSTEM_ERROR)
        
        assert len(handler.error_history) == 2
        
        # 验证严重性分类
        low_entry = handler.error_history[0]
        critical_entry = handler.error_history[1]
        
        assert low_entry['severity'] == ErrorSeverity.LOW
        assert critical_entry['severity'] == ErrorSeverity.CRITICAL

    def test_error_retry_mechanism(self, mock_client):
        """测试错误重试机制"""
        handler = ErrorHandler(mock_client)
        handler.max_retry_attempts = 3
        
        # 模拟重试失败然后成功
        mock_client.reconnect.side_effect = [False, False, True]
        
        error = ConnectionError("Connection lost")
        result = handler.handle_error(error, ErrorType.CONNECTION_ERROR)
        
        assert result == True
        assert mock_client.reconnect.call_count == 3

    def test_error_retry_exhausted(self, mock_client):
        """测试重试次数耗尽"""
        handler = ErrorHandler(mock_client)
        handler.max_retry_attempts = 2
        
        # 模拟所有重试都失败
        mock_client.reconnect.return_value = False
        
        error = ConnectionError("Connection lost")
        result = handler.handle_error(error, ErrorType.CONNECTION_ERROR)
        
        assert result == False
        assert mock_client.reconnect.call_count == 2

    def test_error_rate_limiting(self, mock_client):
        """测试错误频率限制"""
        handler = ErrorHandler(mock_client)
        handler.error_rate_limit = 5  # 每分钟最多5个错误
        
        # 快速产生多个错误
        for i in range(10):
            error = RuntimeError(f"Error {i}")
            handler.handle_error(error, ErrorType.TASK_ERROR)
        
        # 验证错误处理被限制
        assert handler.error_count <= handler.error_rate_limit

    def test_error_circuit_breaker(self, mock_client):
        """测试错误熔断器"""
        handler = ErrorHandler(mock_client)
        handler.circuit_breaker_threshold = 3
        
        # 产生足够的错误触发熔断器
        for i in range(5):
            error = ConnectionError(f"Connection error {i}")
            handler.handle_error(error, ErrorType.CONNECTION_ERROR)
        
        # 验证熔断器被触发
        assert handler.is_circuit_breaker_open == True

    def test_error_recovery_strategies(self, mock_client):
        """测试错误恢复策略"""
        handler = ErrorHandler(mock_client)
        
        # 测试不同类型错误的恢复策略
        strategies = {
            ErrorType.CONNECTION_ERROR: 'reconnect',
            ErrorType.RESOURCE_ERROR: 'cleanup',
            ErrorType.TIMEOUT_ERROR: 'retry',
            ErrorType.VALIDATION_ERROR: 'skip'
        }
        
        for error_type, expected_strategy in strategies.items():
            strategy = handler._get_recovery_strategy(error_type)
            assert strategy == expected_strategy

    def test_error_notification(self, mock_client):
        """测试错误通知"""
        handler = ErrorHandler(mock_client)
        
        # 注册错误监听器
        notifications = []
        
        def error_listener(error_info):
            notifications.append(error_info)
        
        handler.register_error_listener(error_listener)
        
        # 产生错误
        error = RuntimeError("Test error")
        handler.handle_error(error, ErrorType.TASK_ERROR)
        
        # 验证通知被发送
        assert len(notifications) == 1
        assert notifications[0]['error_type'] == ErrorType.TASK_ERROR

    def test_error_statistics(self, mock_client):
        """测试错误统计"""
        handler = ErrorHandler(mock_client)
        
        # 产生不同类型的错误
        errors = [
            (ConnectionError("Connection lost"), ErrorType.CONNECTION_ERROR),
            (RuntimeError("Task failed"), ErrorType.TASK_ERROR),
            (TimeoutError("Operation timeout"), ErrorType.TIMEOUT_ERROR),
            (RuntimeError("Another task failed"), ErrorType.TASK_ERROR)
        ]
        
        for error, error_type in errors:
            handler.handle_error(error, error_type)
        
        stats = handler.get_error_statistics()
        
        assert stats['total_errors'] == 4
        assert stats['error_types'][ErrorType.TASK_ERROR] == 2
        assert stats['error_types'][ErrorType.CONNECTION_ERROR] == 1
        assert stats['error_types'][ErrorType.TIMEOUT_ERROR] == 1

    def test_error_history_management(self, mock_client):
        """测试错误历史管理"""
        handler = ErrorHandler(mock_client)
        handler.max_history_size = 5
        
        # 产生超过历史大小限制的错误
        for i in range(10):
            error = RuntimeError(f"Error {i}")
            handler.handle_error(error, ErrorType.TASK_ERROR)
        
        # 验证历史大小被限制
        assert len(handler.error_history) == 5
        
        # 验证保留的是最新的错误
        latest_error = handler.error_history[-1]
        assert "Error 9" in str(latest_error['error'])

    def test_error_context_information(self, mock_client):
        """测试错误上下文信息"""
        handler = ErrorHandler(mock_client)
        
        error = RuntimeError("Test error")
        context = {
            'task_id': 'test-task-001',
            'round_number': 3,
            'operation': 'model_training'
        }
        
        handler.handle_error(error, ErrorType.TASK_ERROR, context=context)
        
        error_entry = handler.error_history[0]
        assert error_entry['context'] == context

    def test_error_logging(self, mock_client):
        """测试错误日志记录"""
        handler = ErrorHandler(mock_client)
        
        with patch('logging.Logger.error') as mock_logger:
            error = RuntimeError("Test error")
            handler.handle_error(error, ErrorType.TASK_ERROR)
            
            # 验证错误被记录到日志
            mock_logger.assert_called()

    def test_error_metrics_collection(self, mock_client):
        """测试错误指标收集"""
        handler = ErrorHandler(mock_client)
        
        # 产生一些错误
        for i in range(3):
            error = RuntimeError(f"Error {i}")
            handler.handle_error(error, ErrorType.TASK_ERROR)
        
        metrics = handler.collect_metrics()
        
        assert metrics is not None
        assert 'error_rate' in metrics
        assert 'mean_time_between_errors' in metrics
        assert 'error_distribution' in metrics

    def test_error_escalation(self, mock_client):
        """测试错误升级"""
        handler = ErrorHandler(mock_client)
        handler.escalation_threshold = 3
        
        escalations = []
        
        def escalation_handler(error_info):
            escalations.append(error_info)
        
        handler.register_escalation_handler(escalation_handler)
        
        # 产生足够的错误触发升级
        for i in range(5):
            error = RuntimeError(f"Error {i}")
            handler.handle_error(error, ErrorType.TASK_ERROR)
        
        # 验证错误被升级
        assert len(escalations) > 0

    def test_error_suppression(self, mock_client):
        """测试错误抑制"""
        handler = ErrorHandler(mock_client)
        
        # 抑制特定类型的错误
        handler.suppress_error_type(ErrorType.VALIDATION_ERROR)
        
        error = ValueError("Validation failed")
        result = handler.handle_error(error, ErrorType.VALIDATION_ERROR)
        
        # 错误被抑制，不应该被处理
        assert result == True
        assert handler.error_count == 0

    def test_error_recovery_timeout(self, mock_client):
        """测试错误恢复超时"""
        handler = ErrorHandler(mock_client)
        handler.recovery_timeout = 0.1  # 100ms超时
        
        # 模拟恢复操作超时
        def slow_reconnect():
            time.sleep(0.2)  # 超过超时时间
            return True
        
        mock_client.reconnect.side_effect = slow_reconnect
        
        error = ConnectionError("Connection lost")
        result = handler.handle_error(error, ErrorType.CONNECTION_ERROR)
        
        # 恢复应该因超时而失败
        assert result == False

    def test_concurrent_error_handling(self, mock_client):
        """测试并发错误处理"""
        handler = ErrorHandler(mock_client)
        
        results = []
        errors = []
        
        def handle_error_worker(error_id):
            try:
                error = RuntimeError(f"Error {error_id}")
                result = handler.handle_error(error, ErrorType.TASK_ERROR)
                results.append(result)
            except Exception as e:
                errors.append(e)
        
        # 创建多个并发错误处理线程
        threads = []
        for i in range(10):
            thread = threading.Thread(target=handle_error_worker, args=(i,))
            threads.append(thread)
            thread.start()
        
        # 等待所有线程完成
        for thread in threads:
            thread.join()
        
        # 验证所有错误都被正确处理
        assert len(errors) == 0
        assert len(results) == 10
        assert all(results)

    def test_error_handler_disable_enable(self, mock_client):
        """测试错误处理器禁用和启用"""
        handler = ErrorHandler(mock_client)
        
        # 禁用错误处理器
        handler.disable()
        assert handler.is_enabled == False
        
        # 尝试处理错误
        error = RuntimeError("Test error")
        result = handler.handle_error(error, ErrorType.TASK_ERROR)
        
        # 错误不应该被处理
        assert result == False
        assert handler.error_count == 0
        
        # 重新启用
        handler.enable()
        assert handler.is_enabled == True
        
        # 现在错误应该被处理
        result = handler.handle_error(error, ErrorType.TASK_ERROR)
        assert result == True
        assert handler.error_count == 1

    def test_error_cleanup(self, mock_client):
        """测试错误清理"""
        handler = ErrorHandler(mock_client)
        
        # 产生一些错误
        for i in range(5):
            error = RuntimeError(f"Error {i}")
            handler.handle_error(error, ErrorType.TASK_ERROR)
        
        assert handler.error_count == 5
        assert len(handler.error_history) == 5
        
        # 清理错误
        handler.cleanup()
        
        assert handler.error_count == 0
        assert len(handler.error_history) == 0

    @pytest.mark.slow
    def test_error_recovery_performance(self, mock_client):
        """测试错误恢复性能"""
        handler = ErrorHandler(mock_client)
        
        # 测试大量错误的处理性能
        start_time = time.time()
        
        for i in range(100):
            error = RuntimeError(f"Error {i}")
            handler.handle_error(error, ErrorType.TASK_ERROR)
        
        end_time = time.time()
        
        # 100个错误应该在合理时间内处理完成
        assert end_time - start_time < 1.0

    def test_error_pattern_detection(self, mock_client):
        """测试错误模式检测"""
        handler = ErrorHandler(mock_client)
        
        # 产生重复的错误模式
        for i in range(5):
            error = ConnectionError("Connection timeout")
            handler.handle_error(error, ErrorType.CONNECTION_ERROR)
        
        patterns = handler.detect_error_patterns()
        
        assert len(patterns) > 0
        # 应该检测到连接超时的模式
        assert any('Connection timeout' in pattern['message'] for pattern in patterns)
