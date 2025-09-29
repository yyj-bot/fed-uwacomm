"""
任务执行器单元测试
"""

import pytest
import numpy as np
import pandas as pd
from unittest.mock import Mock, patch, MagicMock
from pathlib import Path
import tempfile
import shutil

from feduwacomm.ml.federated.task_executor import TaskExecutor


@pytest.mark.unit
class TestTaskExecutor:
    """任务执行器测试类"""

    @pytest.fixture
    def task_config(self):
        """任务配置fixture"""
        return {
            'localEpochs': 5,
            'learningRate': 0.01,
            'batchSize': 32,
            'modelType': 'RandomForest',
            'modelParameters': {
                'n_estimators': 100,
                'max_depth': 10,
                'random_state': 42
            },
            'datasetId': 'test-dataset-001'
        }

    @pytest.fixture
    def sample_training_data(self):
        """示例训练数据"""
        np.random.seed(42)
        features = np.random.rand(100, 5)
        labels = np.random.randint(0, 2, 100)
        return features, labels

    def test_executor_initialization(self, task_config):
        """测试执行器初始化"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        assert executor.task_id == "test-task-001"
        assert executor.algorithm == "FEDERATED_AVERAGING"
        assert executor.config == task_config
        assert executor.is_initialized == False
        assert executor.current_round == 0

    def test_create_ml_config(self, task_config):
        """测试ML配置创建"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        ml_config = executor._create_ml_config()
        
        assert ml_config is not None
        assert ml_config.local_epochs == 5
        assert ml_config.learning_rate == 0.01
        assert ml_config.batch_size == 32

    def test_create_model_random_forest(self, task_config):
        """测试创建随机森林模型"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        model = executor._create_model()
        
        assert model is not None
        from sklearn.ensemble import RandomForestClassifier
        assert isinstance(model, RandomForestClassifier)
        assert model.n_estimators == 100
        assert model.max_depth == 10

    def test_create_model_logistic_regression(self, task_config):
        """测试创建逻辑回归模型"""
        task_config['modelType'] = 'LogisticRegression'
        task_config['modelParameters'] = {
            'C': 1.0,
            'max_iter': 1000,
            'random_state': 42
        }
        
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        model = executor._create_model()
        
        assert model is not None
        from sklearn.linear_model import LogisticRegression
        assert isinstance(model, LogisticRegression)

    def test_create_model_unsupported(self, task_config):
        """测试创建不支持的模型类型"""
        task_config['modelType'] = 'UnsupportedModel'
        
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        model = executor._create_model()
        
        assert model is None

    @patch('feduwacomm.ml.federated.client.FederatedLearningClient')
    @patch('feduwacomm.ml.federated.coordinator.FederatedLearningCoordinator')
    def test_executor_initialization_success(self, mock_coordinator, mock_client, task_config):
        """测试执行器初始化成功"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 模拟组件创建成功
        mock_client.return_value = Mock()
        mock_coordinator.return_value = Mock()
        
        result = executor.initialize()
        
        assert result == True
        assert executor.is_initialized == True
        assert executor.model is not None
        assert executor.client is not None
        assert executor.coordinator is not None

    def test_executor_initialization_failure(self, task_config):
        """测试执行器初始化失败"""
        # 使用无效配置
        task_config['modelType'] = 'InvalidModel'
        
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        result = executor.initialize()
        
        assert result == False
        assert executor.is_initialized == False

    def test_load_training_data_success(self, task_config, sample_training_data):
        """测试加载训练数据成功"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        features, labels = sample_training_data
        
        # 模拟数据加载
        with patch.object(executor, '_load_dataset_from_source', return_value=(features, labels)):
            result = executor.load_training_data("test-dataset-001")
            
            assert result == True
            assert executor.training_data is not None
            assert executor.training_labels is not None
            assert executor.dataset_id == "test-dataset-001"

    def test_load_training_data_failure(self, task_config):
        """测试加载训练数据失败"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 模拟数据加载失败
        with patch.object(executor, '_load_dataset_from_source', side_effect=Exception("数据加载失败")):
            result = executor.load_training_data("invalid-dataset")
            
            assert result == False
            assert executor.training_data is None

    def test_serialize_model(self, task_config, sample_training_data):
        """测试模型序列化"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化并训练模型
        executor.initialize()
        features, labels = sample_training_data
        executor.model.fit(features, labels)
        
        # 序列化模型
        serialized = executor.serialize_model()
        
        assert serialized is not None
        assert isinstance(serialized, (bytes, str))

    def test_deserialize_model(self, task_config, sample_training_data):
        """测试模型反序列化"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化并训练模型
        executor.initialize()
        features, labels = sample_training_data
        executor.model.fit(features, labels)
        
        # 序列化然后反序列化
        serialized = executor.serialize_model()
        result = executor.deserialize_model(serialized)
        
        assert result == True
        assert executor.model is not None

    def test_compute_gradients(self, task_config, sample_training_data):
        """测试梯度计算"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        # 计算梯度
        gradients = executor.compute_gradients()
        
        assert gradients is not None
        assert isinstance(gradients, dict)

    def test_apply_gradients(self, task_config, sample_training_data):
        """测试应用梯度"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        # 计算梯度
        gradients = executor.compute_gradients()
        
        # 应用梯度
        result = executor.apply_gradients(gradients)
        
        assert result == True

    def test_train_local_model(self, task_config, sample_training_data):
        """测试本地模型训练"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        # 训练模型
        result = executor.train_local_model(epochs=3)
        
        assert result == True

    def test_evaluate_model(self, task_config, sample_training_data):
        """测试模型评估"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化并训练
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        executor.train_local_model(epochs=1)
        
        # 评估模型
        metrics = executor.evaluate_model(features, labels)
        
        assert metrics is not None
        assert isinstance(metrics, dict)
        assert 'accuracy' in metrics or 'mse' in metrics

    def test_get_model_parameters(self, task_config):
        """测试获取模型参数"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        executor.initialize()
        
        params = executor.get_model_parameters()
        
        assert params is not None
        assert isinstance(params, dict)

    def test_set_model_parameters(self, task_config):
        """测试设置模型参数"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        executor.initialize()
        
        # 获取当前参数
        original_params = executor.get_model_parameters()
        
        # 设置新参数
        result = executor.set_model_parameters(original_params)
        
        assert result == True

    def test_compress_gradients(self, task_config, sample_training_data):
        """测试梯度压缩"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        # 计算梯度
        gradients = executor.compute_gradients()
        
        # 压缩梯度
        compressed = executor._compress_gradients(gradients)
        
        assert compressed is not None

    def test_decompress_gradients(self, task_config, sample_training_data):
        """测试梯度解压缩"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        # 计算并压缩梯度
        gradients = executor.compute_gradients()
        compressed = executor._compress_gradients(gradients)
        
        # 解压缩梯度
        decompressed = executor._decompress_gradients(compressed)
        
        assert decompressed is not None
        assert isinstance(decompressed, dict)

    def test_get_statistics(self, task_config):
        """测试获取统计信息"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        executor.initialize()
        
        stats = executor.get_statistics()
        
        assert stats is not None
        assert isinstance(stats, dict)
        assert 'task_id' in stats
        assert 'algorithm' in stats
        assert 'current_round' in stats

    def test_cleanup(self, task_config):
        """测试清理资源"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        executor.initialize()
        
        # 执行清理
        executor.cleanup()
        
        # 验证资源被清理
        assert executor.model is None
        assert executor.training_data is None
        assert executor.training_labels is None

    def test_federated_averaging_algorithm(self, task_config, sample_training_data):
        """测试联邦平均算法"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        # 执行联邦平均
        result = executor._execute_federated_averaging()
        
        assert result == True

    def test_federated_proximal_algorithm(self, task_config, sample_training_data):
        """测试联邦近端算法"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_PROXIMAL",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        # 执行联邦近端
        result = executor._execute_federated_proximal()
        
        assert result == True

    def test_performance_monitoring(self, task_config, sample_training_data):
        """测试性能监控"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        # 开始性能监控
        executor._start_performance_monitoring()
        
        # 执行一些操作
        executor.train_local_model(epochs=1)
        
        # 停止性能监控
        metrics = executor._stop_performance_monitoring()
        
        assert metrics is not None
        assert isinstance(metrics, dict)

    def test_error_handling(self, task_config):
        """测试错误处理"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 测试未初始化时的操作
        result = executor.train_local_model(epochs=1)
        assert result == False
        
        # 测试无效数据的处理
        result = executor.load_training_data(None)
        assert result == False

    @pytest.mark.slow
    def test_concurrent_training(self, task_config, sample_training_data):
        """测试并发训练"""
        import threading
        
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        # 初始化
        executor.initialize()
        features, labels = sample_training_data
        executor.training_data = features
        executor.training_labels = labels
        
        results = []
        
        def train_worker():
            result = executor.train_local_model(epochs=1)
            results.append(result)
        
        # 创建多个训练线程
        threads = []
        for i in range(3):
            thread = threading.Thread(target=train_worker)
            threads.append(thread)
            thread.start()
        
        # 等待所有线程完成
        for thread in threads:
            thread.join()
        
        # 验证结果
        assert len(results) == 3
        # 由于线程安全，可能只有一个成功
        assert any(results)

    def test_memory_usage_optimization(self, task_config):
        """测试内存使用优化"""
        executor = TaskExecutor(
            task_id="test-task-001",
            algorithm="FEDERATED_AVERAGING",
            config=task_config
        )
        
        executor.initialize()
        
        # 获取初始内存使用
        import psutil
        process = psutil.Process()
        initial_memory = process.memory_info().rss
        
        # 执行一些操作
        large_data = np.random.rand(10000, 100)
        executor.training_data = large_data
        
        # 清理数据
        executor._optimize_memory_usage()
        
        # 验证内存被优化
        current_memory = process.memory_info().rss
        # 内存使用应该没有显著增加
        assert current_memory < initial_memory * 2
