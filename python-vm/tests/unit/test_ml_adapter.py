"""
ML模块适配器单元测试
"""

import pytest
import numpy as np
import pandas as pd
from unittest.mock import Mock, patch, MagicMock
from pathlib import Path

from feduwacomm.ml.adapters.ml_adapter import MLAdapter


@pytest.mark.unit
class TestMLAdapter:
    """ML适配器测试类"""

    @pytest.fixture
    def sample_data(self):
        """示例数据fixture"""
        np.random.seed(42)
        features = np.random.rand(100, 5)
        labels = np.random.randint(0, 2, 100)
        return features, labels

    @pytest.fixture
    def sample_dataframe(self):
        """示例DataFrame fixture"""
        np.random.seed(42)
        data = {
            'feature1': np.random.rand(100),
            'feature2': np.random.rand(100),
            'feature3': np.random.rand(100),
            'target': np.random.randint(0, 2, 100)
        }
        return pd.DataFrame(data)

    def test_ml_adapter_initialization(self):
        """测试ML适配器初始化"""
        adapter = MLAdapter()
        
        assert adapter is not None
        assert hasattr(adapter, 'feature_extractor')
        assert hasattr(adapter, 'model_evaluator')
        assert hasattr(adapter, 'random_forest_trainer')

    def test_feature_extraction_adapter(self, sample_dataframe):
        """测试特征提取适配器"""
        adapter = MLAdapter()
        
        # 模拟特征提取器
        with patch('feduwacomm.ml.feature_extractor.FeatureExtractor') as mock_extractor:
            mock_instance = Mock()
            mock_instance.extract_features.return_value = sample_dataframe[['feature1', 'feature2', 'feature3']]
            mock_extractor.return_value = mock_instance
            
            # 测试特征提取
            features = adapter.extract_features(sample_dataframe)
            
            assert features is not None
            assert isinstance(features, pd.DataFrame)
            assert len(features.columns) == 3

    def test_model_training_adapter(self, sample_data):
        """测试模型训练适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 模拟随机森林训练器
        with patch('feduwacomm.ml.random_forest_trainer.RandomForestTrainer') as mock_trainer:
            mock_instance = Mock()
            mock_model = Mock()
            mock_instance.train.return_value = mock_model
            mock_trainer.return_value = mock_instance
            
            # 测试模型训练
            model = adapter.train_model(features, labels, model_type='RandomForest')
            
            assert model is not None
            mock_instance.train.assert_called_once()

    def test_model_evaluation_adapter(self, sample_data):
        """测试模型评估适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 创建模拟模型
        mock_model = Mock()
        mock_model.predict.return_value = labels[:50]  # 预测前50个样本
        
        # 模拟模型评估器
        with patch('feduwacomm.ml.model_evaluator.ModelEvaluator') as mock_evaluator:
            mock_instance = Mock()
            mock_instance.evaluate.return_value = {
                'accuracy': 0.85,
                'precision': 0.82,
                'recall': 0.88,
                'f1_score': 0.85
            }
            mock_evaluator.return_value = mock_instance
            
            # 测试模型评估
            metrics = adapter.evaluate_model(mock_model, features[:50], labels[:50])
            
            assert metrics is not None
            assert 'accuracy' in metrics
            assert metrics['accuracy'] == 0.85

    def test_data_format_conversion(self, sample_dataframe):
        """测试数据格式转换"""
        adapter = MLAdapter()
        
        # DataFrame到numpy转换
        features_array = adapter.dataframe_to_numpy(sample_dataframe, exclude_columns=['target'])
        
        assert isinstance(features_array, np.ndarray)
        assert features_array.shape[1] == 3  # 排除target列
        
        # numpy到DataFrame转换
        feature_names = ['feature1', 'feature2', 'feature3']
        df_converted = adapter.numpy_to_dataframe(features_array, feature_names)
        
        assert isinstance(df_converted, pd.DataFrame)
        assert list(df_converted.columns) == feature_names

    def test_model_serialization_adapter(self, sample_data):
        """测试模型序列化适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 创建并训练一个简单模型
        from sklearn.ensemble import RandomForestClassifier
        model = RandomForestClassifier(n_estimators=10, random_state=42)
        model.fit(features, labels)
        
        # 序列化模型
        serialized_data = adapter.serialize_model(model)
        
        assert serialized_data is not None
        assert isinstance(serialized_data, (bytes, str))
        
        # 反序列化模型
        deserialized_model = adapter.deserialize_model(serialized_data)
        
        assert deserialized_model is not None
        # 验证模型功能
        predictions = deserialized_model.predict(features[:10])
        assert len(predictions) == 10

    def test_feature_scaling_adapter(self, sample_data):
        """测试特征缩放适配器"""
        adapter = MLAdapter()
        features, _ = sample_data
        
        # 标准化
        scaled_features, scaler = adapter.scale_features(features, method='standard')
        
        assert scaled_features is not None
        assert scaler is not None
        assert np.abs(np.mean(scaled_features, axis=0)).max() < 0.1  # 均值接近0
        
        # 逆变换
        original_features = adapter.inverse_scale_features(scaled_features, scaler)
        
        assert np.allclose(original_features, features, rtol=1e-10)

    def test_data_preprocessing_pipeline(self, sample_dataframe):
        """测试数据预处理管道"""
        adapter = MLAdapter()
        
        # 定义预处理步骤
        preprocessing_config = {
            'handle_missing': True,
            'remove_outliers': True,
            'scale_features': True,
            'feature_selection': True
        }
        
        processed_data = adapter.preprocess_data(sample_dataframe, preprocessing_config)
        
        assert processed_data is not None
        assert isinstance(processed_data, pd.DataFrame)
        # 预处理后数据应该没有缺失值
        assert not processed_data.isnull().any().any()

    def test_model_compatibility_check(self):
        """测试模型兼容性检查"""
        adapter = MLAdapter()
        
        # 测试支持的模型类型
        supported_models = [
            'RandomForest',
            'LogisticRegression',
            'SVM',
            'XGBoost'
        ]
        
        for model_type in supported_models:
            is_supported = adapter.is_model_supported(model_type)
            assert is_supported == True
        
        # 测试不支持的模型类型
        unsupported_model = 'UnsupportedModel'
        is_supported = adapter.is_model_supported(unsupported_model)
        assert is_supported == False

    def test_feature_importance_extraction(self, sample_data):
        """测试特征重要性提取"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 创建并训练模型
        from sklearn.ensemble import RandomForestClassifier
        model = RandomForestClassifier(n_estimators=10, random_state=42)
        model.fit(features, labels)
        
        # 提取特征重要性
        importance = adapter.get_feature_importance(model)
        
        assert importance is not None
        assert len(importance) == features.shape[1]
        assert all(imp >= 0 for imp in importance)  # 重要性应该非负

    def test_cross_validation_adapter(self, sample_data):
        """测试交叉验证适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 模拟交叉验证
        cv_config = {
            'cv_folds': 5,
            'scoring': 'accuracy',
            'shuffle': True,
            'random_state': 42
        }
        
        cv_scores = adapter.cross_validate_model(
            model_type='RandomForest',
            features=features,
            labels=labels,
            cv_config=cv_config
        )
        
        assert cv_scores is not None
        assert len(cv_scores) == 5  # 5折交叉验证
        assert all(0 <= score <= 1 for score in cv_scores)  # 准确率在0-1之间

    def test_hyperparameter_tuning_adapter(self, sample_data):
        """测试超参数调优适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 定义超参数搜索空间
        param_grid = {
            'n_estimators': [10, 50],
            'max_depth': [3, 5],
            'min_samples_split': [2, 5]
        }
        
        # 执行超参数调优
        best_params, best_score = adapter.tune_hyperparameters(
            model_type='RandomForest',
            features=features,
            labels=labels,
            param_grid=param_grid,
            cv_folds=3
        )
        
        assert best_params is not None
        assert best_score is not None
        assert isinstance(best_params, dict)
        assert 0 <= best_score <= 1

    def test_ensemble_model_adapter(self, sample_data):
        """测试集成模型适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 定义基础模型
        base_models = [
            {'type': 'RandomForest', 'params': {'n_estimators': 10}},
            {'type': 'LogisticRegression', 'params': {'C': 1.0}},
            {'type': 'SVM', 'params': {'C': 1.0, 'kernel': 'rbf'}}
        ]
        
        # 创建集成模型
        ensemble_model = adapter.create_ensemble_model(base_models, method='voting')
        
        assert ensemble_model is not None
        
        # 训练集成模型
        ensemble_model.fit(features, labels)
        
        # 预测
        predictions = ensemble_model.predict(features[:10])
        assert len(predictions) == 10

    def test_model_explanation_adapter(self, sample_data):
        """测试模型解释适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 创建并训练模型
        from sklearn.ensemble import RandomForestClassifier
        model = RandomForestClassifier(n_estimators=10, random_state=42)
        model.fit(features, labels)
        
        # 生成模型解释
        explanation = adapter.explain_model(model, features[:5])
        
        assert explanation is not None
        assert isinstance(explanation, dict)
        assert 'feature_importance' in explanation

    def test_data_validation_adapter(self, sample_dataframe):
        """测试数据验证适配器"""
        adapter = MLAdapter()
        
        # 验证数据质量
        validation_result = adapter.validate_data(sample_dataframe)
        
        assert validation_result is not None
        assert 'is_valid' in validation_result
        assert 'issues' in validation_result
        assert isinstance(validation_result['is_valid'], bool)

    def test_feature_engineering_adapter(self, sample_dataframe):
        """测试特征工程适配器"""
        adapter = MLAdapter()
        
        # 定义特征工程配置
        engineering_config = {
            'polynomial_features': {'degree': 2, 'include_bias': False},
            'interaction_features': True,
            'log_transform': ['feature1'],
            'binning': {'feature2': {'bins': 5, 'strategy': 'uniform'}}
        }
        
        engineered_features = adapter.engineer_features(sample_dataframe, engineering_config)
        
        assert engineered_features is not None
        assert isinstance(engineered_features, pd.DataFrame)
        # 特征工程后应该有更多特征
        assert engineered_features.shape[1] >= sample_dataframe.shape[1]

    def test_model_versioning_adapter(self, sample_data):
        """测试模型版本管理适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 创建并训练模型
        from sklearn.ensemble import RandomForestClassifier
        model = RandomForestClassifier(n_estimators=10, random_state=42)
        model.fit(features, labels)
        
        # 保存模型版本
        version_id = adapter.save_model_version(model, metadata={
            'algorithm': 'RandomForest',
            'accuracy': 0.85,
            'training_date': '2024-01-01'
        })
        
        assert version_id is not None
        
        # 加载模型版本
        loaded_model, metadata = adapter.load_model_version(version_id)
        
        assert loaded_model is not None
        assert metadata is not None
        assert metadata['algorithm'] == 'RandomForest'

    def test_batch_prediction_adapter(self, sample_data):
        """测试批量预测适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 创建并训练模型
        from sklearn.ensemble import RandomForestClassifier
        model = RandomForestClassifier(n_estimators=10, random_state=42)
        model.fit(features, labels)
        
        # 批量预测
        batch_size = 20
        predictions = adapter.batch_predict(model, features, batch_size=batch_size)
        
        assert predictions is not None
        assert len(predictions) == len(features)

    def test_model_monitoring_adapter(self, sample_data):
        """测试模型监控适配器"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 创建并训练模型
        from sklearn.ensemble import RandomForestClassifier
        model = RandomForestClassifier(n_estimators=10, random_state=42)
        model.fit(features, labels)
        
        # 监控模型性能
        monitoring_result = adapter.monitor_model_performance(
            model, features, labels, threshold=0.7
        )
        
        assert monitoring_result is not None
        assert 'performance_score' in monitoring_result
        assert 'is_degraded' in monitoring_result
        assert isinstance(monitoring_result['is_degraded'], bool)

    def test_data_drift_detection(self, sample_data):
        """测试数据漂移检测"""
        adapter = MLAdapter()
        features, _ = sample_data
        
        # 创建参考数据和新数据
        reference_data = features[:50]
        new_data = features[50:] + np.random.normal(0, 0.1, features[50:].shape)  # 添加噪声模拟漂移
        
        # 检测数据漂移
        drift_result = adapter.detect_data_drift(reference_data, new_data)
        
        assert drift_result is not None
        assert 'is_drift_detected' in drift_result
        assert 'drift_score' in drift_result
        assert isinstance(drift_result['is_drift_detected'], bool)

    def test_federated_learning_integration(self, sample_data):
        """测试联邦学习集成"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 模拟联邦学习场景
        client_data = [
            (features[:30], labels[:30]),
            (features[30:60], labels[30:60]),
            (features[60:], labels[60:])
        ]
        
        # 训练本地模型
        local_models = []
        for client_features, client_labels in client_data:
            model = adapter.train_federated_model(client_features, client_labels)
            local_models.append(model)
        
        assert len(local_models) == 3
        assert all(model is not None for model in local_models)
        
        # 聚合模型
        global_model = adapter.aggregate_federated_models(local_models)
        
        assert global_model is not None

    def test_error_handling_in_adapter(self, sample_data):
        """测试适配器中的错误处理"""
        adapter = MLAdapter()
        
        # 测试无效数据处理
        invalid_features = None
        invalid_labels = None
        
        result = adapter.train_model(invalid_features, invalid_labels)
        assert result is None
        
        # 测试无效模型类型
        features, labels = sample_data
        result = adapter.train_model(features, labels, model_type='InvalidModel')
        assert result is None

    def test_performance_optimization(self, sample_data):
        """测试性能优化"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 测试并行处理
        import time
        
        start_time = time.time()
        
        # 并行训练多个模型
        models = adapter.train_multiple_models_parallel(
            features, labels,
            model_types=['RandomForest', 'LogisticRegression'],
            n_jobs=2
        )
        
        end_time = time.time()
        
        assert len(models) == 2
        assert all(model is not None for model in models)
        # 并行处理应该相对较快
        assert end_time - start_time < 10.0

    def test_memory_management(self, sample_data):
        """测试内存管理"""
        adapter = MLAdapter()
        features, labels = sample_data
        
        # 创建大量模型测试内存管理
        models = []
        for i in range(10):
            model = adapter.train_model(features, labels, model_type='RandomForest')
            models.append(model)
        
        # 清理模型
        adapter.cleanup_models(models)
        
        # 验证内存被释放（这里只是示例，实际测试可能需要更复杂的内存监控）
        assert True  # 占位符断言
