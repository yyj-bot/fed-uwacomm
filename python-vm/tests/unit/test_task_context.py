"""
任务上下文管理单元测试
"""

import pytest
import time
from unittest.mock import Mock, patch, MagicMock
from enum import Enum

from feduwacomm.ml.websocket.task_context import TaskContext, TaskStatus


@pytest.mark.unit
class TestTaskContext:
    """任务上下文测试类"""

    @pytest.fixture
    def task_config(self):
        """任务配置fixture"""
        return {
            'taskId': 'test-task-001',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 5,
            'localTrainingConfig': {
                'datasetId': 'test-dataset-001',
                'epochs': 3,
                'batchSize': 16,
                'learningRate': 0.01
            },
            'modelConfig': {
                'modelType': 'RandomForest',
                'parameters': {
                    'n_estimators': 100,
                    'max_depth': 10
                }
            }
        }

    def test_task_context_initialization(self, task_config):
        """测试任务上下文初始化"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        assert context.task_id == "test-task-001"
        assert context.config == task_config
        assert context.status == TaskStatus.INITIALIZING
        assert context.current_round == 0
        assert context.is_active == False

    def test_task_status_enum(self):
        """测试任务状态枚举"""
        required_statuses = [
            'INITIALIZING', 'READY', 'TRAINING', 'GRADIENT_READY',
            'WAITING', 'PAUSED', 'ERROR', 'COMPLETED', 'CLEANED'
        ]
        
        for status in required_statuses:
            assert hasattr(TaskStatus, status)

    @patch('feduwacomm.ml.federated.task_executor.TaskExecutor')
    def test_initialize_success(self, mock_executor, task_config):
        """测试初始化成功"""
        # 模拟执行器初始化成功
        mock_executor_instance = Mock()
        mock_executor_instance.initialize.return_value = True
        mock_executor.return_value = mock_executor_instance
        
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        result = context.initialize()
        
        assert result == True
        assert context.status == TaskStatus.READY
        assert context.executor is not None

    @patch('feduwacomm.ml.federated.task_executor.TaskExecutor')
    def test_initialize_failure(self, mock_executor, task_config):
        """测试初始化失败"""
        # 模拟执行器初始化失败
        mock_executor_instance = Mock()
        mock_executor_instance.initialize.return_value = False
        mock_executor.return_value = mock_executor_instance
        
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        result = context.initialize()
        
        assert result == False
        assert context.status == TaskStatus.ERROR

    def test_start_training_success(self, task_config):
        """测试开始训练成功"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 设置为就绪状态
        context.status = TaskStatus.READY
        context.executor = Mock()
        context.executor.train_local_model.return_value = True
        
        result = context.start_training()
        
        assert result == True
        assert context.status == TaskStatus.TRAINING
        assert context.is_active == True

    def test_start_training_invalid_status(self, task_config):
        """测试在无效状态下开始训练"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 保持初始化状态
        assert context.status == TaskStatus.INITIALIZING
        
        result = context.start_training()
        
        assert result == False
        assert context.status == TaskStatus.INITIALIZING

    def test_pause_training(self, task_config):
        """测试暂停训练"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 设置为训练状态
        context.status = TaskStatus.TRAINING
        context.is_active = True
        
        result = context.pause()
        
        assert result == True
        assert context.status == TaskStatus.PAUSED
        assert context.is_active == False

    def test_resume_training(self, task_config):
        """测试恢复训练"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 设置为暂停状态
        context.status = TaskStatus.PAUSED
        context.executor = Mock()
        
        result = context.resume()
        
        assert result == True
        assert context.status == TaskStatus.READY
        assert context.is_active == True

    def test_stop_training(self, task_config):
        """测试停止训练"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 设置为训练状态
        context.status = TaskStatus.TRAINING
        context.is_active = True
        context.executor = Mock()
        
        result = context.stop()
        
        assert result == True
        assert context.status == TaskStatus.COMPLETED
        assert context.is_active == False

    def test_start_round_training(self, task_config):
        """测试开始轮次训练"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 设置为就绪状态
        context.status = TaskStatus.READY
        context.executor = Mock()
        context.executor.train_local_model.return_value = True
        
        round_config = {
            'roundNumber': 1,
            'epochs': 3
        }
        
        result = context.start_round_training(1, round_config)
        
        assert result == True
        assert context.current_round == 1
        assert context.status == TaskStatus.TRAINING

    def test_complete_round_training(self, task_config):
        """测试完成轮次训练"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 设置为训练状态
        context.status = TaskStatus.TRAINING
        context.current_round = 1
        context.executor = Mock()
        context.executor.compute_gradients.return_value = {'gradients': [0.1, 0.2]}
        
        result = context.complete_round_training()
        
        assert result == True
        assert context.status == TaskStatus.GRADIENT_READY

    def test_update_global_model(self, task_config):
        """测试更新全局模型"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        context.executor = Mock()
        context.executor.deserialize_model.return_value = True
        
        global_model_data = {
            'model_type': 'RandomForest',
            'parameters': {'n_estimators': 100},
            'weights': [0.1, 0.2, 0.3]
        }
        
        result = context.update_global_model(global_model_data)
        
        assert result == True
        context.executor.deserialize_model.assert_called_once()

    def test_get_local_gradients(self, task_config):
        """测试获取本地梯度"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        context.executor = Mock()
        expected_gradients = {'gradients': [0.1, 0.2, 0.3]}
        context.executor.compute_gradients.return_value = expected_gradients
        
        gradients = context.get_local_gradients()
        
        assert gradients == expected_gradients

    def test_get_statistics(self, task_config):
        """测试获取统计信息"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        context.status = TaskStatus.TRAINING
        context.current_round = 2
        context.executor = Mock()
        context.executor.get_statistics.return_value = {
            'training_time': 120.5,
            'accuracy': 0.85
        }
        
        stats = context.get_statistics()
        
        assert stats is not None
        assert stats['task_id'] == "test-task-001"
        assert stats['status'] == TaskStatus.TRAINING.value
        assert stats['current_round'] == 2
        assert 'training_time' in stats
        assert 'accuracy' in stats

    def test_cleanup_resources(self, task_config):
        """测试清理资源"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        context.executor = Mock()
        context.status = TaskStatus.TRAINING
        
        context.cleanup()
        
        assert context.status == TaskStatus.CLEANED
        assert context.is_active == False
        context.executor.cleanup.assert_called_once()

    def test_validate_config_valid(self, task_config):
        """测试有效配置验证"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        result = context._validate_config()
        
        assert result == True

    def test_validate_config_invalid(self):
        """测试无效配置验证"""
        invalid_config = {
            'taskId': 'test-task-001'
            # 缺少必需字段
        }
        
        context = TaskContext(
            task_id="test-task-001",
            config=invalid_config,
            client=Mock()
        )
        
        result = context._validate_config()
        
        assert result == False

    def test_handle_error(self, task_config):
        """测试错误处理"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        error = Exception("测试错误")
        
        context._handle_error(error, "TRAINING")
        
        assert context.status == TaskStatus.ERROR
        assert context.is_active == False

    def test_resource_management(self, task_config):
        """测试资源管理"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 分配资源
        context._allocate_resources()
        
        # 检查资源使用
        usage = context._get_resource_usage()
        
        assert usage is not None
        assert isinstance(usage, dict)
        
        # 释放资源
        context._release_resources()

    def test_performance_monitoring(self, task_config):
        """测试性能监控"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 开始监控
        context._start_performance_monitoring()
        
        # 模拟一些操作
        time.sleep(0.1)
        
        # 停止监控
        metrics = context._stop_performance_monitoring()
        
        assert metrics is not None
        assert isinstance(metrics, dict)
        assert 'duration' in metrics

    def test_state_transitions(self, task_config):
        """测试状态转换"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 测试有效的状态转换
        valid_transitions = [
            (TaskStatus.INITIALIZING, TaskStatus.READY),
            (TaskStatus.READY, TaskStatus.TRAINING),
            (TaskStatus.TRAINING, TaskStatus.PAUSED),
            (TaskStatus.PAUSED, TaskStatus.READY),
            (TaskStatus.TRAINING, TaskStatus.COMPLETED)
        ]
        
        for from_status, to_status in valid_transitions:
            context.status = from_status
            result = context._can_transition_to(to_status)
            assert result == True

    def test_invalid_state_transitions(self, task_config):
        """测试无效状态转换"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 测试无效的状态转换
        invalid_transitions = [
            (TaskStatus.COMPLETED, TaskStatus.TRAINING),
            (TaskStatus.ERROR, TaskStatus.READY),
            (TaskStatus.CLEANED, TaskStatus.TRAINING)
        ]
        
        for from_status, to_status in invalid_transitions:
            context.status = from_status
            result = context._can_transition_to(to_status)
            assert result == False

    def test_concurrent_operations(self, task_config):
        """测试并发操作"""
        import threading
        
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        context.executor = Mock()
        context.status = TaskStatus.READY
        
        results = []
        
        def start_training():
            result = context.start_training()
            results.append(('start', result))
        
        def pause_training():
            time.sleep(0.05)  # 稍微延迟
            result = context.pause()
            results.append(('pause', result))
        
        # 创建并发线程
        thread1 = threading.Thread(target=start_training)
        thread2 = threading.Thread(target=pause_training)
        
        thread1.start()
        thread2.start()
        
        thread1.join()
        thread2.join()
        
        # 验证操作结果
        assert len(results) == 2
        # 至少有一个操作成功
        assert any(result[1] for result in results)

    def test_memory_optimization(self, task_config):
        """测试内存优化"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 模拟大量数据
        context.training_history = [{'round': i, 'data': list(range(1000))} for i in range(100)]
        
        # 执行内存优化
        context._optimize_memory()
        
        # 验证内存被优化
        assert len(context.training_history) <= 10  # 应该保留最近的记录

    @pytest.mark.slow
    def test_long_running_task(self, task_config):
        """测试长时间运行的任务"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        context.executor = Mock()
        context.status = TaskStatus.READY
        
        # 模拟长时间训练
        def long_training():
            time.sleep(0.5)
            return True
        
        context.executor.train_local_model.side_effect = long_training
        
        start_time = time.time()
        result = context.start_training()
        end_time = time.time()
        
        assert result == True
        assert end_time - start_time >= 0.5

    def test_task_recovery(self, task_config):
        """测试任务恢复"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        # 模拟任务出错
        context.status = TaskStatus.ERROR
        context.executor = Mock()
        
        # 尝试恢复
        result = context.recover()
        
        assert result == True
        assert context.status == TaskStatus.READY

    def test_checkpoint_management(self, task_config):
        """测试检查点管理"""
        context = TaskContext(
            task_id="test-task-001",
            config=task_config,
            client=Mock()
        )
        
        context.executor = Mock()
        context.current_round = 3
        
        # 保存检查点
        checkpoint_data = context.save_checkpoint()
        
        assert checkpoint_data is not None
        assert checkpoint_data['round'] == 3
        
        # 加载检查点
        result = context.load_checkpoint(checkpoint_data)
        
        assert result == True
        assert context.current_round == 3
