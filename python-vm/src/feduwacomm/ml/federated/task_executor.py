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
from .client import FederatedLearningClient
from .coordinator import FederatedLearningCoordinator

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
        self.client = None
        self.coordinator = None
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
            
            # 创建联邦学习客户端
            self.client = FederatedLearningClient(
                client_id=f"client-{self.task_id}",
                model=self.model,
                config=self.ml_config
            )
            
            # 创建协调器
            self.coordinator = FederatedLearningCoordinator(
                task_id=self.task_id,
                config=self.ml_config
            )
            
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
            
            # 初始化协调器
            success = self.coordinator.initialize(model_data)
            if not success:
                raise RuntimeError("协调器初始化失败")
            
            self.logger.info(f"任务 {self.task_id} 全局模型加载成功")
            return True
            
        except Exception as e:
            self.logger.error(f"加载全局模型失败: {e}")
            return False
    
    def load_training_data(self, dataset_id: str) -> bool:
        """加载训练数据"""
        try:
            self.dataset_id = dataset_id
            
            # 这里应该从数据管理系统加载数据
            # 暂时使用模拟数据
            self.training_data, self.training_labels = self._load_mock_data()
            
            # 加载到客户端
            success = self.client.load_local_data(self.training_data, self.training_labels)
            if not success:
                raise RuntimeError("客户端数据加载失败")
            
            self.logger.info(f"任务 {self.task_id} 训练数据加载成功: {len(self.training_data)} 样本")
            return True
            
        except Exception as e:
            self.logger.error(f"加载训练数据失败: {e}")
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
        
        if not self.client or not self.coordinator:
            raise RuntimeError("客户端或协调器未初始化")
        
        self.current_round += 1
        round_num = round_config.get("roundNumber", self.current_round)
        
        try:
            # 执行本地训练
            training_result = self.coordinator.execute_local_training(
                round_num=round_num,
                round_config=round_config,
                client=self.client
            )
            
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
            if not self.client:
                raise RuntimeError("客户端未初始化")
            
            # 获取模型参数
            model_params = self.client.model_wrapper.get_parameters()
            
            # 计算梯度信息
            gradient_info = {
                "model_type": self.client.model_wrapper.model_type,
                "algorithm": self.algorithm,
                "parameters": model_params,
                "parameter_count": self.client.model_wrapper.get_parameter_count(),
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
            if not self.coordinator:
                raise RuntimeError("协调器未初始化")
            
            success = self.coordinator.update_global_model(model_data)
            if success:
                self.logger.info(f"任务 {self.task_id} 全局模型更新成功")
            
            return success
            
        except Exception as e:
            self.logger.error(f"更新全局模型失败: {e}")
            return False
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取执行器统计信息"""
        try:
            client_status = self.client.get_training_status() if self.client else {}
            coordinator_stats = self.coordinator.get_training_statistics() if self.coordinator else {}
            
            return {
                "task_id": self.task_id,
                "algorithm": self.algorithm,
                "is_initialized": self.is_initialized,
                "current_round": self.current_round,
                "total_training_rounds": len(self.training_results),
                "dataset_id": self.dataset_id,
                "training_samples": len(self.training_data) if self.training_data is not None else 0,
                "client_status": client_status,
                "coordinator_stats": coordinator_stats,
                "last_training_results": self.training_results[-3:] if self.training_results else []
            }
            
        except Exception as e:
            self.logger.error(f"获取统计信息失败: {e}")
            return {"error": str(e)}
    
    def cleanup(self):
        """清理执行器资源"""
        self.logger.info(f"清理任务执行器 {self.task_id}")
        
        try:
            if self.coordinator:
                self.coordinator.cleanup()
            
            # 清理数据
            self.training_data = None
            self.training_labels = None
            self.training_results.clear()
            
            # 重置状态
            self.is_initialized = False
            self.current_round = 0
            
        except Exception as e:
            self.logger.error(f"清理资源失败: {e}")
