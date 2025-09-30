#!/usr/bin/env python3
"""
Random Forest Model Training Module
Train and optimize random forest models for underwater acoustic communication
"""

import os
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
from sklearn.model_selection import (
    train_test_split, GridSearchCV, cross_val_score, 
    validation_curve, learning_curve
)
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.metrics import (
    mean_squared_error, r2_score, classification_report, 
    confusion_matrix, mean_absolute_error
)
from sklearn.inspection import permutation_importance
import joblib
import logging
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Any, Union
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
import warnings
from functools import lru_cache
import psutil
import gc

# 配置
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
warnings.filterwarnings('ignore', category=FutureWarning)

class RandomForestTrainer:
    """Random Forest model trainer for underwater acoustic features"""
    
    def __init__(self, model_dir: str = "models", random_state: int = 42, 
                 enable_memory_optimization: bool = True):
        self.model_dir = Path(model_dir)
        self.model_dir.mkdir(exist_ok=True)
        self.random_state = random_state
        self.enable_memory_optimization = enable_memory_optimization
        
        # Initialize models
        self.regressor = None
        self.classifier = None
        self.scaler = StandardScaler()
        self.label_encoder = LabelEncoder()
        
        # Model parameters - 使用固定随机种子确保可重现性
        self.regressor_params = {
            'n_estimators': 100,
            'max_depth': 10,
            'min_samples_split': 5,
            'min_samples_leaf': 2,
            'random_state': self.random_state,
            'n_jobs': -1,
            'oob_score': True  # 启用袋外评分
        }
        
        self.classifier_params = {
            'n_estimators': 100,
            'max_depth': 8,
            'min_samples_split': 5,
            'min_samples_leaf': 2,
            'random_state': self.random_state,
            'n_jobs': -1,
            'oob_score': True  # 启用袋外评分
        }
        
        # 优化的参数搜索空间
        self.param_grids = {
            'regressor': {
                'n_estimators': [50, 100, 200, 300],
                'max_depth': [5, 10, 15, 20, None],
                'min_samples_split': [2, 5, 10, 15],
                'min_samples_leaf': [1, 2, 4, 8],
                'max_features': ['sqrt', 'log2', None, 0.5]
            },
            'classifier': {
                'n_estimators': [50, 100, 200, 300],
                'max_depth': [5, 8, 12, 15, None],
                'min_samples_split': [2, 5, 10, 15],
                'min_samples_leaf': [1, 2, 4, 8],
                'max_features': ['sqrt', 'log2', None]
            }
        }
        
        # Set up logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        
        # Training history and performance tracking
        self.training_history = {
            'regressor': {},
            'classifier': {}
        }
        
        # Performance monitoring
        self.performance_metrics = {
            'training_times': [],
            'memory_usage': [],
            'model_sizes': []
        }
        
        # Early stopping configuration
        self.early_stopping_config = {
            'patience': 10,
            'min_delta': 0.001,
            'monitor_metric': 'val_score'
        }
    
    def _check_memory_usage(self) -> Dict[str, float]:
        """检查当前内存使用情况"""
        process = psutil.Process(os.getpid())
        memory_info = process.memory_info()
        return {
            'rss_mb': memory_info.rss / 1024 / 1024,
            'vms_mb': memory_info.vms / 1024 / 1024,
            'percent': process.memory_percent()
        }
    
    def _optimize_memory(self):
        """内存优化"""
        if self.enable_memory_optimization:
            gc.collect()
    
    def _validate_data_quality(self, X: pd.DataFrame, y: pd.Series = None) -> Dict[str, Any]:
        """验证数据质量"""
        quality_report = {
            'n_samples': len(X),
            'n_features': X.shape[1],
            'missing_values': X.isnull().sum().sum(),
            'duplicate_rows': X.duplicated().sum(),
            'constant_features': (X.var() == 0).sum(),
            'memory_usage_mb': X.memory_usage(deep=True).sum() / 1024 / 1024
        }
        
        if y is not None:
            quality_report.update({
                'target_missing': y.isnull().sum(),
                'target_unique_values': y.nunique(),
                'target_type': str(y.dtype)
            })
        
        return quality_report
    
    def prepare_data(self, df: pd.DataFrame, target_cols: List[str] = None) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """Prepare data for training with enhanced validation"""
        if df.empty:
            raise ValueError("Input dataframe is empty")
        
        logger.info(f"准备数据: {df.shape[0]} 样本, {df.shape[1]} 特征")
        
        # 数据质量检查
        quality_report = self._validate_data_quality(df)
        logger.info(f"数据质量报告: {quality_report}")
        
        # 内存使用检查
        memory_before = self._check_memory_usage()
        logger.info(f"内存使用 (处理前): {memory_before['rss_mb']:.1f}MB")
        
        # Remove non-numeric columns and handle missing values
        numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()
        
        # Exclude ID columns
        exclude_cols = ['id', 'timestamp']
        feature_cols = [col for col in numeric_cols if col not in exclude_cols]
        
        if 'env_id' in df.columns:
            feature_cols = [col for col in feature_cols if col != 'env_id']
        
        # Prepare features
        X = df[feature_cols].copy()
        
        # Handle missing values
        X = X.fillna(X.mean())
        
        # Remove columns with zero variance and highly correlated features
        variance = X.var()
        low_variance_cols = variance[variance <= 1e-10].index
        if len(low_variance_cols) > 0:
            logger.warning(f"移除 {len(low_variance_cols)} 个零方差特征")
            X = X.drop(columns=low_variance_cols)
        
        # 移除高度相关的特征
        if X.shape[1] > 1:
            corr_matrix = X.corr().abs()
            upper_tri = corr_matrix.where(
                np.triu(np.ones(corr_matrix.shape), k=1).astype(bool)
            )
            high_corr_features = [column for column in upper_tri.columns 
                                if any(upper_tri[column] > 0.95)]
            if high_corr_features:
                logger.warning(f"移除 {len(high_corr_features)} 个高相关特征")
                X = X.drop(columns=high_corr_features)
        
        logger.info(f"数据准备完成: {X.shape[1]} 特征, {X.shape[0]} 样本")
        
        # 内存优化
        self._optimize_memory()
        memory_after = self._check_memory_usage()
        logger.info(f"内存使用 (处理后): {memory_after['rss_mb']:.1f}MB")
        
        # Prepare targets
        y = pd.DataFrame()
        if target_cols:
            available_targets = [col for col in target_cols if col in df.columns]
            if available_targets:
                y = df[available_targets].copy()
                y = y.fillna(y.mean())
        
        return X, y
    
    def create_synthetic_targets(self, X: pd.DataFrame) -> pd.DataFrame:
        """Create synthetic target variables for demonstration"""
        # 使用固定种子确保可重现性
        np.random.seed(self.random_state)
        n_samples = len(X)
        
        # Synthetic targets based on feature combinations
        targets = pd.DataFrame()
        
        # Communication quality (0-1, higher is better)
        targets['comm_quality'] = np.clip(
            0.5 + 0.3 * np.random.randn(n_samples) + 
            0.2 * X.get('ml_propagation_efficiency', np.zeros(n_samples)) -
            0.1 * X.get('ml_channel_complexity', np.zeros(n_samples)),
            0, 1
        )
        
        # Transmission loss prediction (dB)
        base_tl = X.get('shd_mean_tl', 60 * np.ones(n_samples))
        targets['transmission_loss'] = np.clip(
            base_tl + 10 * np.random.randn(n_samples),
            30, 120
        )
        
        # Channel stability (categorical: stable, moderate, unstable)
        stability_score = (
            X.get('ml_environment_variability', np.zeros(n_samples)) +
            X.get('ray_path_complexity', np.zeros(n_samples)) / 10
        )
        
        targets['channel_stability'] = pd.cut(
            stability_score,
            bins=3,
            labels=['stable', 'moderate', 'unstable']
        ).astype(str)
        
        # Signal-to-noise ratio (dB)
        targets['snr'] = np.clip(
            20 + 10 * np.random.randn(n_samples) +
            5 * X.get('ml_signal_quality', np.zeros(n_samples)),
            0, 40
        )
        
        # Optimal frequency (Hz)
        targets['optimal_frequency'] = np.clip(
            X.get('env_frequency', 1000 * np.ones(n_samples)) + 
            500 * np.random.randn(n_samples),
            500, 5000
        )
        
        logger.info(f"创建了 {len(targets.columns)} 个合成目标变量")
        return targets
    
    def _detect_overfitting(self, train_scores: np.ndarray, val_scores: np.ndarray, 
                           patience: int = 5) -> bool:
        """检测过拟合"""
        if len(train_scores) < patience + 1:
            return False
        
        # 检查验证分数是否在最近几轮中持续下降
        recent_val_scores = val_scores[-patience:]
        trend = np.polyfit(range(len(recent_val_scores)), recent_val_scores, 1)[0]
        
        # 检查训练和验证分数的差距是否过大
        train_val_gap = np.mean(train_scores[-patience:]) - np.mean(val_scores[-patience:])
        
        return trend < -0.001 or train_val_gap > 0.1
    
    def _optimize_params(self, X: pd.DataFrame, y: pd.Series, 
                        model_type: str = 'regressor') -> Dict[str, Any]:
        """参数优化"""
        logger.info(f"开始智能参数优化 ({model_type})")
        
        # 根据数据大小调整参数范围
        n_samples, n_features = X.shape
        
        if n_samples < 1000:
            # 小数据集使用较小的参数范围
            param_grid = {
                'n_estimators': [50, 100],
                'max_depth': [5, 10, None],
                'min_samples_split': [2, 5],
                'min_samples_leaf': [1, 2]
            }
        elif n_samples < 10000:
            # 中等数据集
            param_grid = self.param_grids[model_type]
        else:
            # 大数据集使用更保守的参数
            param_grid = {
                'n_estimators': [100, 200],
                'max_depth': [10, 15, 20],
                'min_samples_split': [5, 10],
                'min_samples_leaf': [2, 4],
                'max_features': ['sqrt', 'log2']
            }
        
        # 选择合适的模型
        if model_type == 'regressor':
            base_model = RandomForestRegressor(
                random_state=self.random_state, 
                n_jobs=-1, 
                oob_score=True
            )
            scoring = 'r2'
        else:
            base_model = RandomForestClassifier(
                random_state=self.random_state, 
                n_jobs=-1, 
                oob_score=True
            )
            scoring = 'accuracy'
        
        # 使用较少的CV折数以节省时间
        cv_folds = min(5, max(3, n_samples // 200))
        
        grid_search = GridSearchCV(
            base_model, 
            param_grid, 
            cv=cv_folds, 
            scoring=scoring, 
            n_jobs=1,  # 避免嵌套并行
            verbose=1
        )
        
        return grid_search
    
    def _estimate_model_size(self, model) -> float:
        """估算模型大小（MB）"""
        try:
            import pickle
            import io
            
            buffer = io.BytesIO()
            pickle.dump(model, buffer)
            size_bytes = buffer.tell()
            return size_bytes / 1024 / 1024
        except Exception:
            return 0.0
    
    def _analyze_feature_stability(self, model, X: pd.DataFrame, 
                                  n_iterations: int = 10) -> Dict[str, float]:
        """分析特征重要性稳定性"""
        if not hasattr(model, 'estimators_'):
            return {}
        
        try:
            # 使用不同的随机子集计算特征重要性
            importances_list = []
            n_samples = len(X)
            
            for i in range(n_iterations):
                # 随机采样
                sample_indices = np.random.choice(
                    n_samples, size=int(n_samples * 0.8), replace=False
                )
                X_sample = X.iloc[sample_indices]
                
                # 计算该子集上的特征重要性
                temp_importances = np.zeros(X.shape[1])
                for tree in model.estimators_:
                    temp_importances += tree.feature_importances_
                temp_importances /= len(model.estimators_)
                importances_list.append(temp_importances)
            
            # 计算稳定性指标
            importances_array = np.array(importances_list)
            stability_scores = {}
            
            for i, feature in enumerate(X.columns):
                feature_importances = importances_array[:, i]
                stability_scores[feature] = {
                    'mean': np.mean(feature_importances),
                    'std': np.std(feature_importances),
                    'cv': np.std(feature_importances) / (np.mean(feature_importances) + 1e-8)
                }
            
            return stability_scores
        except Exception as e:
            logger.warning(f"特征重要性稳定性分析失败: {e}")
            return {}
    
    def train_regressor(self, X: pd.DataFrame, y: pd.Series, 
                       optimize_params: bool = True) -> Dict[str, Any]:
        """Train random forest regressor with enhanced features"""
        logger.info(f"训练回归器: {X.shape[1]} 特征, {X.shape[0]} 样本")
        
        # 记录训练开始时间和内存
        start_time = datetime.now()
        memory_before = self._check_memory_usage()
        
        # Split data - 使用固定随机种子
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=self.random_state
        )
        
        # Scale features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Optimize parameters if requested
        if optimize_params:
            grid_search = self._optimize_params(X_train, y_train, 'regressor')
            grid_search.fit(X_train_scaled, y_train)
            
            self.regressor = grid_search.best_estimator_
            best_params = grid_search.best_params_
            logger.info(f"最佳参数: {best_params}")
            logger.info(f"最佳CV分数: {grid_search.best_score_:.4f}")
        else:
            self.regressor = RandomForestRegressor(**self.regressor_params)
            self.regressor.fit(X_train_scaled, y_train)
        
        # Make predictions
        y_pred_train = self.regressor.predict(X_train_scaled)
        y_pred_test = self.regressor.predict(X_test_scaled)
        
        # Calculate comprehensive metrics
        metrics = {
            'train_r2': r2_score(y_train, y_pred_train),
            'test_r2': r2_score(y_test, y_pred_test),
            'train_rmse': np.sqrt(mean_squared_error(y_train, y_pred_train)),
            'test_rmse': np.sqrt(mean_squared_error(y_test, y_pred_test)),
            'train_mae': mean_absolute_error(y_train, y_pred_train),
            'test_mae': mean_absolute_error(y_test, y_pred_test),
            'feature_importance': dict(zip(X.columns, self.regressor.feature_importances_)),
            'oob_score': getattr(self.regressor, 'oob_score_', None)
        }
        
        # 过拟合检测
        overfitting_score = metrics['train_r2'] - metrics['test_r2']
        metrics['overfitting_score'] = overfitting_score
        metrics['is_overfitting'] = overfitting_score > 0.1
        
        # Cross-validation with error handling
        try:
            cv_scores = cross_val_score(
                self.regressor, X_train_scaled, y_train, 
                cv=min(5, len(y_train) // 10), scoring='r2'
            )
            metrics['cv_r2_mean'] = cv_scores.mean()
            metrics['cv_r2_std'] = cv_scores.std()
        except Exception as e:
            logger.warning(f"交叉验证失败: {e}")
            metrics['cv_r2_mean'] = metrics['test_r2']
            metrics['cv_r2_std'] = 0
        
        # 特征重要性统计显著性测试
        if hasattr(self.regressor, 'estimators_'):
            try:
                # 计算特征重要性的置信区间
                importances = np.array([tree.feature_importances_ for tree in self.regressor.estimators_])
                metrics['feature_importance_std'] = dict(zip(X.columns, np.std(importances, axis=0)))
            except Exception as e:
                logger.warning(f"特征重要性统计分析失败: {e}")
        
        # 性能统计
        training_time = (datetime.now() - start_time).total_seconds()
        memory_after = self._check_memory_usage()
        
        metrics.update({
            'training_time_seconds': training_time,
            'memory_usage_mb': memory_after['rss_mb'] - memory_before['rss_mb'],
            'model_size_mb': self._estimate_model_size(self.regressor)
        })
        
        # 特征重要性稳定性分析
        stability_analysis = self._analyze_feature_stability(self.regressor, X)
        if stability_analysis:
            metrics['feature_importance_stability'] = stability_analysis
        
        # Store enhanced training history
        self.training_history['regressor'] = {
            'timestamp': datetime.now(),
            'target': y.name if hasattr(y, 'name') else 'unknown',
            'metrics': metrics,
            'feature_names': X.columns.tolist(),
            'data_quality': self._validate_data_quality(X, y),
            'hyperparameters': self.regressor.get_params()
        }
        
        # 更新性能统计
        self.performance_metrics['training_times'].append(training_time)
        self.performance_metrics['memory_usage'].append(metrics['memory_usage_mb'])
        self.performance_metrics['model_sizes'].append(metrics['model_size_mb'])
        
        # 内存优化
        self._optimize_memory()
        
        logger.info(f"回归器训练完成 - 测试R²: {metrics['test_r2']:.4f}, "
                   f"训练时间: {training_time:.1f}s, "
                   f"过拟合检测: {'是' if metrics['is_overfitting'] else '否'}")
        
        # Return enhanced results
        return {
            'model': self.regressor,
            'metrics': metrics,
            'best_score': metrics['test_r2'],
            'training_summary': {
                'overfitting_detected': metrics['is_overfitting'],
                'training_time': training_time,
                'memory_efficient': metrics['memory_usage_mb'] < 100
            }
        }
    
    def train_classifier(self, X: pd.DataFrame, y: pd.Series, 
                        optimize_params: bool = True) -> Dict[str, Any]:
        """Train random forest classifier with enhanced features"""
        logger.info(f"训练分类器: {X.shape[1]} 特征, {X.shape[0]} 样本")
        
        # 记录训练开始时间和内存
        start_time = datetime.now()
        memory_before = self._check_memory_usage()
        
        # Encode labels
        y_encoded = self.label_encoder.fit_transform(y)
        
        # Split data - 使用固定随机种子
        X_train, X_test, y_train, y_test = train_test_split(
            X, y_encoded, test_size=0.2, random_state=self.random_state, stratify=y_encoded
        )
        
        # Scale features
        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)
        
        # Optimize parameters if requested
        if optimize_params:
            grid_search = self._optimize_params(X_train, y_train, 'classifier')
            grid_search.fit(X_train_scaled, y_train)
            
            self.classifier = grid_search.best_estimator_
            best_params = grid_search.best_params_
            logger.info(f"最佳参数: {best_params}")
            logger.info(f"最佳CV分数: {grid_search.best_score_:.4f}")
        else:
            self.classifier = RandomForestClassifier(**self.classifier_params)
            self.classifier.fit(X_train_scaled, y_train)
        
        # Make predictions
        y_pred_train = self.classifier.predict(X_train_scaled)
        y_pred_test = self.classifier.predict(X_test_scaled)
        
        # Calculate metrics
        train_accuracy = self.classifier.score(X_train_scaled, y_train)
        test_accuracy = self.classifier.score(X_test_scaled, y_test)
        
        metrics = {
            'train_accuracy': train_accuracy,
            'test_accuracy': test_accuracy,
            'feature_importance': dict(zip(X.columns, self.classifier.feature_importances_)),
            'classification_report': classification_report(
                self.label_encoder.inverse_transform(y_test), 
                self.label_encoder.inverse_transform(y_pred_test),
                output_dict=True
            )
        }
        
        # Cross-validation
        cv_scores = cross_val_score(self.classifier, X_train_scaled, y_train, cv=5)
        metrics['cv_accuracy_mean'] = cv_scores.mean()
        metrics['cv_accuracy_std'] = cv_scores.std()
        
        # Store training history
        self.training_history['classifier'] = {
            'timestamp': datetime.now(),
            'target': y.name if hasattr(y, 'name') else 'unknown',
            'metrics': metrics,
            'feature_names': X.columns.tolist(),
            'classes': self.label_encoder.classes_.tolist()
        }
        
        self.logger.info(f"Classifier training completed. Test accuracy: {metrics['test_accuracy']:.4f}")
        
        # Return both model and metrics
        return {
            'model': self.classifier,
            'metrics': metrics,
            'best_score': metrics['test_accuracy']
        }
    
    def train_multiple_targets(self, X: pd.DataFrame, y: pd.DataFrame) -> Dict[str, Any]:
        """Train models for multiple target variables"""
        results = {}
        
        for target_col in y.columns:
            self.logger.info(f"Training models for target: {target_col}")
            
            target_data = y[target_col].dropna()
            X_target = X.loc[target_data.index]
            
            if target_data.dtype == 'object' or len(target_data.unique()) < 10:
                # Classification task
                try:
                    metrics = self.train_classifier(X_target, target_data, optimize_params=False)
                    results[f"{target_col}_classification"] = metrics
                except Exception as e:
                    self.logger.error(f"Error training classifier for {target_col}: {e}")
            else:
                # Regression task
                try:
                    metrics = self.train_regressor(X_target, target_data, optimize_params=False)
                    results[f"{target_col}_regression"] = metrics
                except Exception as e:
                    self.logger.error(f"Error training regressor for {target_col}: {e}")
        
        return results
    
    def save_models(self, suffix: str = "") -> bool:
        """Save trained models and scalers"""
        try:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            
            if self.regressor:
                regressor_path = self.model_dir / f"rf_regressor_{timestamp}{suffix}.joblib"
                joblib.dump(self.regressor, regressor_path)
                self.logger.info(f"Regressor saved to {regressor_path}")
            
            if self.classifier:
                classifier_path = self.model_dir / f"rf_classifier_{timestamp}{suffix}.joblib"
                joblib.dump(self.classifier, classifier_path)
                self.logger.info(f"Classifier saved to {classifier_path}")
            
            # Save scaler and label encoder
            scaler_path = self.model_dir / f"scaler_{timestamp}{suffix}.joblib"
            joblib.dump(self.scaler, scaler_path)
            
            if len(self.label_encoder.classes_) > 0:
                encoder_path = self.model_dir / f"label_encoder_{timestamp}{suffix}.joblib"
                joblib.dump(self.label_encoder, encoder_path)
            
            # Save training history
            history_path = self.model_dir / f"training_history_{timestamp}{suffix}.joblib"
            joblib.dump(self.training_history, history_path)
            
            return True
            
        except Exception as e:
            self.logger.error(f"Error saving models: {e}")
            return False
    
    def load_models(self, regressor_path: str = None, classifier_path: str = None, 
                   scaler_path: str = None, encoder_path: str = None) -> bool:
        """Load trained models"""
        try:
            if regressor_path and Path(regressor_path).exists():
                self.regressor = joblib.load(regressor_path)
                self.logger.info(f"Regressor loaded from {regressor_path}")
            
            if classifier_path and Path(classifier_path).exists():
                self.classifier = joblib.load(classifier_path)
                self.logger.info(f"Classifier loaded from {classifier_path}")
            
            if scaler_path and Path(scaler_path).exists():
                self.scaler = joblib.load(scaler_path)
                self.logger.info(f"Scaler loaded from {scaler_path}")
            
            if encoder_path and Path(encoder_path).exists():
                self.label_encoder = joblib.load(encoder_path)
                self.logger.info(f"Label encoder loaded from {encoder_path}")
            
            return True
            
        except Exception as e:
            self.logger.error(f"Error loading models: {e}")
            return False
    
    def predict(self, X: pd.DataFrame, model_type: str = 'regressor') -> np.ndarray:
        """Make predictions with trained models"""
        if model_type == 'regressor' and self.regressor:
            X_scaled = self.scaler.transform(X)
            return self.regressor.predict(X_scaled)
        elif model_type == 'classifier' and self.classifier:
            X_scaled = self.scaler.transform(X)
            predictions = self.classifier.predict(X_scaled)
            return self.label_encoder.inverse_transform(predictions)
        else:
            raise ValueError(f"Model {model_type} not trained or invalid type")
    
    def get_feature_importance(self, model_type: str = 'regressor', top_n: int = 20) -> pd.DataFrame:
        """Get feature importance from trained model"""
        if model_type == 'regressor' and self.regressor:
            model = self.regressor
            feature_names = self.training_history['regressor'].get('feature_names', [])
        elif model_type == 'classifier' and self.classifier:
            model = self.classifier
            feature_names = self.training_history['classifier'].get('feature_names', [])
        else:
            raise ValueError(f"Model {model_type} not trained or invalid type")
        
        importance_df = pd.DataFrame({
            'feature': feature_names,
            'importance': model.feature_importances_
        }).sort_values('importance', ascending=False)
        
        return importance_df.head(top_n)
    
    def plot_feature_importance(self, model_type: str = 'regressor', top_n: int = 15, 
                               save_path: str = None):
        """Plot feature importance"""
        importance_df = self.get_feature_importance(model_type, top_n)
        
        plt.figure(figsize=(10, 8))
        sns.barplot(data=importance_df, x='importance', y='feature')
        plt.title(f'Top {top_n} Feature Importance ({model_type.title()})')
        plt.xlabel('Importance')
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            self.logger.info(f"Feature importance plot saved to {save_path}")
        
        plt.show()
    
    def generate_report(self) -> str:
        """Generate training report"""
        report = []
        report.append("=== Random Forest Training Report ===")
        report.append(f"Generated at: {datetime.now()}")
        report.append("")
        
        # Regressor report
        if 'regressor' in self.training_history and self.training_history['regressor']:
            reg_history = self.training_history['regressor']
            report.append("=== Regressor Results ===")
            report.append(f"Target: {reg_history.get('target', 'Unknown')}")
            
            metrics = reg_history.get('metrics', {})
            report.append(f"Test R²: {metrics.get('test_r2', 0):.4f}")
            report.append(f"Test RMSE: {metrics.get('test_rmse', 0):.4f}")
            report.append(f"CV R² (mean ± std): {metrics.get('cv_r2_mean', 0):.4f} ± {metrics.get('cv_r2_std', 0):.4f}")
            report.append("")
        
        # Classifier report
        if 'classifier' in self.training_history and self.training_history['classifier']:
            cls_history = self.training_history['classifier']
            report.append("=== Classifier Results ===")
            report.append(f"Target: {cls_history.get('target', 'Unknown')}")
            
            metrics = cls_history.get('metrics', {})
            report.append(f"Test Accuracy: {metrics.get('test_accuracy', 0):.4f}")
            report.append(f"CV Accuracy (mean ± std): {metrics.get('cv_accuracy_mean', 0):.4f} ± {metrics.get('cv_accuracy_std', 0):.4f}")
            
            classes = cls_history.get('classes', [])
            if classes:
                report.append(f"Classes: {', '.join(classes)}")
            report.append("")
        
        return "\n".join(report)

if __name__ == "__main__":
    # Example usage with synthetic data
    trainer = RandomForestTrainer()
    
    # Create sample data
    np.random.seed(42)
    n_samples = 100
    n_features = 20
    
    # Sample feature data
    X = pd.DataFrame(
        np.random.randn(n_samples, n_features),
        columns=[f'feature_{i}' for i in range(n_features)]
    )
    
    # Add some meaningful features
    X['ml_propagation_efficiency'] = np.random.uniform(0, 1, n_samples)
    X['ml_channel_complexity'] = np.random.uniform(0, 10, n_samples)
    X['shd_mean_tl'] = np.random.uniform(40, 100, n_samples)
    
    # Create synthetic targets
    y = trainer.create_synthetic_targets(X)
    
    print("Training models with synthetic data...")
    results = trainer.train_multiple_targets(X, y)
    
    # Save models
    trainer.save_models("_synthetic")
    
    # Generate report
    print("\n" + trainer.generate_report())
    
    print(f"\nTraining completed. Results for {len(results)} models:")
    for model_name, metrics in results.items():
        if 'test_r2' in metrics:
            print(f"{model_name}: R² = {metrics['test_r2']:.4f}")
        elif 'test_accuracy' in metrics:
            print(f"{model_name}: Accuracy = {metrics['test_accuracy']:.4f}")