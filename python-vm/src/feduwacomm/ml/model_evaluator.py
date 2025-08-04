#!/usr/bin/env python3
"""
Model Evaluation and Prediction Module
Evaluate trained models and make predictions for underwater acoustic communication
"""

import os
import numpy as np
import pandas as pd
from sklearn.metrics import (
    mean_squared_error, mean_absolute_error, r2_score,
    accuracy_score, precision_recall_fscore_support,
    confusion_matrix, classification_report,
    roc_auc_score, roc_curve
)
from sklearn.model_selection import cross_val_score, learning_curve
import matplotlib.pyplot as plt
import seaborn as sns
import joblib
import logging
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Any, Union
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

class ModelEvaluator:
    """Evaluate and analyze trained models"""
    
    def __init__(self, models_dir: str = "models", results_dir: str = "results"):
        self.models_dir = Path(models_dir)
        self.results_dir = Path(results_dir)
        self.results_dir.mkdir(exist_ok=True)
        
        # Set up logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        
        # Evaluation results storage
        self.evaluation_results = {}
        
    def evaluate_regression_model(self, y_true: np.ndarray, y_pred: np.ndarray, 
                                 model_name: str = "model") -> Dict[str, float]:
        """Evaluate regression model performance"""
        
        metrics = {
            'mse': mean_squared_error(y_true, y_pred),
            'rmse': np.sqrt(mean_squared_error(y_true, y_pred)),
            'mae': mean_absolute_error(y_true, y_pred),
            'r2': r2_score(y_true, y_pred),
            'mape': np.mean(np.abs((y_true - y_pred) / (y_true + 1e-8))) * 100,
            'max_error': np.max(np.abs(y_true - y_pred)),
            'std_error': np.std(y_true - y_pred)
        }
        
        # Calculate additional metrics
        residuals = y_true - y_pred
        metrics['mean_residual'] = np.mean(residuals)
        metrics['residual_std'] = np.std(residuals)
        
        # Explained variance
        metrics['explained_variance'] = 1 - np.var(residuals) / np.var(y_true)
        
        self.logger.info(f"Regression evaluation for {model_name}:")
        self.logger.info(f"  R²: {metrics['r2']:.4f}")
        self.logger.info(f"  RMSE: {metrics['rmse']:.4f}")
        self.logger.info(f"  MAE: {metrics['mae']:.4f}")
        
        return metrics
    
    def evaluate_classification_model(self, y_true: np.ndarray, y_pred: np.ndarray, 
                                    y_prob: Optional[np.ndarray] = None,
                                    model_name: str = "model") -> Dict[str, Any]:
        """Evaluate classification model performance"""
        
        # Basic metrics
        accuracy = accuracy_score(y_true, y_pred)
        precision, recall, f1, support = precision_recall_fscore_support(
            y_true, y_pred, average='weighted'
        )
        
        metrics = {
            'accuracy': accuracy,
            'precision': precision,
            'recall': recall,
            'f1_score': f1,
            'support': support.sum()
        }
        
        # Per-class metrics
        precision_per_class, recall_per_class, f1_per_class, support_per_class = \
            precision_recall_fscore_support(y_true, y_pred, average=None)
        
        unique_labels = np.unique(np.concatenate([y_true, y_pred]))
        
        for i, label in enumerate(unique_labels):
            metrics[f'precision_{label}'] = precision_per_class[i] if i < len(precision_per_class) else 0
            metrics[f'recall_{label}'] = recall_per_class[i] if i < len(recall_per_class) else 0
            metrics[f'f1_{label}'] = f1_per_class[i] if i < len(f1_per_class) else 0
        
        # Confusion matrix
        cm = confusion_matrix(y_true, y_pred)
        metrics['confusion_matrix'] = cm.tolist()
        
        # ROC AUC for binary/multiclass
        if y_prob is not None:
            try:
                if len(unique_labels) == 2:
                    metrics['roc_auc'] = roc_auc_score(y_true, y_prob[:, 1])
                else:
                    metrics['roc_auc'] = roc_auc_score(y_true, y_prob, multi_class='ovr')
            except Exception as e:
                self.logger.warning(f"Could not calculate ROC AUC: {e}")
        
        # Classification report
        metrics['classification_report'] = classification_report(y_true, y_pred, output_dict=True)
        
        self.logger.info(f"Classification evaluation for {model_name}:")
        self.logger.info(f"  Accuracy: {metrics['accuracy']:.4f}")
        self.logger.info(f"  F1-Score: {metrics['f1_score']:.4f}")
        self.logger.info(f"  Precision: {metrics['precision']:.4f}")
        self.logger.info(f"  Recall: {metrics['recall']:.4f}")
        
        return metrics
    
    def cross_validate_model(self, model, X: pd.DataFrame, y: pd.Series, 
                           cv: int = 5, scoring: str = 'auto') -> Dict[str, Any]:
        """Perform cross-validation evaluation"""
        
        # Determine scoring method
        if scoring == 'auto':
            if hasattr(model, 'predict_proba') or 'Classifier' in str(type(model)):
                scoring = 'accuracy'
            else:
                scoring = 'r2'
        
        # Perform cross-validation
        cv_scores = cross_val_score(model, X, y, cv=cv, scoring=scoring)
        
        results = {
            'cv_scores': cv_scores.tolist(),
            'cv_mean': cv_scores.mean(),
            'cv_std': cv_scores.std(),
            'cv_min': cv_scores.min(),
            'cv_max': cv_scores.max(),
            'scoring': scoring
        }
        
        self.logger.info(f"Cross-validation results ({scoring}):")
        self.logger.info(f"  Mean: {results['cv_mean']:.4f} ± {results['cv_std']:.4f}")
        self.logger.info(f"  Range: [{results['cv_min']:.4f}, {results['cv_max']:.4f}]")
        
        return results
    
    def plot_learning_curve(self, model, X: pd.DataFrame, y: pd.Series, 
                           model_name: str = "model", save_path: str = None):
        """Plot learning curve"""
        
        train_sizes, train_scores, val_scores = learning_curve(
            model, X, y, cv=5, n_jobs=-1, 
            train_sizes=np.linspace(0.1, 1.0, 10),
            scoring='r2' if hasattr(model, 'predict') and not hasattr(model, 'predict_proba') else 'accuracy'
        )
        
        train_mean = np.mean(train_scores, axis=1)
        train_std = np.std(train_scores, axis=1)
        val_mean = np.mean(val_scores, axis=1)
        val_std = np.std(val_scores, axis=1)
        
        plt.figure(figsize=(10, 6))
        plt.plot(train_sizes, train_mean, 'o-', color='blue', label='Training Score')
        plt.fill_between(train_sizes, train_mean - train_std, train_mean + train_std, alpha=0.1, color='blue')
        
        plt.plot(train_sizes, val_mean, 'o-', color='red', label='Cross-Validation Score')
        plt.fill_between(train_sizes, val_mean - val_std, val_mean + val_std, alpha=0.1, color='red')
        
        plt.xlabel('Training Set Size')
        plt.ylabel('Score')
        plt.title(f'Learning Curve - {model_name}')
        plt.legend(loc='best')
        plt.grid(True, alpha=0.3)
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            self.logger.info(f"Learning curve saved to {save_path}")
        
        plt.show()
    
    def plot_regression_results(self, y_true: np.ndarray, y_pred: np.ndarray, 
                              model_name: str = "model", save_path: str = None):
        """Plot regression results"""
        
        fig, axes = plt.subplots(2, 2, figsize=(15, 12))
        
        # Actual vs Predicted
        axes[0, 0].scatter(y_true, y_pred, alpha=0.5)
        axes[0, 0].plot([y_true.min(), y_true.max()], [y_true.min(), y_true.max()], 'r--', lw=2)
        axes[0, 0].set_xlabel('Actual Values')
        axes[0, 0].set_ylabel('Predicted Values')
        axes[0, 0].set_title('Actual vs Predicted')
        
        # Residuals
        residuals = y_true - y_pred
        axes[0, 1].scatter(y_pred, residuals, alpha=0.5)
        axes[0, 1].axhline(y=0, color='r', linestyle='--')
        axes[0, 1].set_xlabel('Predicted Values')
        axes[0, 1].set_ylabel('Residuals')
        axes[0, 1].set_title('Residual Plot')
        
        # Residuals histogram
        axes[1, 0].hist(residuals, bins=30, alpha=0.7)
        axes[1, 0].set_xlabel('Residuals')
        axes[1, 0].set_ylabel('Frequency')
        axes[1, 0].set_title('Residuals Distribution')
        
        # Q-Q plot
        from scipy import stats
        stats.probplot(residuals, dist="norm", plot=axes[1, 1])
        axes[1, 1].set_title('Q-Q Plot')
        
        plt.suptitle(f'Regression Analysis - {model_name}', fontsize=16)
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            self.logger.info(f"Regression plot saved to {save_path}")
        
        plt.show()
    
    def plot_classification_results(self, y_true: np.ndarray, y_pred: np.ndarray, 
                                  labels: List[str] = None, model_name: str = "model",
                                  save_path: str = None):
        """Plot classification results"""
        
        # Confusion matrix
        cm = confusion_matrix(y_true, y_pred)
        
        plt.figure(figsize=(12, 5))
        
        # Confusion matrix heatmap
        plt.subplot(1, 2, 1)
        sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', 
                   xticklabels=labels, yticklabels=labels)
        plt.title('Confusion Matrix')
        plt.xlabel('Predicted Label')
        plt.ylabel('True Label')
        
        # Classification metrics bar plot
        plt.subplot(1, 2, 2)
        report = classification_report(y_true, y_pred, output_dict=True)
        
        if 'weighted avg' in report:
            metrics = ['precision', 'recall', 'f1-score']
            values = [report['weighted avg'][metric] for metric in metrics]
            
            bars = plt.bar(metrics, values, color=['skyblue', 'lightgreen', 'coral'])
            plt.ylim(0, 1)
            plt.title('Weighted Average Metrics')
            plt.ylabel('Score')
            
            # Add value labels on bars
            for bar, value in zip(bars, values):
                plt.text(bar.get_x() + bar.get_width()/2, bar.get_height() + 0.01,
                        f'{value:.3f}', ha='center', va='bottom')
        
        plt.suptitle(f'Classification Analysis - {model_name}', fontsize=16)
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            self.logger.info(f"Classification plot saved to {save_path}")
        
        plt.show()
    
    def plot_feature_importance(self, feature_importance: Dict[str, float], 
                              top_n: int = 15, model_name: str = "model",
                              save_path: str = None):
        """Plot feature importance"""
        
        # Sort features by importance
        sorted_features = sorted(feature_importance.items(), key=lambda x: x[1], reverse=True)
        
        if len(sorted_features) > top_n:
            sorted_features = sorted_features[:top_n]
        
        features, importance = zip(*sorted_features)
        
        plt.figure(figsize=(10, 8))
        y_pos = np.arange(len(features))
        
        bars = plt.barh(y_pos, importance, color='skyblue')
        plt.yticks(y_pos, features)
        plt.xlabel('Importance')
        plt.title(f'Top {len(features)} Feature Importance - {model_name}')
        plt.gca().invert_yaxis()
        
        # Add value labels
        for i, (bar, imp) in enumerate(zip(bars, importance)):
            plt.text(bar.get_width() + max(importance) * 0.01, bar.get_y() + bar.get_height()/2,
                    f'{imp:.3f}', ha='left', va='center')
        
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            self.logger.info(f"Feature importance plot saved to {save_path}")
        
        plt.show()
    
    def compare_models(self, model_results: Dict[str, Dict[str, Any]], 
                      metric: str = 'auto') -> pd.DataFrame:
        """Compare multiple models"""
        
        comparison_data = []
        
        for model_name, results in model_results.items():
            row = {'Model': model_name}
            
            # Add relevant metrics
            if 'r2' in results:
                row['R²'] = results['r2']
                row['RMSE'] = results['rmse']
                row['MAE'] = results['mae']
            
            if 'accuracy' in results:
                row['Accuracy'] = results['accuracy']
                row['F1-Score'] = results['f1_score']
                row['Precision'] = results['precision']
                row['Recall'] = results['recall']
            
            # Add cross-validation results if available
            if 'cv_mean' in results:
                row['CV Mean'] = results['cv_mean']
                row['CV Std'] = results['cv_std']
            
            comparison_data.append(row)
        
        comparison_df = pd.DataFrame(comparison_data)
        
        # Sort by main metric
        if metric == 'auto':
            if 'R²' in comparison_df.columns:
                comparison_df = comparison_df.sort_values('R²', ascending=False)
            elif 'Accuracy' in comparison_df.columns:
                comparison_df = comparison_df.sort_values('Accuracy', ascending=False)
        else:
            if metric in comparison_df.columns:
                comparison_df = comparison_df.sort_values(metric, ascending=False)
        
        self.logger.info("Model comparison:")
        print(comparison_df.to_string(index=False))
        
        return comparison_df
    
    def generate_evaluation_report(self, model_results: Dict[str, Any], 
                                 model_name: str = "model") -> str:
        """Generate comprehensive evaluation report"""
        
        report = []
        report.append("=" * 60)
        report.append(f"MODEL EVALUATION REPORT: {model_name.upper()}")
        report.append("=" * 60)
        report.append(f"Generated at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")
        
        # Regression metrics
        if 'r2' in model_results:
            report.append("REGRESSION METRICS:")
            report.append("-" * 30)
            report.append(f"R² Score:           {model_results['r2']:.4f}")
            report.append(f"RMSE:              {model_results['rmse']:.4f}")
            report.append(f"MAE:               {model_results['mae']:.4f}")
            report.append(f"MAPE:              {model_results.get('mape', 0):.2f}%")
            report.append(f"Max Error:         {model_results.get('max_error', 0):.4f}")
            report.append(f"Explained Variance: {model_results.get('explained_variance', 0):.4f}")
            report.append("")
        
        # Classification metrics
        if 'accuracy' in model_results:
            report.append("CLASSIFICATION METRICS:")
            report.append("-" * 30)
            report.append(f"Accuracy:          {model_results['accuracy']:.4f}")
            report.append(f"Precision:         {model_results['precision']:.4f}")
            report.append(f"Recall:            {model_results['recall']:.4f}")
            report.append(f"F1-Score:          {model_results['f1_score']:.4f}")
            
            if 'roc_auc' in model_results:
                report.append(f"ROC AUC:           {model_results['roc_auc']:.4f}")
            report.append("")
        
        # Cross-validation results
        if 'cv_mean' in model_results:
            report.append("CROSS-VALIDATION RESULTS:")
            report.append("-" * 30)
            report.append(f"Mean Score:        {model_results['cv_mean']:.4f}")
            report.append(f"Standard Deviation: {model_results['cv_std']:.4f}")
            report.append(f"Score Range:       [{model_results['cv_min']:.4f}, {model_results['cv_max']:.4f}]")
            report.append(f"Scoring Method:    {model_results.get('scoring', 'unknown')}")
            report.append("")
        
        # Feature importance (top 10)
        if 'feature_importance' in model_results:
            feature_imp = model_results['feature_importance']
            if isinstance(feature_imp, dict):
                sorted_features = sorted(feature_imp.items(), key=lambda x: x[1], reverse=True)[:10]
                
                report.append("TOP 10 FEATURE IMPORTANCE:")
                report.append("-" * 30)
                for i, (feature, importance) in enumerate(sorted_features, 1):
                    report.append(f"{i:2d}. {feature:<25} {importance:.4f}")
                report.append("")
        
        # Model quality assessment
        report.append("MODEL QUALITY ASSESSMENT:")
        report.append("-" * 30)
        
        if 'r2' in model_results:
            r2 = model_results['r2']
            if r2 >= 0.9:
                quality = "Excellent"
            elif r2 >= 0.7:
                quality = "Good"
            elif r2 >= 0.5:
                quality = "Fair"
            else:
                quality = "Poor"
            report.append(f"Regression Quality: {quality} (R² = {r2:.4f})")
        
        if 'accuracy' in model_results:
            acc = model_results['accuracy']
            if acc >= 0.95:
                quality = "Excellent"
            elif acc >= 0.85:
                quality = "Good"
            elif acc >= 0.7:
                quality = "Fair"
            else:
                quality = "Poor"
            report.append(f"Classification Quality: {quality} (Accuracy = {acc:.4f})")
        
        report.append("")
        report.append("=" * 60)
        
        return "\n".join(report)
    
    def save_evaluation_results(self, results: Dict[str, Any], 
                              model_name: str = "model") -> bool:
        """Save evaluation results to file"""
        try:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            
            # Save detailed results as joblib
            results_path = self.results_dir / f"evaluation_{model_name}_{timestamp}.joblib"
            joblib.dump(results, results_path)
            
            # Save report as text
            report = self.generate_evaluation_report(results, model_name)
            report_path = self.results_dir / f"report_{model_name}_{timestamp}.txt"
            
            with open(report_path, 'w', encoding='utf-8') as f:
                f.write(report)
            
            self.logger.info(f"Evaluation results saved to {results_path}")
            self.logger.info(f"Evaluation report saved to {report_path}")
            
            return True
            
        except Exception as e:
            self.logger.error(f"Error saving evaluation results: {e}")
            return False
    
    def evaluate_regressor(self, model, X_test: np.ndarray, y_test: np.ndarray) -> Dict[str, float]:
        """
        评估回归模型性能
        
        参数:
            model: 训练好的回归模型
            X_test: 测试特征
            y_test: 测试标签
            
        返回:
            Dict[str, float]: 评估指标字典
        """
        try:
            # 进行预测
            y_pred = model.predict(X_test)
            
            # 计算评估指标
            metrics = self.evaluate_regression_model(y_test, y_pred)
            
            self.logger.info(f"回归模型评估完成 - R²: {metrics['r2']:.4f}, RMSE: {metrics['rmse']:.4f}")
            
            return metrics
            
        except Exception as e:
            self.logger.error(f"回归模型评估失败: {e}")
            return {}
    
    def evaluate_classifier(self, model, X_test: np.ndarray, y_test: np.ndarray) -> Dict[str, float]:
        """
        评估分类模型性能
        
        参数:
            model: 训练好的分类模型
            X_test: 测试特征
            y_test: 测试标签
            
        返回:
            Dict[str, float]: 评估指标字典
        """
        try:
            # 进行预测
            y_pred = model.predict(X_test)
            
            # 计算评估指标
            metrics = self.evaluate_classification_model(y_test, y_pred)
            
            self.logger.info(f"分类模型评估完成 - 准确率: {metrics['accuracy']:.4f}")
            
            return metrics
            
        except Exception as e:
            self.logger.error(f"分类模型评估失败: {e}")
            return {}

class PredictionEngine:
    """Make predictions with trained models"""
    
    def __init__(self, models_dir: str = "models"):
        self.models_dir = Path(models_dir)
        self.models = {}
        self.scalers = {}
        self.encoders = {}
        
        # Set up logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
    
    def load_model_artifacts(self, model_name: str, 
                           model_path: str = None, 
                           scaler_path: str = None,
                           encoder_path: str = None) -> bool:
        """Load model and associated artifacts"""
        try:
            # Load model
            if model_path and Path(model_path).exists():
                self.models[model_name] = joblib.load(model_path)
                self.logger.info(f"Model {model_name} loaded from {model_path}")
            
            # Load scaler
            if scaler_path and Path(scaler_path).exists():
                self.scalers[model_name] = joblib.load(scaler_path)
                self.logger.info(f"Scaler for {model_name} loaded from {scaler_path}")
            
            # Load encoder
            if encoder_path and Path(encoder_path).exists():
                self.encoders[model_name] = joblib.load(encoder_path)
                self.logger.info(f"Encoder for {model_name} loaded from {encoder_path}")
            
            return model_name in self.models
            
        except Exception as e:
            self.logger.error(f"Error loading model artifacts: {e}")
            return False
    
    def predict(self, X: pd.DataFrame, model_name: str, 
                return_probabilities: bool = False) -> Union[np.ndarray, Tuple[np.ndarray, np.ndarray]]:
        """Make predictions with loaded model"""
        
        if model_name not in self.models:
            raise ValueError(f"Model {model_name} not loaded")
        
        model = self.models[model_name]
        
        # Scale features if scaler is available
        if model_name in self.scalers:
            X_scaled = self.scalers[model_name].transform(X)
        else:
            X_scaled = X.values
        
        # Make predictions
        predictions = model.predict(X_scaled)
        
        # Handle classification models with label encoding
        if model_name in self.encoders:
            predictions = self.encoders[model_name].inverse_transform(predictions)
        
        # Return probabilities for classification if requested
        if return_probabilities and hasattr(model, 'predict_proba'):
            probabilities = model.predict_proba(X_scaled)
            return predictions, probabilities
        
        return predictions
    
    def batch_predict(self, X: pd.DataFrame, model_names: List[str]) -> Dict[str, np.ndarray]:
        """Make predictions with multiple models"""
        results = {}
        
        for model_name in model_names:
            if model_name in self.models:
                try:
                    results[model_name] = self.predict(X, model_name)
                    self.logger.info(f"Predictions made with {model_name}")
                except Exception as e:
                    self.logger.error(f"Error making predictions with {model_name}: {e}")
            else:
                self.logger.warning(f"Model {model_name} not loaded")
        
        return results

if __name__ == "__main__":
    # Example usage
    evaluator = ModelEvaluator()
    
    # Create sample data for demonstration
    np.random.seed(42)
    n_samples = 100
    
    # Regression example
    y_true_reg = np.random.randn(n_samples) * 10 + 50
    y_pred_reg = y_true_reg + np.random.randn(n_samples) * 2
    
    reg_metrics = evaluator.evaluate_regression_model(y_true_reg, y_pred_reg, "Sample Regressor")
    evaluator.plot_regression_results(y_true_reg, y_pred_reg, "Sample Regressor")
    
    # Classification example
    y_true_cls = np.random.choice(['stable', 'moderate', 'unstable'], n_samples)
    y_pred_cls = y_true_cls.copy()
    # Add some errors
    error_indices = np.random.choice(n_samples, size=n_samples//10, replace=False)
    y_pred_cls[error_indices] = np.random.choice(['stable', 'moderate', 'unstable'], len(error_indices))
    
    cls_metrics = evaluator.evaluate_classification_model(y_true_cls, y_pred_cls, model_name="Sample Classifier")
    evaluator.plot_classification_results(y_true_cls, y_pred_cls, 
                                        labels=['stable', 'moderate', 'unstable'],
                                        model_name="Sample Classifier")
    
    # Generate reports
    print("\nRegression Report:")
    print(evaluator.generate_evaluation_report(reg_metrics, "Sample Regressor"))
    
    print("\nClassification Report:")
    print(evaluator.generate_evaluation_report(cls_metrics, "Sample Classifier"))