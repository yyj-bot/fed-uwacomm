"""
任务执行器实现
职责：专门的任务执行器，支持多种联邦学习算法
"""

import time
import logging
import joblib
import pickle
from typing import Dict, Any, Optional, Union
from pathlib import Path
import numpy as np
import pandas as pd

from .config import MLConfig, MLAlgorithm
# 注意：client.py和coordinator.py已被删除，使用新的WebSocket v1.4架构

# Scikit-learn是必需依赖
from sklearn.base import BaseEstimator
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.svm import SVC, SVR
from sklearn.linear_model import LogisticRegression, LinearRegression
from sklearn.metrics import accuracy_score, mean_squared_error, r2_score


class TaskExecutor:
    """任务执行器 - 支持多种联邦学习算法"""
    
    def __init__(self, task_id: str, algorithm: str, config: Dict[str, Any]):
        """初始化任务执行器
        
        Args:
            task_id: 任务ID
            algorithm: 联邦学习算法
            config: 训练配置
        """
        self.task_id = task_id
        self.algorithm = algorithm
        self.config = config
        
        # 设置日志
        self.logger = logging.getLogger(f"TaskExecutor-{task_id}")
        
        # 核心组件
        self.ml_config = None
        self.model = None
        
        # 数据
        self.training_data = None
        self.training_labels = None
        self.dataset_id = None
        
        # 状态
        self.is_initialized = False
        self.current_round = 0
        self.training_results = []
        
        self.logger.info(f"创建任务执行器: {task_id}, 算法: {algorithm}")
    
    def initialize(self) -> bool:
        """初始化任务执行器"""
        try:
            # 创建ML配置
            self.ml_config = self._create_ml_config()
            
            # 创建模型
            self.model = self._create_model()
            if not self.model:
                raise ValueError("无法创建模型")
            
            # 注意：新架构中不再需要单独的客户端和协调器
            # 训练逻辑直接在TaskExecutor中实现
            
            self.is_initialized = True
            self.logger.info(f"任务执行器 {self.task_id} 初始化成功")
            return True
            
        except Exception as e:
            self.logger.error(f"任务执行器初始化失败: {e}")
            return False
    
    def _create_ml_config(self) -> MLConfig:
        """创建ML配置"""
        try:
            # 从配置中提取参数
            local_epochs = self.config.get("localEpochs", 5)
            learning_rate = self.config.get("learningRate", 0.01)
            batch_size = self.config.get("batchSize", 32)
            
            # 映射算法
            algorithm_map = {
                "FEDERATED_AVERAGING": MLAlgorithm.RANDOM_FOREST,
                "FEDERATED_PROXIMAL": MLAlgorithm.SVM,
                "FEDERATED_NOVA": MLAlgorithm.RANDOM_FOREST,
                "SCAFFOLD": MLAlgorithm.RANDOM_FOREST
            }
            
            ml_algorithm = algorithm_map.get(self.algorithm, MLAlgorithm.RANDOM_FOREST)
            
            config = MLConfig(
                algorithm=ml_algorithm,
                local_epochs=local_epochs,
                learning_rate=learning_rate,
                batch_size=batch_size
            )
            
            return config
            
        except Exception as e:
            self.logger.error(f"创建ML配置失败: {e}")
            raise
    
    def _create_model(self) -> Optional[BaseEstimator]:
        """根据算法创建模型"""
        try:
            model_type = self.config.get("modelType", "classification")
            
            if self.algorithm in ["FEDERATED_AVERAGING", "FEDERATED_NOVA", "SCAFFOLD"]:
                # 使用随机森林
                if model_type == "classification":
                    return RandomForestClassifier(
                        n_estimators=self.config.get("nEstimators", 100),
                        max_depth=self.config.get("maxDepth", 10),
                        random_state=42
                    )
                else:
                    return RandomForestRegressor(
                        n_estimators=self.config.get("nEstimators", 100),
                        max_depth=self.config.get("maxDepth", 10),
                        random_state=42
                    )
                    
            elif self.algorithm == "FEDERATED_PROXIMAL":
                # 使用SVM
                if model_type == "classification":
                    return SVC(
                        C=self.config.get("C", 1.0),
                        kernel=self.config.get("kernel", "rbf"),
                        probability=True,
                        random_state=42
                    )
                else:
                    return SVR(
                        C=self.config.get("C", 1.0),
                        kernel=self.config.get("kernel", "rbf")
                    )
            else:
                # 默认使用逻辑回归/线性回归
                if model_type == "classification":
                    return LogisticRegression(
                        C=self.config.get("C", 1.0),
                        max_iter=self.config.get("maxIter", 1000),
                        random_state=42
                    )
                else:
                    return LinearRegression()
                    
        except Exception as e:
            self.logger.error(f"创建模型失败: {e}")
            return None
    
    def load_global_model(self, model_data: Dict[str, Any]) -> bool:
        """加载全局模型"""
        try:
            if not self.is_initialized:
                raise RuntimeError("执行器未初始化")
            
            # 在新架构中，直接加载模型参数到本地模型
            if "model_params" in model_data:
                # 加载模型参数
                model_params = model_data["model_params"]
                self._apply_model_params(model_params)
            
            self.logger.info(f"任务 {self.task_id} 全局模型加载成功")
            return True
            
        except Exception as e:
            self.logger.error(f"加载全局模型失败: {e}")
            return False
    
    def load_training_data(self, dataset_id: str) -> bool:
        """加载训练数据"""
        try:
            self.dataset_id = dataset_id
            
            # 尝试从实际数据源加载数据
            success = self._load_real_data(dataset_id)
            if not success:
                self.logger.warning(f"无法加载真实数据集 {dataset_id}，使用模拟数据")
                self.training_data, self.training_labels = self._load_mock_data()
            
            # 验证数据
            if not self._validate_training_data():
                raise RuntimeError("训练数据验证失败")
            
            # 在新架构中，数据直接存储在执行器中
            # 数据已经加载到self.training_data和self.training_labels
            
            self.logger.info(f"任务 {self.task_id} 训练数据加载成功: {len(self.training_data)} 样本")
            return True
            
        except Exception as e:
            self.logger.error(f"加载训练数据失败: {e}")
            return False
    
    def _load_real_data(self, dataset_id: str) -> bool:
        """尝试加载真实数据"""
        try:
            # 尝试从SQLite存储加载数据
            from ..storage.sqlite_storage import VMStorage
            
            # 使用默认存储路径
            storage = VMStorage()
            
            # 这里可以扩展为从不同数据源加载
            # 例如：CSV文件、数据库、API等
            
            # 目前返回False，使用模拟数据
            return False
            
        except Exception as e:
            self.logger.debug(f"加载真实数据失败: {e}")
            return False
    
    def _validate_training_data(self) -> bool:
        """验证训练数据"""
        try:
            if self.training_data is None or self.training_labels is None:
                return False
            
            if len(self.training_data) == 0 or len(self.training_labels) == 0:
                return False
            
            if len(self.training_data) != len(self.training_labels):
                self.logger.error("特征数据和标签数据长度不匹配")
                return False
            
            # 检查数据类型
            if not isinstance(self.training_data, pd.DataFrame):
                self.logger.error("训练数据必须是pandas DataFrame")
                return False
            
            if not isinstance(self.training_labels, pd.Series):
                self.logger.error("标签数据必须是pandas Series")
                return False
            
            # 检查是否有缺失值
            if self.training_data.isnull().any().any():
                self.logger.warning("训练数据包含缺失值，将进行处理")
                self.training_data = self.training_data.fillna(self.training_data.mean())
            
            if self.training_labels.isnull().any():
                self.logger.warning("标签数据包含缺失值，将进行处理")
                self.training_labels = self.training_labels.fillna(self.training_labels.mode()[0])
            
            self.logger.info(f"数据验证通过: {len(self.training_data)} 样本, {len(self.training_data.columns)} 特征")
            return True
            
        except Exception as e:
            self.logger.error(f"数据验证失败: {e}")
            return False
    
    def _load_mock_data(self) -> tuple:
        """加载模拟数据"""
        # 生成模拟数据
        np.random.seed(42)
        n_samples = self.config.get("sampleCount", 1000)
        n_features = self.config.get("featureCount", 10)
        
        X = pd.DataFrame(
            np.random.randn(n_samples, n_features),
            columns=[f"feature_{i}" for i in range(n_features)]
        )
        
        # 生成标签
        model_type = self.config.get("modelType", "classification")
        if model_type == "classification":
            y = pd.Series(np.random.randint(0, 2, n_samples))
        else:
            y = pd.Series(np.random.randn(n_samples))
        
        return X, y
    
    def train_round(self, round_config: Dict[str, Any]) -> Dict[str, Any]:
        """执行训练轮次"""
        if not self.is_initialized:
            raise RuntimeError("执行器未初始化")
        
        if not self.model or not self.is_initialized:
            raise RuntimeError("模型或执行器未初始化")
        
        self.current_round += 1
        round_num = round_config.get("roundNumber", self.current_round)
        
        try:
            # 执行本地训练
            training_result = self._execute_local_training(round_config)
            
            # 记录结果
            self.training_results.append(training_result)
            
            self.logger.info(f"任务 {self.task_id} 第 {round_num} 轮训练完成")
            return training_result
            
        except Exception as e:
            self.logger.error(f"训练轮次执行失败: {e}")
            raise
    
    def extract_gradients(self) -> Dict[str, Any]:
        """提取梯度/模型参数"""
        try:
            if not self.model:
                raise RuntimeError("模型未初始化")
            
            # 获取模型参数
            model_params = self._get_model_parameters()
            
            # 计算梯度信息
            gradient_info = {
                "model_type": type(self.model).__name__,
                "algorithm": self.algorithm,
                "parameters": model_params,
                "parameter_count": len(str(model_params)),
                "training_samples": len(self.training_data) if self.training_data is not None else 0,
                "round_number": self.current_round,
                "timestamp": time.time()
            }
            
            # 添加算法特定信息
            if self.algorithm == "FEDERATED_PROXIMAL":
                gradient_info["proximal_term"] = self.ml_config.mu
            
            self.logger.info(f"任务 {self.task_id} 梯度提取完成")
            return gradient_info
            
        except Exception as e:
            self.logger.error(f"提取梯度失败: {e}")
            raise
    
    def update_global_model(self, model_data: Dict[str, Any]) -> bool:
        """更新全局模型"""
        try:
            # 在新架构中，直接更新本地模型
            if "model_params" in model_data:
                model_params = model_data["model_params"]
                success = self._apply_model_params(model_params)
            else:
                success = True  # 如果没有参数，认为更新成功
            
            if success:
                self.logger.info(f"任务 {self.task_id} 全局模型更新成功")
            
            return success
            
        except Exception as e:
            self.logger.error(f"更新全局模型失败: {e}")
            return False
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取执行器统计信息"""
        try:
            # 在新架构中，直接获取模型和训练统计信息
            model_info = {"model_type": type(self.model).__name__} if self.model else {}
            training_stats = {"total_rounds": len(self.training_results)}
            
            return {
                "task_id": self.task_id,
                "algorithm": self.algorithm,
                "is_initialized": self.is_initialized,
                "current_round": self.current_round,
                "total_training_rounds": len(self.training_results),
                "dataset_id": self.dataset_id,
                "training_samples": len(self.training_data) if self.training_data is not None else 0,
                "model_info": model_info,
                "training_stats": training_stats,
                "last_training_results": self.training_results[-3:] if self.training_results else []
            }
            
        except Exception as e:
            self.logger.error(f"获取统计信息失败: {e}")
            return {"error": str(e)}
    
    def cleanup(self):
        """清理执行器资源"""
        self.logger.info(f"清理任务执行器 {self.task_id}")
        
        try:
            # 在新架构中，直接清理模型和数据
            
            # 清理数据
            self.training_data = None
            self.training_labels = None
            self.training_results.clear()
            
            # 重置状态
            self.is_initialized = False
            self.current_round = 0
            
        except Exception as e:
            self.logger.error(f"清理资源失败: {e}")
    
    def _execute_local_training(self, round_config: Dict[str, Any]) -> Dict[str, Any]:
        """执行本地训练"""
        start_time = time.time()
        
        # 训练模型
        self.model.fit(self.training_data, self.training_labels)
        
        # 计算训练指标
        train_pred = self.model.predict(self.training_data)
        
        # 根据模型类型计算指标
        if hasattr(self.model, 'predict_proba'):
            # 分类模型
            accuracy = accuracy_score(self.training_labels, train_pred)
            final_loss = 1.0 - accuracy
            final_metric = accuracy
        else:
            # 回归模型
            mse = mean_squared_error(self.training_labels, train_pred)
            final_loss = mse
            final_metric = 1.0 / (1.0 + mse)  # 简化的评分
        
        training_time = time.time() - start_time
        
        return {
            "algorithm": self.algorithm,
            "round_number": self.current_round,
            "training_time": training_time,
            "samples_count": len(self.training_data),
            "final_loss": final_loss,
            "final_accuracy": final_metric
        }
    
    def _get_model_parameters(self) -> Dict[str, Any]:
        """获取模型参数"""
        if hasattr(self.model, 'get_params'):
            return self.model.get_params()
        else:
            return {}
    
    def _apply_model_params(self, model_params: Dict[str, Any]) -> bool:
        """应用模型参数"""
        try:
            if hasattr(self.model, 'set_params'):
                self.model.set_params(**model_params)
                return True
            return True
        except Exception as e:
            self.logger.error(f"应用模型参数失败: {e}")
            return False
