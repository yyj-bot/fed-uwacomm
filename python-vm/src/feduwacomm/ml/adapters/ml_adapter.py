"""
ML模块集成适配器
职责：保持与现有ML模块的兼容性，提供接口转换层
"""

import logging
import pandas as pd
import numpy as np
from typing import Dict, Any, Optional, Union, List
from pathlib import Path

# 导入现有ML模块
try:
    from ..feature_extractor import FeatureExtractor
    from ..random_forest_trainer import RandomForestTrainer
    from ..model_evaluator import ModelEvaluator
    HAS_ML_MODULES = True
except ImportError as e:
    logging.warning(f"现有ML模块导入失败: {e}")
    HAS_ML_MODULES = False


class MLAdapter:
    """ML模块集成适配器"""
    
    def __init__(self, task_id: str):
        """初始化ML适配器
        
        Args:
            task_id: 任务ID
        """
        self.task_id = task_id
        self.logger = logging.getLogger(f"MLAdapter-{task_id}")
        
        # 现有模块实例
        self.feature_extractor = None
        self.trainer = None
        self.evaluator = None
        
        # 适配器状态
        self.is_initialized = False
        self.supported_modules = []
        
        self._initialize_modules()
    
    def _initialize_modules(self):
        """初始化现有ML模块"""
        try:
            if not HAS_ML_MODULES:
                self.logger.warning("现有ML模块不可用，使用模拟适配器")
                self._initialize_mock_modules()
                return
            
            # 初始化特征提取器
            try:
                self.feature_extractor = FeatureExtractor()
                self.supported_modules.append("feature_extractor")
                self.logger.info("特征提取器适配器初始化成功")
            except Exception as e:
                self.logger.warning(f"特征提取器适配器初始化失败: {e}")
            
            # 初始化随机森林训练器
            try:
                self.trainer = RandomForestTrainer()
                self.supported_modules.append("random_forest_trainer")
                self.logger.info("随机森林训练器适配器初始化成功")
            except Exception as e:
                self.logger.warning(f"随机森林训练器适配器初始化失败: {e}")
            
            # 初始化模型评估器
            try:
                self.evaluator = ModelEvaluator()
                self.supported_modules.append("model_evaluator")
                self.logger.info("模型评估器适配器初始化成功")
            except Exception as e:
                self.logger.warning(f"模型评估器适配器初始化失败: {e}")
            
            self.is_initialized = True
            self.logger.info(f"ML适配器初始化完成，支持模块: {self.supported_modules}")
            
        except Exception as e:
            self.logger.error(f"ML适配器初始化失败: {e}")
            self._initialize_mock_modules()
    
    def _initialize_mock_modules(self):
        """初始化模拟模块"""
        self.feature_extractor = MockFeatureExtractor()
        self.trainer = MockRandomForestTrainer()
        self.evaluator = MockModelEvaluator()
        self.supported_modules = ["mock_feature_extractor", "mock_trainer", "mock_evaluator"]
        self.is_initialized = True
        self.logger.info("模拟ML适配器初始化完成")
    
    def extract_features(self, data: Union[pd.DataFrame, np.ndarray], 
                        config: Dict[str, Any] = None) -> pd.DataFrame:
        """特征提取适配器
        
        Args:
            data: 输入数据
            config: 特征提取配置
            
        Returns:
            pd.DataFrame: 提取的特征
        """
        try:
            if not self.feature_extractor:
                raise RuntimeError("特征提取器未初始化")
            
            # 数据格式转换
            if isinstance(data, np.ndarray):
                data = pd.DataFrame(data)
            
            # 调用现有特征提取器
            if hasattr(self.feature_extractor, 'extract'):
                features = self.feature_extractor.extract(data, config or {})
            else:
                # 模拟特征提取
                features = self.feature_extractor.extract_features(data)
            
            self.logger.info(f"特征提取完成: {features.shape}")
            return features
            
        except Exception as e:
            self.logger.error(f"特征提取失败: {e}")
            raise
    
    def train_model(self, X: pd.DataFrame, y: pd.Series, 
                   config: Dict[str, Any] = None) -> Dict[str, Any]:
        """模型训练适配器
        
        Args:
            X: 特征数据
            y: 标签数据
            config: 训练配置
            
        Returns:
            dict: 训练结果
        """
        try:
            if not self.trainer:
                raise RuntimeError("训练器未初始化")
            
            # 调用现有训练器
            if hasattr(self.trainer, 'train'):
                result = self.trainer.train(X, y, config or {})
            else:
                # 模拟训练
                result = self.trainer.train_model(X, y)
            
            self.logger.info(f"模型训练完成")
            return result
            
        except Exception as e:
            self.logger.error(f"模型训练失败: {e}")
            raise
    
    def evaluate_model(self, model, X_test: pd.DataFrame, y_test: pd.Series,
                      config: Dict[str, Any] = None) -> Dict[str, Any]:
        """模型评估适配器
        
        Args:
            model: 训练好的模型
            X_test: 测试特征数据
            y_test: 测试标签数据
            config: 评估配置
            
        Returns:
            dict: 评估结果
        """
        try:
            if not self.evaluator:
                raise RuntimeError("评估器未初始化")
            
            # 调用现有评估器
            if hasattr(self.evaluator, 'evaluate'):
                result = self.evaluator.evaluate(model, X_test, y_test, config or {})
            else:
                # 模拟评估
                result = self.evaluator.evaluate_model(model, X_test, y_test)
            
            self.logger.info(f"模型评估完成")
            return result
            
        except Exception as e:
            self.logger.error(f"模型评估失败: {e}")
            raise
    
    def preprocess_data(self, data: Union[pd.DataFrame, np.ndarray],
                       config: Dict[str, Any] = None) -> pd.DataFrame:
        """数据预处理适配器
        
        Args:
            data: 原始数据
            config: 预处理配置
            
        Returns:
            pd.DataFrame: 预处理后的数据
        """
        try:
            # 基本数据预处理
            if isinstance(data, np.ndarray):
                data = pd.DataFrame(data)
            
            # 处理缺失值
            if config and config.get("fill_missing", True):
                data = data.fillna(data.mean())
            
            # 数据标准化
            if config and config.get("normalize", False):
                from sklearn.preprocessing import StandardScaler
                scaler = StandardScaler()
                numeric_columns = data.select_dtypes(include=[np.number]).columns
                data[numeric_columns] = scaler.fit_transform(data[numeric_columns])
            
            self.logger.info(f"数据预处理完成: {data.shape}")
            return data
            
        except Exception as e:
            self.logger.error(f"数据预处理失败: {e}")
            raise
    
    def get_supported_modules(self) -> List[str]:
        """获取支持的模块列表
        
        Returns:
            list: 支持的模块名称列表
        """
        return self.supported_modules.copy()
    
    def get_adapter_status(self) -> Dict[str, Any]:
        """获取适配器状态
        
        Returns:
            dict: 适配器状态信息
        """
        return {
            "task_id": self.task_id,
            "is_initialized": self.is_initialized,
            "has_ml_modules": HAS_ML_MODULES,
            "supported_modules": self.supported_modules,
            "feature_extractor_available": self.feature_extractor is not None,
            "trainer_available": self.trainer is not None,
            "evaluator_available": self.evaluator is not None
        }


# 模拟模块类
class MockFeatureExtractor:
    """模拟特征提取器"""
    
    def extract_features(self, data: pd.DataFrame) -> pd.DataFrame:
        """模拟特征提取"""
        # 简单的特征工程
        features = data.copy()
        
        # 添加一些统计特征
        if len(features.columns) > 1:
            features['mean'] = features.mean(axis=1)
            features['std'] = features.std(axis=1)
            features['sum'] = features.sum(axis=1)
        
        return features


class MockRandomForestTrainer:
    """模拟随机森林训练器"""
    
    def train_model(self, X: pd.DataFrame, y: pd.Series) -> Dict[str, Any]:
        """模拟模型训练"""
        from sklearn.ensemble import RandomForestClassifier
        from sklearn.metrics import accuracy_score
        
        # 创建和训练模型
        model = RandomForestClassifier(n_estimators=10, random_state=42)
        model.fit(X, y)
        
        # 计算训练准确率
        predictions = model.predict(X)
        accuracy = accuracy_score(y, predictions)
        
        return {
            "model": model,
            "training_accuracy": accuracy,
            "feature_importance": dict(zip(X.columns, model.feature_importances_))
        }


class MockModelEvaluator:
    """模拟模型评估器"""
    
    def evaluate_model(self, model, X_test: pd.DataFrame, y_test: pd.Series) -> Dict[str, Any]:
        """模拟模型评估"""
        from sklearn.metrics import accuracy_score, classification_report
        
        # 预测
        predictions = model.predict(X_test)
        
        # 计算指标
        accuracy = accuracy_score(y_test, predictions)
        
        return {
            "test_accuracy": accuracy,
            "predictions": predictions.tolist(),
            "classification_report": classification_report(y_test, predictions, output_dict=True)
        }
