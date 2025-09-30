#!/usr/bin/env python3
"""
增强版模型评估器
符合FedUWAComm项目要求的完整模型评估系统

根据API文档要求，支持以下评估指标：
- 回归任务：R²、RMSE、MAE、MAPE、最大误差等
- 分类任务：准确率、精确率、召回率、F1分数、ROC AUC等
- 联邦学习专用：轮次性能跟踪、聚合效果评估、客户端贡献度分析
"""

import os
import numpy as np
import pandas as pd
from sklearn.metrics import (
    mean_squared_error, mean_absolute_error, r2_score,
    accuracy_score, precision_recall_fscore_support,
    confusion_matrix, classification_report,
    roc_auc_score, roc_curve, auc,
    explained_variance_score, median_absolute_error
)
from sklearn.model_selection import cross_val_score, learning_curve, validation_curve
import matplotlib.pyplot as plt
import seaborn as sns
import joblib
import json
import logging
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Any, Union
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

# 水声通信特定评估指标
from scipy.stats import pearsonr, spearmanr
from scipy import stats

class ModelEvaluator:
    """增强版模型评估器，符合水声联邦学习系统要求"""
    
    def __init__(self, models_dir: str = "models", results_dir: str = "results", 
                 enable_performance_monitoring: bool = True):
        self.models_dir = Path(models_dir)
        self.results_dir = Path(results_dir)
        self.results_dir.mkdir(parents=True, exist_ok=True)
        self.enable_performance_monitoring = enable_performance_monitoring
        
        # 创建子目录
        self.reports_dir = self.results_dir / "reports"
        self.plots_dir = self.results_dir / "plots"
        self.metrics_dir = self.results_dir / "metrics"
        
        for dir_path in [self.reports_dir, self.plots_dir, self.metrics_dir]:
            dir_path.mkdir(exist_ok=True)
        
        # 设置日志
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        
        # 评估结果存储
        self.evaluation_results = {}
        self.round_metrics = {}  # 联邦学习轮次指标
        
        # 性能监控
        self.performance_stats = {
            'evaluation_count': 0,
            'total_evaluation_time': 0,
            'memory_usage_peak': 0
        } if enable_performance_monitoring else None
        
        # 水声通信任务的标准评估指标
        self.underwater_metrics = {
            'signal_quality': ['snr_improvement', 'ber_reduction', 'channel_estimation_accuracy'],
            'communication_performance': ['throughput', 'latency', 'packet_loss_rate'],
            'environmental_adaptation': ['depth_variance_tolerance', 'salinity_adaptation', 'temperature_robustness']
        }
    
    def _validate_input_data(self, y_true: np.ndarray, y_pred: np.ndarray) -> bool:
        """验证输入数据的有效性"""
        if len(y_true) == 0 or len(y_pred) == 0:
            self.logger.error("输入数据为空")
            return False
        
        if len(y_true) != len(y_pred):
            self.logger.error(f"真实值和预测值长度不匹配: {len(y_true)} vs {len(y_pred)}")
            return False
        
        if np.any(np.isnan(y_true)) or np.any(np.isnan(y_pred)):
            self.logger.warning("输入数据包含NaN值")
            return False
        
        if np.any(np.isinf(y_true)) or np.any(np.isinf(y_pred)):
            self.logger.warning("输入数据包含无穷值")
            return False
        
        return True
    
    def _monitor_performance(self, func_name: str):
        """性能监控装饰器"""
        def decorator(func):
            def wrapper(*args, **kwargs):
                if not self.enable_performance_monitoring:
                    return func(*args, **kwargs)
                
                import time
                import psutil
                import os
                
                start_time = time.time()
                start_memory = psutil.Process(os.getpid()).memory_info().rss / 1024 / 1024  # MB
                
                result = func(*args, **kwargs)
                
                end_time = time.time()
                end_memory = psutil.Process(os.getpid()).memory_info().rss / 1024 / 1024  # MB
                
                execution_time = end_time - start_time
                memory_used = end_memory - start_memory
                
                self.performance_stats['evaluation_count'] += 1
                self.performance_stats['total_evaluation_time'] += execution_time
                self.performance_stats['memory_usage_peak'] = max(
                    self.performance_stats['memory_usage_peak'], end_memory
                )
                
                self.logger.info(f"{func_name} 执行时间: {execution_time:.2f}s, 内存使用: {memory_used:.1f}MB")
                
                return result
            return wrapper
        return decorator
    
    def evaluate_regression(self, y_true: np.ndarray, y_pred: np.ndarray, 
                           model_name: str = "model") -> Dict[str, float]:
        """
        回归模型评估，包含水声通信特定指标
        
        参数:
            y_true: 真实值
            y_pred: 预测值
            model_name: 模型名称
            
        返回:
            包含所有评估指标的字典
        """
        
        # 输入数据验证
        if not self._validate_input_data(y_true, y_pred):
            return {'error': '输入数据验证失败'}
        
        # 应用性能监控
        if self.enable_performance_monitoring:
            return self._monitor_performance('回归评估')(self._evaluate_regression_core)(y_true, y_pred, model_name)
        else:
            return self._evaluate_regression_core(y_true, y_pred, model_name)
    
    def _evaluate_regression_core(self, y_true: np.ndarray, y_pred: np.ndarray, 
                                 model_name: str = "model") -> Dict[str, float]:
        """回归评估的核心逻辑"""
        
        # 基础回归指标
        metrics = {
            'r2': r2_score(y_true, y_pred),
            'mse': mean_squared_error(y_true, y_pred),
            'rmse': np.sqrt(mean_squared_error(y_true, y_pred)),
            'mae': mean_absolute_error(y_true, y_pred),
            'median_ae': median_absolute_error(y_true, y_pred),
            'explained_variance': explained_variance_score(y_true, y_pred),
        }
        
        # 改进的MAPE计算，更好的数值稳定性
        mape_mask = np.abs(y_true) > 1e-8
        if np.any(mape_mask) and np.sum(mape_mask) > len(y_true) * 0.1:  # 至少10%的数据有效
            mape_values = np.abs((y_true[mape_mask] - y_pred[mape_mask]) / y_true[mape_mask])
            # 移除异常值
            mape_values = mape_values[mape_values < 10]  # 移除超过1000%的异常值
            metrics['mape'] = np.mean(mape_values) * 100 if len(mape_values) > 0 else float('inf')
        else:
            metrics['mape'] = float('inf')
        
        # 残差分析
        residuals = y_true - y_pred
        metrics.update({
            'max_error': np.max(np.abs(residuals)),
            'mean_residual': np.mean(residuals),
            'std_residual': np.std(residuals),
            'residual_skewness': stats.skew(residuals),
            'residual_kurtosis': stats.kurtosis(residuals)
        })
        
        # 相关性分析（增强的错误处理）
        if len(y_true) > 2:  # 至少需要3个数据点
            try:
                # 检查数据变异性
                if np.std(y_true) > 1e-10 and np.std(y_pred) > 1e-10:
                    pearson_corr, pearson_p = pearsonr(y_true, y_pred)
                    spearman_corr, spearman_p = spearmanr(y_true, y_pred)
                    
                    metrics.update({
                        'pearson_correlation': pearson_corr if not np.isnan(pearson_corr) else 0,
                        'pearson_p_value': pearson_p if not np.isnan(pearson_p) else 1,
                        'spearman_correlation': spearman_corr if not np.isnan(spearman_corr) else 0,
                        'spearman_p_value': spearman_p if not np.isnan(spearman_p) else 1
                    })
                else:
                    # 数据无变异性
                    metrics.update({
                        'pearson_correlation': 0,
                        'pearson_p_value': 1,
                        'spearman_correlation': 0,
                        'spearman_p_value': 1
                    })
            except Exception as e:
                self.logger.warning(f"相关性计算失败: {e}")
                metrics.update({
                    'pearson_correlation': 0,
                    'pearson_p_value': 1,
                    'spearman_correlation': 0,
                    'spearman_p_value': 1
                })
        
        # 预测区间分析
        metrics.update({
            'prediction_std': np.std(y_pred),
            'prediction_range': np.max(y_pred) - np.min(y_pred),
            'true_range': np.max(y_true) - np.min(y_true),
            'range_ratio': (np.max(y_pred) - np.min(y_pred)) / (np.max(y_true) - np.min(y_true) + 1e-8)
        })
        
        # 水声通信特定指标（基于预测误差的信号质量评估）
        if 'transmission_loss' in model_name.lower() or 'signal' in model_name.lower():
            metrics.update(self._calculate_acoustic_metrics(y_true, y_pred))
        
        self.logger.info(f"回归评估完成 - {model_name}")
        self.logger.info(f"  R²: {metrics['r2']:.4f}, RMSE: {metrics['rmse']:.4f}, MAE: {metrics['mae']:.4f}")
        
        return metrics
    
    def evaluate_classification(self, y_true: np.ndarray, y_pred: np.ndarray,
                               y_prob: Optional[np.ndarray] = None,
                               class_names: Optional[List[str]] = None,
                               model_name: str = "model") -> Dict[str, Any]:
        """
        分类模型评估
        
        参数:
            y_true: 真实标签
            y_pred: 预测标签
            y_prob: 预测概率（可选）
            class_names: 类别名称
            model_name: 模型名称
            
        返回:
            包含所有评估指标的字典
        """
        
        # 输入数据验证
        if not self._validate_input_data(y_true, y_pred):
            return {'error': '输入数据验证失败'}
        
        # 应用性能监控
        if self.enable_performance_monitoring:
            return self._monitor_performance('分类评估')(self._evaluate_classification_core)(
                y_true, y_pred, y_prob, class_names, model_name)
        else:
            return self._evaluate_classification_core(y_true, y_pred, y_prob, class_names, model_name)
    
    def _evaluate_classification_core(self, y_true: np.ndarray, y_pred: np.ndarray,
                                    y_prob: Optional[np.ndarray] = None,
                                    class_names: Optional[List[str]] = None,
                                    model_name: str = "model") -> Dict[str, Any]:
        """分类评估的核心逻辑"""
        
        unique_labels = np.unique(np.concatenate([y_true, y_pred]))
        n_classes = len(unique_labels)
        
        # 基础分类指标
        metrics = {
            'accuracy': accuracy_score(y_true, y_pred),
            'n_classes': n_classes,
            'n_samples': len(y_true)
        }
        
        # 每类别和加权平均指标
        for average in ['macro', 'weighted', 'micro']:
            precision, recall, f1, support = precision_recall_fscore_support(
                y_true, y_pred, average=average, zero_division=0
            )
            metrics.update({
                f'precision_{average}': precision,
                f'recall_{average}': recall,
                f'f1_{average}': f1
            })
        
        # 每个类别的详细指标
        precision_per_class, recall_per_class, f1_per_class, support_per_class = \
            precision_recall_fscore_support(y_true, y_pred, average=None, zero_division=0)
        
        class_metrics = {}
        for i, label in enumerate(unique_labels):
            class_name = class_names[i] if class_names and i < len(class_names) else f'class_{label}'
            class_metrics[class_name] = {
                'precision': precision_per_class[i] if i < len(precision_per_class) else 0,
                'recall': recall_per_class[i] if i < len(recall_per_class) else 0,
                'f1': f1_per_class[i] if i < len(f1_per_class) else 0,
                'support': support_per_class[i] if i < len(support_per_class) else 0
            }
        
        metrics['per_class_metrics'] = class_metrics
        
        # 混淆矩阵
        cm = confusion_matrix(y_true, y_pred)
        metrics['confusion_matrix'] = cm.tolist()
        
        # 计算混淆矩阵衍生指标
        if n_classes == 2:
            tn, fp, fn, tp = cm.ravel() if cm.size == 4 else (0, 0, 0, 0)
            metrics.update({
                'sensitivity': tp / (tp + fn) if (tp + fn) > 0 else 0,  # 召回率
                'specificity': tn / (tn + fp) if (tn + fp) > 0 else 0,
                'positive_predictive_value': tp / (tp + fp) if (tp + fp) > 0 else 0,  # 精确率
                'negative_predictive_value': tn / (tn + fn) if (tn + fn) > 0 else 0,
                'false_positive_rate': fp / (fp + tn) if (fp + tn) > 0 else 0,
                'false_negative_rate': fn / (fn + tp) if (fn + tp) > 0 else 0
            })
        
        # ROC AUC 计算
        if y_prob is not None:
            try:
                if n_classes == 2:
                    if y_prob.ndim == 2 and y_prob.shape[1] == 2:
                        metrics['roc_auc'] = roc_auc_score(y_true, y_prob[:, 1])
                    else:
                        metrics['roc_auc'] = roc_auc_score(y_true, y_prob)
                    
                    # ROC曲线数据
                    fpr, tpr, thresholds = roc_curve(y_true, y_prob[:, 1] if y_prob.ndim == 2 else y_prob)
                    metrics['roc_curve'] = {
                        'fpr': fpr.tolist(),
                        'tpr': tpr.tolist(),
                        'thresholds': thresholds.tolist()
                    }
                else:
                    # 多分类ROC AUC
                    metrics['roc_auc_ovr'] = roc_auc_score(y_true, y_prob, multi_class='ovr')
                    metrics['roc_auc_ovo'] = roc_auc_score(y_true, y_prob, multi_class='ovo')
            except Exception as e:
                self.logger.warning(f"ROC AUC计算失败: {e}")
        
        # 分类报告
        metrics['classification_report'] = classification_report(y_true, y_pred, 
                                                               target_names=class_names,
                                                               output_dict=True, 
                                                               zero_division=0)
        
        # 预测置信度分析
        if y_prob is not None:
            metrics.update(self._analyze_prediction_confidence(y_true, y_pred, y_prob))
        
        self.logger.info(f"分类评估完成 - {model_name}")
        self.logger.info(f"  准确率: {metrics['accuracy']:.4f}, F1: {metrics['f1_weighted']:.4f}")
        
        return metrics
    
    def _calculate_acoustic_metrics(self, y_true: np.ndarray, y_pred: np.ndarray) -> Dict[str, float]:
        """计算水声通信特定指标"""
        
        # 信号质量指标
        signal_error = np.abs(y_true - y_pred)
        relative_error = signal_error / (np.abs(y_true) + 1e-8)
        
        metrics = {
            'signal_fidelity': 1 - np.mean(relative_error),  # 信号保真度
            'peak_signal_accuracy': 1 - np.max(relative_error),  # 峰值信号准确度
            'transmission_efficiency': np.mean(np.exp(-signal_error)),  # 传输效率
        }
        
        # 信道建模准确度（基于传播损失预测）
        if np.std(y_true) > 0:
            metrics['channel_modeling_accuracy'] = 1 - np.std(signal_error) / np.std(y_true)
        else:
            metrics['channel_modeling_accuracy'] = 1.0
        
        # 环境适应性指标
        error_stability = np.std(signal_error) / (np.mean(signal_error) + 1e-8)
        metrics['environmental_robustness'] = 1 / (1 + error_stability)
        
        return metrics
    
    def _analyze_prediction_confidence(self, y_true: np.ndarray, y_pred: np.ndarray, 
                                     y_prob: np.ndarray) -> Dict[str, float]:
        """分析预测置信度"""
        
        # 预测概率分析
        max_probs = np.max(y_prob, axis=1)
        correct_mask = (y_true == y_pred)
        
        metrics = {
            'avg_confidence': np.mean(max_probs),
            'correct_avg_confidence': np.mean(max_probs[correct_mask]) if np.any(correct_mask) else 0,
            'incorrect_avg_confidence': np.mean(max_probs[~correct_mask]) if np.any(~correct_mask) else 0,
            'confidence_std': np.std(max_probs),
        }
        
        # 置信度校准分析
        confidence_bins = np.linspace(0, 1, 11)
        bin_accuracies = []
        bin_confidences = []
        
        for i in range(len(confidence_bins) - 1):
            mask = (max_probs >= confidence_bins[i]) & (max_probs < confidence_bins[i + 1])
            if np.any(mask):
                bin_accuracy = np.mean(correct_mask[mask])
                bin_confidence = np.mean(max_probs[mask])
                bin_accuracies.append(bin_accuracy)
                bin_confidences.append(bin_confidence)
        
        if bin_accuracies:
            metrics['calibration_error'] = np.mean(np.abs(np.array(bin_accuracies) - np.array(bin_confidences)))
        else:
            metrics['calibration_error'] = 0
        
        return metrics
    
    def evaluate_federated_round(self, round_num: int, client_results: List[Dict[str, Any]], 
                                global_metrics: Dict[str, float]) -> Dict[str, Any]:
        """
        评估联邦学习轮次性能
        
        参数:
            round_num: 轮次编号
            client_results: 客户端结果列表
            global_metrics: 全局模型指标
            
        返回:
            轮次评估结果
        """
        
        round_metrics = {
            'round_number': round_num,
            'timestamp': datetime.now().isoformat(),
            'global_metrics': global_metrics,
            'client_count': len(client_results),
        }
        
        # 客户端性能统计
        if client_results:
            client_losses = [result.get('training_loss', 0) for result in client_results]
            client_accuracies = [result.get('training_accuracy', 0) for result in client_results]
            client_samples = [result.get('num_samples', 0) for result in client_results]
            
            round_metrics.update({
                'client_metrics': {
                    'avg_loss': np.mean(client_losses),
                    'std_loss': np.std(client_losses),
                    'min_loss': np.min(client_losses),
                    'max_loss': np.max(client_losses),
                    'avg_accuracy': np.mean(client_accuracies),
                    'std_accuracy': np.std(client_accuracies),
                    'total_samples': sum(client_samples),
                    'avg_samples_per_client': np.mean(client_samples)
                }
            })
            
            # 客户端贡献度分析
            total_samples = sum(client_samples)
            client_contributions = [samples / total_samples for samples in client_samples] if total_samples > 0 else []
            
            if client_contributions:
                round_metrics['client_contribution'] = {
                    'contribution_weights': client_contributions,
                    'contribution_entropy': -sum(p * np.log(p + 1e-8) for p in client_contributions),
                    'max_contribution': max(client_contributions),
                    'min_contribution': min(client_contributions)
                }
        
        # 收敛性分析
        if round_num in self.round_metrics:
            prev_metrics = self.round_metrics[round_num - 1]['global_metrics']
            current_metrics = global_metrics
            
            # 计算改进情况
            improvements = {}
            for metric, value in current_metrics.items():
                if metric in prev_metrics:
                    if metric in ['loss', 'mse', 'rmse', 'mae']:  # 越小越好的指标
                        improvement = prev_metrics[metric] - value
                    else:  # 越大越好的指标
                        improvement = value - prev_metrics[metric]
                    improvements[f'{metric}_improvement'] = improvement
            
            round_metrics['improvements'] = improvements
        
        # 存储轮次指标
        self.round_metrics[round_num] = round_metrics
        
        self.logger.info(f"联邦学习第 {round_num} 轮评估完成")
        
        return round_metrics
    
    def analyze_federated_convergence(self) -> Dict[str, Any]:
        """分析联邦学习收敛性"""
        
        if not self.round_metrics:
            return {'error': '没有轮次数据可供分析'}
        
        rounds = sorted(self.round_metrics.keys())
        
        # 提取关键指标序列
        metrics_series = {}
        for round_num in rounds:
            round_data = self.round_metrics[round_num]
            for metric, value in round_data['global_metrics'].items():
                if metric not in metrics_series:
                    metrics_series[metric] = []
                metrics_series[metric].append(value)
        
        convergence_analysis = {
            'total_rounds': len(rounds),
            'metrics_trends': {},
            'convergence_assessment': {}
        }
        
        # 分析每个指标的趋势
        for metric, values in metrics_series.items():
            if len(values) > 1:
                # 趋势分析
                x = np.array(range(len(values)))
                y = np.array(values)
                
                # 线性回归拟合趋势
                slope, intercept, r_value, p_value, std_err = stats.linregress(x, y)
                
                # 检测收敛
                recent_values = values[-min(5, len(values)):]  # 最近5轮
                is_stabilizing = np.std(recent_values) < 0.01 * np.mean(recent_values) if recent_values else False
                
                convergence_analysis['metrics_trends'][metric] = {
                    'slope': slope,
                    'correlation': r_value,
                    'p_value': p_value,
                    'is_stabilizing': is_stabilizing,
                    'recent_std': np.std(recent_values),
                    'overall_improvement': values[-1] - values[0] if metric not in ['loss', 'mse', 'rmse'] else values[0] - values[-1]
                }
        
        # 总体收敛评估
        stabilizing_metrics = sum(1 for trend in convergence_analysis['metrics_trends'].values() 
                                if trend['is_stabilizing'])
        total_metrics = len(convergence_analysis['metrics_trends'])
        
        convergence_analysis['convergence_assessment'] = {
            'stabilizing_ratio': stabilizing_metrics / total_metrics if total_metrics > 0 else 0,
            'is_converged': stabilizing_metrics >= total_metrics * 0.8,  # 80%的指标稳定
            'recommended_action': self._get_convergence_recommendation(convergence_analysis['metrics_trends'])
        }
        
        return convergence_analysis
    
    def _get_convergence_recommendation(self, trends: Dict[str, Dict]) -> str:
        """根据收敛分析给出建议"""
        
        stabilizing_count = sum(1 for trend in trends.values() if trend['is_stabilizing'])
        improving_count = sum(1 for trend in trends.values() if trend['overall_improvement'] > 0)
        
        total_count = len(trends)
        
        if stabilizing_count >= total_count * 0.8:
            return "模型已收敛，建议停止训练"
        elif improving_count >= total_count * 0.6:
            return "模型正在改善，建议继续训练"
        else:
            return "模型性能不稳定，建议调整超参数"
    
    def generate_report(self, model_results: Dict[str, Any], 
                       model_name: str = "model",
                       include_federated: bool = False) -> str:
        """生成评估报告"""
        
        report = []
        report.append("=" * 80)
        report.append(f"水声联邦学习系统 - 模型评估报告")
        report.append(f"模型名称: {model_name}")
        report.append("=" * 80)
        report.append(f"生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        report.append("")
        
        # 基础模型性能
        if 'r2' in model_results:
            report.append("🔍 回归模型性能评估")
            report.append("-" * 40)
            report.append(f"决定系数 (R²):        {model_results['r2']:.4f}")
            report.append(f"均方根误差 (RMSE):    {model_results['rmse']:.4f}")
            report.append(f"平均绝对误差 (MAE):   {model_results['mae']:.4f}")
            report.append(f"平均绝对百分比误差:   {model_results.get('mape', 0):.2f}%")
            report.append(f"解释方差:            {model_results.get('explained_variance', 0):.4f}")
            report.append("")
            
            # 水声通信特定指标
            if 'signal_fidelity' in model_results:
                report.append("🌊 水声通信性能指标")
                report.append("-" * 40)
                report.append(f"信号保真度:          {model_results['signal_fidelity']:.4f}")
                report.append(f"信道建模准确度:      {model_results.get('channel_modeling_accuracy', 0):.4f}")
                report.append(f"环境鲁棒性:          {model_results.get('environmental_robustness', 0):.4f}")
                report.append("")
        
        if 'accuracy' in model_results:
            report.append("🎯 分类模型性能评估")
            report.append("-" * 40)
            report.append(f"准确率:              {model_results['accuracy']:.4f}")
            report.append(f"精确率 (加权):       {model_results.get('precision_weighted', 0):.4f}")
            report.append(f"召回率 (加权):       {model_results.get('recall_weighted', 0):.4f}")
            report.append(f"F1分数 (加权):       {model_results.get('f1_weighted', 0):.4f}")
            
            if 'roc_auc' in model_results:
                report.append(f"ROC AUC:            {model_results['roc_auc']:.4f}")
            report.append("")
        
        # 联邦学习性能
        if include_federated and self.round_metrics:
            report.append("🤝 联邦学习性能分析")
            report.append("-" * 40)
            
            convergence = self.analyze_federated_convergence()
            report.append(f"训练轮次:            {convergence['total_rounds']}")
            report.append(f"收敛状态:            {'已收敛' if convergence['convergence_assessment']['is_converged'] else '未收敛'}")
            report.append(f"稳定指标比例:        {convergence['convergence_assessment']['stabilizing_ratio']:.2%}")
            report.append(f"建议:               {convergence['convergence_assessment']['recommended_action']}")
            report.append("")
            
            # 最新轮次客户端统计
            latest_round = max(self.round_metrics.keys())
            latest_metrics = self.round_metrics[latest_round]
            if 'client_metrics' in latest_metrics:
                cm = latest_metrics['client_metrics']
                report.append("👥 客户端性能统计 (最新轮次)")
                report.append("-" * 40)
                report.append(f"参与客户端数:        {latest_metrics['client_count']}")
                report.append(f"平均训练损失:        {cm['avg_loss']:.4f} ± {cm['std_loss']:.4f}")
                report.append(f"平均训练准确率:      {cm['avg_accuracy']:.4f} ± {cm['std_accuracy']:.4f}")
                report.append(f"总训练样本数:        {cm['total_samples']}")
                report.append("")
        
        # 模型质量评级
        report.append("⭐ 模型质量评级")
        report.append("-" * 40)
        
        if 'r2' in model_results:
            r2 = model_results['r2']
            if r2 >= 0.95:
                grade = "A+ (优秀)"
            elif r2 >= 0.85:
                grade = "A  (良好)"
            elif r2 >= 0.70:
                grade = "B  (一般)"
            elif r2 >= 0.50:
                grade = "C  (较差)"
            else:
                grade = "D  (很差)"
            report.append(f"回归性能等级:        {grade}")
        
        if 'accuracy' in model_results:
            acc = model_results['accuracy']
            if acc >= 0.95:
                grade = "A+ (优秀)"
            elif acc >= 0.85:
                grade = "A  (良好)"
            elif acc >= 0.75:
                grade = "B  (一般)"
            elif acc >= 0.60:
                grade = "C  (较差)"
            else:
                grade = "D  (很差)"
            report.append(f"分类性能等级:        {grade}")
        
        # 建议和总结
        report.append("")
        report.append("📋 改进建议")
        report.append("-" * 40)
        suggestions = self._generate_improvement_suggestions(model_results)
        for suggestion in suggestions:
            report.append(f"• {suggestion}")
        
        report.append("")
        report.append("=" * 80)
        
        return "\n".join(report)
    
    def _generate_improvement_suggestions(self, results: Dict[str, Any]) -> List[str]:
        """生成改进建议"""
        suggestions = []
        
        # 回归模型建议
        if 'r2' in results:
            r2 = results['r2']
            rmse = results.get('rmse', 0)
            
            if r2 < 0.7:
                suggestions.append("R²较低，建议尝试更复杂的模型或增加特征工程")
            if rmse > np.mean([results.get('true_range', 1)]) * 0.1:
                suggestions.append("RMSE较高，建议检查异常值或调整模型参数")
                
            if 'residual_skewness' in results and abs(results['residual_skewness']) > 1:
                suggestions.append("残差分布偏斜，建议对目标变量进行变换")
        
        # 分类模型建议
        if 'accuracy' in results:
            acc = results['accuracy']
            if acc < 0.8:
                suggestions.append("准确率有待提升，建议增加训练数据或调整模型复杂度")
                
            if 'calibration_error' in results and results['calibration_error'] > 0.1:
                suggestions.append("模型校准性较差，建议使用概率校准技术")
        
        # 联邦学习建议
        if self.round_metrics:
            convergence = self.analyze_federated_convergence()
            if not convergence['convergence_assessment']['is_converged']:
                suggestions.append("联邦学习未收敛，建议调整学习率或增加通信轮次")
        
        # 通用建议
        if not suggestions:
            suggestions.append("模型性能良好，可考虑在生产环境中部署")
        
        return suggestions
    
    def generate_intelligent_insights(self, results: Dict[str, Any]) -> List[str]:
        """基于评估结果生成智能洞察"""
        insights = []
        
        # 回归模型洞察
        if 'r2' in results:
            r2 = results['r2']
            rmse = results.get('rmse', 0)
            
            if r2 > 0.95:
                insights.append("🎯 模型表现优秀，R²超过0.95，预测精度很高")
            elif r2 > 0.8:
                insights.append("✅ 模型表现良好，但仍有改进空间")
            elif r2 < 0.5:
                insights.append("⚠️ 模型表现较差，建议重新考虑特征工程或模型选择")
            
            # 残差分析洞察
            if 'residual_skewness' in results:
                skewness = abs(results['residual_skewness'])
                if skewness > 1:
                    insights.append("📊 残差分布存在偏斜，可能需要数据变换")
                elif skewness < 0.5:
                    insights.append("📊 残差分布接近正态，模型假设较好满足")
        
        # 分类模型洞察
        if 'accuracy' in results:
            acc = results['accuracy']
            n_classes = results.get('n_classes', 2)
            
            if acc > 0.9:
                insights.append("🎯 分类准确率优秀，模型性能很好")
            elif acc < 1.0 / n_classes * 1.2:  # 仅比随机猜测好20%
                insights.append("⚠️ 分类准确率接近随机水平，模型可能存在问题")
            
            # 类别平衡性分析
            if 'per_class_metrics' in results:
                class_f1s = [metrics['f1'] for metrics in results['per_class_metrics'].values()]
                f1_std = np.std(class_f1s)
                if f1_std > 0.2:
                    insights.append("⚖️ 各类别性能差异较大，可能存在类别不平衡问题")
        
        # 水声通信特定洞察
        if 'signal_fidelity' in results:
            fidelity = results['signal_fidelity']
            if fidelity > 0.9:
                insights.append("🌊 信号保真度优秀，适合水声通信应用")
            elif fidelity < 0.7:
                insights.append("🌊 信号保真度较低，可能影响水声通信质量")
        
        # 联邦学习洞察
        if self.round_metrics:
            convergence = self.analyze_federated_convergence()
            if convergence['convergence_assessment']['is_converged']:
                insights.append("🤝 联邦学习已收敛，训练效果稳定")
            else:
                insights.append("🤝 联邦学习尚未收敛，建议继续训练或调整参数")
        
        return insights
    
    def get_performance_summary(self) -> Dict[str, Any]:
        """获取性能监控摘要"""
        if not self.enable_performance_monitoring or not self.performance_stats:
            return {'message': '性能监控未启用'}
        
        stats = self.performance_stats
        return {
            'total_evaluations': stats['evaluation_count'],
            'total_time': stats['total_evaluation_time'],
            'average_time_per_evaluation': stats['total_evaluation_time'] / max(stats['evaluation_count'], 1),
            'peak_memory_usage_mb': stats['memory_usage_peak']
        }
    
    def save_evaluation_artifacts(self, results: Dict[str, Any], 
                                model_name: str = "model",
                                include_plots: bool = True) -> Dict[str, str]:
        """保存评估产物"""
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        saved_files = {}
        
        try:
            # 保存评估指标
            metrics_file = self.metrics_dir / f"metrics_{model_name}_{timestamp}.json"
            with open(metrics_file, 'w', encoding='utf-8') as f:
                json.dump(results, f, indent=2, ensure_ascii=False, default=str)
            saved_files['metrics'] = str(metrics_file)
            
            # 保存详细报告
            report = self.generate_report(results, model_name, include_federated=True)
            report_file = self.reports_dir / f"report_{model_name}_{timestamp}.txt"
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write(report)
            saved_files['report'] = str(report_file)
            
            # 保存联邦学习轮次数据
            if self.round_metrics:
                rounds_file = self.metrics_dir / f"federated_rounds_{model_name}_{timestamp}.json"
                with open(rounds_file, 'w', encoding='utf-8') as f:
                    json.dump(self.round_metrics, f, indent=2, ensure_ascii=False, default=str)
                saved_files['rounds'] = str(rounds_file)
            
            self.logger.info(f"评估产物已保存: {len(saved_files)} 个文件")
            
            return saved_files
            
        except Exception as e:
            self.logger.error(f"保存评估产物失败: {e}")
            return {}
    
    def compare_models(self, model_results: Dict[str, Dict[str, Any]]) -> pd.DataFrame:
        """模型比较"""
        
        comparison_data = []
        
        for model_name, results in model_results.items():
            row = {'模型名称': model_name}
            
            # 回归指标
            if 'r2' in results:
                row.update({
                    'R²': results['r2'],
                    'RMSE': results['rmse'],
                    'MAE': results['mae'],
                    'MAPE(%)': results.get('mape', 0)
                })
            
            # 分类指标
            if 'accuracy' in results:
                row.update({
                    '准确率': results['accuracy'],
                    'F1分数': results.get('f1_weighted', 0),
                    '精确率': results.get('precision_weighted', 0),
                    '召回率': results.get('recall_weighted', 0)
                })
            
            # 水声通信指标
            if 'signal_fidelity' in results:
                row.update({
                    '信号保真度': results['signal_fidelity'],
                    '环境鲁棒性': results.get('environmental_robustness', 0)
                })
            
            comparison_data.append(row)
        
        df = pd.DataFrame(comparison_data)
        
        # 根据主要指标排序
        if 'R²' in df.columns:
            df = df.sort_values('R²', ascending=False)
        elif '准确率' in df.columns:
            df = df.sort_values('准确率', ascending=False)
        
        return df
    
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
            metrics = self.evaluate_regression(y_test, y_pred)
            
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
            metrics = self.evaluate_classification(y_test, y_pred)
            
            self.logger.info(f"分类模型评估完成 - 准确率: {metrics['accuracy']:.4f}")
            
            return metrics
            
        except Exception as e:
            self.logger.error(f"分类模型评估失败: {e}")
            return {}


class PredictionEngine:
    """使用训练好的模型进行预测"""
    
    def __init__(self, models_dir: str = "models"):
        self.models_dir = Path(models_dir)
        self.models = {}
        self.scalers = {}
        self.encoders = {}
        
        # 设置日志
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
    
    def load_model_artifacts(self, model_name: str, 
                           model_path: str = None, 
                           scaler_path: str = None,
                           encoder_path: str = None) -> bool:
        """加载模型和相关文件"""
        try:
            # 加载模型
            if model_path and Path(model_path).exists():
                self.models[model_name] = joblib.load(model_path)
                self.logger.info(f"模型 {model_name} 已从 {model_path} 加载")
            
            # 加载缩放器
            if scaler_path and Path(scaler_path).exists():
                self.scalers[model_name] = joblib.load(scaler_path)
                self.logger.info(f"{model_name} 的缩放器已从 {scaler_path} 加载")
            
            # 加载编码器
            if encoder_path and Path(encoder_path).exists():
                self.encoders[model_name] = joblib.load(encoder_path)
                self.logger.info(f"{model_name} 的编码器已从 {encoder_path} 加载")
            
            return model_name in self.models
            
        except Exception as e:
            self.logger.error(f"加载模型文件失败: {e}")
            return False
    
    def predict(self, X: pd.DataFrame, model_name: str, 
                return_probabilities: bool = False) -> Union[np.ndarray, Tuple[np.ndarray, np.ndarray]]:
        """使用加载的模型进行预测"""
        
        if model_name not in self.models:
            raise ValueError(f"模型 {model_name} 未加载")
        
        model = self.models[model_name]
        
        # 如果有缩放器，进行特征缩放
        if model_name in self.scalers:
            X_scaled = self.scalers[model_name].transform(X)
        else:
            X_scaled = X.values
        
        # 进行预测
        predictions = model.predict(X_scaled)
        
        # 处理带有标签编码的分类模型
        if model_name in self.encoders:
            predictions = self.encoders[model_name].inverse_transform(predictions)
        
        # 如果需要，返回分类概率
        if return_probabilities and hasattr(model, 'predict_proba'):
            probabilities = model.predict_proba(X_scaled)
            return predictions, probabilities
        
        return predictions


def create_evaluation_pipeline():
    """创建完整的评估流水线"""
    
    evaluator = ModelEvaluator()
    
    def evaluate_model_complete(model, X_test, y_test, model_name="model", 
                              task_type="auto", y_prob=None):
        """完整的模型评估流水线"""
        
        # 自动检测任务类型
        if task_type == "auto":
            if hasattr(model, 'predict_proba') or len(np.unique(y_test)) < 20:
                task_type = "classification"
            else:
                task_type = "regression"
        
        # 进行预测
        y_pred = model.predict(X_test)
        if y_prob is None and hasattr(model, 'predict_proba'):
            y_prob = model.predict_proba(X_test)
        
        # 执行评估
        if task_type == "regression":
            results = evaluator.evaluate_regression(y_test, y_pred, model_name)
        else:
            results = evaluator.evaluate_classification(y_test, y_pred, y_prob, model_name=model_name)
        
        # 保存结果
        saved_files = evaluator.save_evaluation_artifacts(results, model_name)
        
        return results, saved_files
    
    return evaluate_model_complete

if __name__ == "__main__":
    # 示例使用
    evaluator = ModelEvaluator()
    
    # 创建示例数据
    np.random.seed(42)
    n_samples = 200
    
    # 回归示例
    y_true_reg = np.random.randn(n_samples) * 10 + 50
    y_pred_reg = y_true_reg + np.random.randn(n_samples) * 2
    
    reg_metrics = evaluator.evaluate_regression(y_true_reg, y_pred_reg, "水声传播损失预测模型")
    
    # 分类示例
    y_true_cls = np.random.choice([0, 1, 2], n_samples, p=[0.4, 0.35, 0.25])
    y_pred_cls = y_true_cls.copy()
    errors = np.random.choice(n_samples, size=n_samples//10, replace=False)
    y_pred_cls[errors] = np.random.choice([0, 1, 2], len(errors))
    
    # 模拟概率预测
    y_prob_cls = np.random.dirichlet([2, 2, 2], n_samples)
    
    cls_metrics = evaluator.evaluate_classification(
        y_true_cls, y_pred_cls, y_prob_cls, 
        class_names=['稳定', '中等', '不稳定'],
        model_name="水声信道状态分类模型"
    )
    
    # 生成报告
    print("\n" + "="*60)
    print("回归模型评估报告")
    print("="*60)
    print(evaluator.generate_report(reg_metrics, "水声传播损失预测模型"))
    
    print("\n" + "="*60)
    print("分类模型评估报告") 
    print("="*60)
    print(evaluator.generate_report(cls_metrics, "水声信道状态分类模型"))