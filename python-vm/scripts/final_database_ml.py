#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
最终版数据库机器学习工作流
========================
功能说明：
1. 从BELLHOP仿真结果中提取特征
2. 将特征数据存储到数据库
3. 基于特征数据训练机器学习模型
4. 评估模型性能并保存结果

输入：BELLHOP仿真输出文件（.prt、.arr、.ray、.shd）
输出：
- 训练好的随机森林模型（回归和分类）
- 特征数据库
- 模型评估报告
- 特征CSV文件

工作流程：
generate_environments.py → run_bellhop_batch.py → final_database_ml.py
    (生成.env文件)         (运行BELLHOP仿真)        (特征提取和ML)

特征类型：
1. 环境特征：频率、深度、声源配置等
2. 传播特征：到达时间、振幅、射线路径等
3. 传输损失特征：能量分布、梯度等
"""

import os
import sys
import pandas as pd
import numpy as np
from pathlib import Path
from typing import Dict, List, Any
import logging

# 添加src目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / 'src'))

# 导入自定义模块
from feduwacomm.ml.feature_extractor import BellhopFeatureExtractor     # 特征提取器
from feduwacomm.database.database import DatabaseManager           # 数据库管理器
from feduwacomm.ml.random_forest_trainer import RandomForestTrainer     # 随机森林训练器
from feduwacomm.ml.model_evaluator import ModelEvaluator               # 模型评估器

# 设置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class FinalDatabaseMLWorkflow:
    """
    最终版数据库机器学习工作流类
    
    这个类整合了整个机器学习流水线：
    1. 特征提取：从BELLHOP输出文件中提取声学特征
    2. 数据管理：将特征存储到数据库中
    3. 模型训练：使用随机森林算法训练回归和分类模型
    4. 模型评估：评估模型性能并生成报告
    5. 结果保存：保存模型和分析结果
    """
    
    def __init__(self):
        """
        初始化工作流
        
        设置各个组件：
        - 项目路径和数据目录
        - 数据库管理器
        - 特征提取器
        - 模型训练器
        - 模型评估器
        """
        self.project_root = project_root
        self.bellhop_dir = project_root / "data" / "bellhop"  # BELLHOP数据目录
        self.db = DatabaseManager()                         # 数据库管理器
        self.extractor = BellhopFeatureExtractor()            # 特征提取器
        self.trainer = RandomForestTrainer()                  # 随机森林训练器
        self.evaluator = ModelEvaluator()                     # 模型评估器
        
    def run_complete_workflow(self):
        """
        运行完整的机器学习工作流
        
        执行步骤：
        1. 特征提取：从BELLHOP输出文件提取声学特征
        2. 数据库存储：将特征数据存储到数据库
        3. 数据加载：从数据库加载训练数据
        4. 数据准备：清理和预处理特征数据
        5. 模型训练：训练随机森林回归和分类模型
        6. 模型评估：评估模型性能
        7. 结果保存：保存模型和生成报告
        
        返回:
            bool: 工作流是否成功执行
        """
        
        logger.info("开始运行最终版数据库ML工作流")
        
        try:
            # 步骤1: 提取特征并存储到数据库
            logger.info("\n" + "="*50)
            logger.info("第1步：特征提取和数据库存储")
            logger.info("="*50)
            if not self.extract_and_store_features():
                logger.error("特征提取失败，工作流终止")
                return False
            
            # 步骤2: 从数据库获取特征用于训练
            logger.info("\n" + "="*50)
            logger.info("第2步：从数据库加载特征数据")
            logger.info("="*50)
            df = self.load_features_from_database()
            if df is None or len(df) < 10:
                logger.error("数据集太小，无法进行机器学习训练")
                return False
            
            # 步骤3: 准备训练数据
            logger.info("\n" + "="*50)
            logger.info("第3步：数据预处理和准备")
            logger.info("="*50)
            X, y_reg, y_clf = self.prepare_training_data(df)
            
            # 步骤4: 训练模型
            logger.info("\n" + "="*50)
            logger.info("第4步：机器学习模型训练")
            logger.info("="*50)
            models = self.train_models(X, y_reg, y_clf)
            
            # 步骤5: 评估模型
            logger.info("\n" + "="*50)
            logger.info("第5步：模型性能评估")
            logger.info("="*50)
            self.evaluate_models(models, X, y_reg, y_clf)
            
            # 步骤6: 保存模型和结果
            logger.info("\n" + "="*50)
            logger.info("第6步：保存结果和生成报告")
            logger.info("="*50)
            self.save_results(models, df)
            
            logger.info("\n" + "="*50)
            logger.info("🎉 完整工作流执行成功！")
            logger.info("="*50)
            return True
            
        except Exception as e:
            logger.error(f"工作流执行失败: {e}")
            import traceback
            logger.error(traceback.format_exc())
            return False
    
    def extract_and_store_features(self) -> bool:
        """
        特征提取和数据库存储
        
        功能：
        1. 扫描BELLHOP输出文件
        2. 使用特征提取器提取声学特征
        3. 将特征数据保存为CSV文件
        4. 创建数据库表并导入数据
        
        特征类型包括：
        - 环境参数：频率、深度、几何配置
        - 声速剖面：最小/最大/平均声速、层数等
        - 到达时间：最早/最晚到达、时间分布统计
        - 射线路径：路径复杂度、转向点、反射次数
        - 传输损失：能量分布、衰减梯度
        
        返回:
            bool: 是否成功
        """
        
        logger.info("开始特征提取和数据库存储...")
        
        # 连接数据库
        if not self.db.connect():
            logger.error("无法连接数据库")
            return False
        
        # 查找所有环境文件
        # 特征提取器会自动寻找对应的输出文件(.prt, .arr, .ray, .shd)
        env_files = list(self.bellhop_dir.glob("*.env"))
        logger.info(f"找到 {len(env_files)} 个环境文件")
        
        if len(env_files) == 0:
            logger.error("没有找到环境文件")
            logger.error("请先运行 python tools/generate_environments.py")
            return False
        
        # 检查是否有对应的输出文件
        output_files = list(self.bellhop_dir.glob("*.prt"))
        logger.info(f"找到 {len(output_files)} 个BELLHOP输出文件")
        
        if len(output_files) == 0:
            logger.error("没有找到BELLHOP输出文件")
            logger.error("请先运行 python run_bellhop_batch.py")
            return False
        
        # 批量提取特征
        all_features = []
        successful_extractions = 0
        failed_extractions = 0
        
        logger.info("开始批量特征提取...")
        for i, env_file in enumerate(env_files, 1):
            try:
                # 使用特征提取器提取所有可用特征
                env_basename = env_file.stem  # 例如：B001_arr
                features = self.extractor.extract_all_features(env_basename)
                
                if features:
                    all_features.append(features)
                    successful_extractions += 1
                    
                    # 每处理20个文件输出一次进度
                    if i % 20 == 0:
                        logger.info(f"已处理: {i}/{len(env_files)} (成功: {successful_extractions})")
                else:
                    failed_extractions += 1
                    logger.warning(f"环境 {env_basename} 特征提取失败：返回空特征")
                    
            except Exception as e:
                failed_extractions += 1
                logger.warning(f"处理 {env_file.name} 失败: {e}")
                continue
        
        logger.info(f"特征提取完成：成功 {successful_extractions} 个，失败 {failed_extractions} 个")
        
        if len(all_features) == 0:
            logger.error("没有成功提取任何特征")
            return False
        
        # 保存特征到CSV文件
        csv_file = self.project_root / "bellhop_features_final.csv"
        df = pd.DataFrame(all_features)
        df.to_csv(csv_file, index=False, encoding='utf-8')
        logger.info(f"特征数据保存到: {csv_file}")
        logger.info(f"特征维度: {df.shape} (样本数: {len(df)}, 特征数: {len(df.columns)})")
        
        # 显示特征概览
        logger.info(f"特征类型概览:")
        for col in df.columns[:10]:  # 显示前10个特征
            logger.info(f"  - {col}: {df[col].dtype}")
        if len(df.columns) > 10:
            logger.info(f"  ... 还有 {len(df.columns)-10} 个特征")
        
        # 创建数据库表
        if self.db.create_table_from_csv(str(csv_file)):
            logger.info("数据库表创建成功")
            
            # 导入数据到数据库
            if self.db.insert_csv_data(str(csv_file)):
                logger.info("数据导入数据库成功")
                return True
            else:
                logger.error("数据导入失败")
        else:
            logger.error("数据库表创建失败")
        
        return False
    
    def load_features_from_database(self) -> pd.DataFrame:
        """
        从数据库加载特征数据
        
        功能：
        1. 从数据库查询所有特征数据
        2. 转换数据类型（解决数据库类型问题）
        3. 数据清理和预处理
        
        返回:
            pd.DataFrame: 清理后的特征数据，如果失败则返回None
        """
        
        logger.info("从数据库加载特征数据...")
        
        try:
            # 直接从数据库获取数据，避免pandas.read_sql的类型问题
            with self.db.connection.cursor() as cursor:
                cursor.execute("SELECT * FROM features")
                results = cursor.fetchall()
                
                if not results:
                    logger.error("数据库中没有数据")
                    return None
                
                # 手动构建DataFrame
                df = pd.DataFrame(results)
                
                logger.info(f"从数据库加载了 {len(df)} 条记录，{len(df.columns)} 个特征")
                
                # 手动转换数值列
                # 这些是已知的数值特征列名
                numeric_columns = [
                    # 环境特征
                    'env_frequency', 'env_source_count', 'env_receiver_count', 'env_range_count', 'env_max_depth',
                    # PRT文件特征（运行参数）
                    'prt_freq', 'prt_ssp_points', 'prt_ssp_min_speed', 'prt_ssp_max_speed', 'prt_ssp_mean_speed', 
                    'prt_ssp_std_speed', 'prt_depth_min', 'prt_depth_max', 'prt_depth_range', 'prt_run_time',
                    # ARR文件特征（到达时间）
                    'arr_arrival_count', 'arr_min_time', 'arr_max_time', 'arr_mean_time', 'arr_std_time',
                    'arr_min_amplitude', 'arr_max_amplitude', 'arr_mean_amplitude', 'arr_std_amplitude', 
                    'arr_angle_spread', 'arr_phase_variance',
                    # RAY文件特征（射线路径）
                    'ray_ray_count', 'ray_total_points', 'ray_max_range', 'ray_max_depth', 'ray_avg_ray_length',
                    'ray_path_complexity', 'ray_turning_points', 'ray_bounce_count',
                    # SHD文件特征（传输损失）
                    'shd_grid_size', 'shd_range_extent', 'shd_depth_extent', 'shd_min_tl', 'shd_max_tl',
                    'shd_mean_tl', 'shd_std_tl', 'shd_tl_gradient', 'shd_energy_distribution'
                ]
                
                # 转换数值列
                converted_count = 0
                for col in numeric_columns:
                    if col in df.columns:
                        df[col] = pd.to_numeric(df[col], errors='coerce')
                        converted_count += 1
                
                logger.info(f"成功转换 {converted_count} 个数值列")
            
            # 数据清理
            df = self.clean_features(df)
            
            return df
            
        except Exception as e:
            logger.error(f"从数据库加载数据失败: {e}")
            return None
    
    def clean_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        清理特征数据
        
        功能：
        1. 移除系统字段（id, created_at, updated_at）
        2. 移除完全空的列
        3. 移除完全相同的列（无变化的特征）
        4. 填充缺失值
        
        参数:
            df: 原始特征数据
            
        返回:
            pd.DataFrame: 清理后的特征数据
        """
        
        logger.info("开始数据清理...")
        original_shape = df.shape
        
        # 移除系统字段
        system_cols = ['id', 'created_at', 'updated_at']
        system_cols_found = [col for col in system_cols if col in df.columns]
        if system_cols_found:
            df = df.drop(columns=system_cols_found)
            logger.info(f"移除系统字段: {system_cols_found}")
        
        # 移除完全空的列
        empty_cols = df.columns[df.isnull().all()].tolist()
        if empty_cols:
            df = df.dropna(axis=1, how='all')
            logger.info(f"移除完全空的列: {empty_cols}")
        
        # 移除完全相同的列（标准差为0的列）
        constant_cols = []
        for col in df.columns:
            if df[col].nunique() <= 1:
                constant_cols.append(col)
        
        if constant_cols:
            df = df.drop(columns=constant_cols)
            logger.info(f"移除常数列: {constant_cols}")
        
        # 填充数值列的空值
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        if len(numeric_cols) > 0:
            null_counts_before = df[numeric_cols].isnull().sum().sum()
            df[numeric_cols] = df[numeric_cols].fillna(0)
            logger.info(f"填充 {len(numeric_cols)} 个数值列的 {null_counts_before} 个空值")
        
        # 填充字符串列的空值
        string_cols = df.select_dtypes(include=['object']).columns
        if len(string_cols) > 0:
            null_counts_before = df[string_cols].isnull().sum().sum()
            df[string_cols] = df[string_cols].fillna('unknown')
            logger.info(f"填充 {len(string_cols)} 个字符串列的 {null_counts_before} 个空值")
        
        logger.info(f"数据清理完成：{original_shape} → {df.shape}")
        
        return df
    
    def prepare_training_data(self, df: pd.DataFrame) -> tuple:
        """
        准备训练数据
        
        功能：
        1. 选择特征列（排除标识符列）
        2. 创建目标变量
        3. 生成回归和分类任务的标签
        
        参数:
            df: 清理后的特征数据
            
        返回:
            tuple: (特征矩阵X, 回归目标y_reg, 分类目标y_clf)
        """
        
        logger.info("准备训练数据...")
        
        # 选择特征列（排除目标变量和标识符）
        exclude_cols = ['filename', 'env_id', 'run_type']  # 排除非特征列
        feature_cols = [col for col in df.columns if col not in exclude_cols]
        X = df[feature_cols]
        
        # 只保留数值特征用于机器学习
        X = X.select_dtypes(include=[np.number])
        
        logger.info(f"特征矩阵维度: {X.shape}")
        logger.info(f"使用的特征列: {list(X.columns[:5])}..." if len(X.columns) > 5 else f"使用的特征列: {list(X.columns)}")
        
        # 创建目标变量
        # 目标变量设计：基于声学物理意义的复合指标
        if 'prt_freq' in X.columns and 'env_max_depth' in X.columns:
            # 使用频率-深度复合指标：体现声波传播的基本物理关系
            # 对数变换用于处理深度的非线性影响
            y_reg = X['prt_freq'] * np.log1p(X['env_max_depth'])
            target_description = "频率×log(深度+1)"
        elif 'env_frequency' in X.columns and 'env_max_depth' in X.columns:
            # 备选方案
            y_reg = X['env_frequency'] * np.log1p(X['env_max_depth'])
            target_description = "环境频率×log(深度+1)"
        elif len(X.columns) > 0:
            # 最后方案：使用第一个可用特征
            y_reg = X.iloc[:, 0]
            target_description = f"第一个特征({X.columns[0]})"
        else:
            raise ValueError("没有可用的数值特征来创建目标变量")
        
        # 分类目标：基于回归目标的中位数分割
        # 创建二分类任务：高值类(1) vs 低值类(0)
        y_clf = (y_reg > y_reg.median()).astype(int)
        
        # 输出目标变量统计信息
        logger.info(f"回归目标 ({target_description}):")
        logger.info(f"  - 均值: {y_reg.mean():.2f}")
        logger.info(f"  - 标准差: {y_reg.std():.2f}")
        logger.info(f"  - 范围: [{y_reg.min():.2f}, {y_reg.max():.2f}]")
        
        logger.info(f"分类目标分布:")
        logger.info(f"  - 类别0 (低值): {(y_clf==0).sum()} 样本 ({(y_clf==0).mean()*100:.1f}%)")
        logger.info(f"  - 类别1 (高值): {(y_clf==1).sum()} 样本 ({(y_clf==1).mean()*100:.1f}%)")
        
        return X, y_reg, y_clf
    
    def train_models(self, X: pd.DataFrame, y_reg: pd.Series, y_clf: pd.Series) -> Dict[str, Any]:
        """
        训练机器学习模型
        
        功能：
        1. 训练随机森林回归模型（预测连续值）
        2. 训练随机森林分类模型（预测类别）
        3. 使用网格搜索优化超参数
        
        参数:
            X: 特征矩阵
            y_reg: 回归目标
            y_clf: 分类目标
            
        返回:
            dict: 包含训练好的模型和相关信息
        """
        
        logger.info("开始模型训练...")
        
        models = {}
        
        # 训练随机森林回归模型
        logger.info("训练随机森林回归模型...")
        logger.info("- 任务：预测声学传播的连续数值指标")
        logger.info("- 算法：随机森林回归 + 网格搜索优化")
        
        reg_results = self.trainer.train_regressor(X, y_reg, optimize_params=True)
        models['regressor'] = reg_results
        
        if reg_results and 'best_score' in reg_results:
            logger.info(f"回归模型训练完成，最佳R²得分: {reg_results['best_score']:.4f}")
        
        # 训练随机森林分类模型
        logger.info("训练随机森林分类模型...")
        logger.info("- 任务：预测声学传播质量的分类标签")
        logger.info("- 算法：随机森林分类 + 网格搜索优化")
        
        clf_results = self.trainer.train_classifier(X, y_clf, optimize_params=True)
        models['classifier'] = clf_results
        
        if clf_results and 'best_score' in clf_results:
            logger.info(f"分类模型训练完成，最佳准确率: {clf_results['best_score']:.4f}")
        
        logger.info("所有模型训练完成")
        
        return models
    
    def evaluate_models(self, models: Dict[str, Any], X: pd.DataFrame, y_reg: pd.Series, y_clf: pd.Series):
        """
        评估模型性能
        
        功能：
        1. 评估回归模型：R²、RMSE、MAE等指标
        2. 评估分类模型：准确率、F1、精确率、召回率等
        3. 计算特征重要性
        
        参数:
            models: 训练好的模型字典
            X: 特征矩阵
            y_reg: 回归目标
            y_clf: 分类目标
        """
        
        logger.info("开始模型评估...")
        
        # 评估回归模型
        if 'regressor' in models and models['regressor']:
            logger.info("\n--- 回归模型评估 ---")
            reg_model = models['regressor']['model']
            reg_metrics = self.evaluator.evaluate_regressor(reg_model, X, y_reg)
            
            logger.info("回归性能指标:")
            logger.info(f"  - R² (决定系数): {reg_metrics['r2']:.4f}")
            logger.info(f"  - RMSE (均方根误差): {reg_metrics['rmse']:.4f}")
            logger.info(f"  - MAE (平均绝对误差): {reg_metrics['mae']:.4f}")
            
            # 显示特征重要性（前5个）
            if hasattr(reg_model, 'feature_importances_'):
                feature_importance = pd.DataFrame({
                    'feature': X.columns,
                    'importance': reg_model.feature_importances_
                }).sort_values('importance', ascending=False)
                
                logger.info("回归模型特征重要性 (Top 5):")
                for i, (_, row) in enumerate(feature_importance.head().iterrows()):
                    logger.info(f"  {i+1}. {row['feature']}: {row['importance']:.4f}")
        
        # 评估分类模型
        if 'classifier' in models and models['classifier']:
            logger.info("\n--- 分类模型评估 ---")
            clf_model = models['classifier']['model']
            clf_metrics = self.evaluator.evaluate_classifier(clf_model, X, y_clf)
            
            logger.info("分类性能指标:")
            logger.info(f"  - 准确率 (Accuracy): {clf_metrics['accuracy']:.4f}")
            logger.info(f"  - F1得分: {clf_metrics['f1']:.4f}")
            logger.info(f"  - 精确率 (Precision): {clf_metrics['precision']:.4f}")
            logger.info(f"  - 召回率 (Recall): {clf_metrics['recall']:.4f}")
            
            # 显示特征重要性（前5个）
            if hasattr(clf_model, 'feature_importances_'):
                feature_importance = pd.DataFrame({
                    'feature': X.columns,
                    'importance': clf_model.feature_importances_
                }).sort_values('importance', ascending=False)
                
                logger.info("分类模型特征重要性 (Top 5):")
                for i, (_, row) in enumerate(feature_importance.head().iterrows()):
                    logger.info(f"  {i+1}. {row['feature']}: {row['importance']:.4f}")
    
    def save_results(self, models: Dict[str, Any], df: pd.DataFrame):
        """
        保存结果和生成报告
        
        功能：
        1. 保存训练好的模型文件
        2. 生成机器学习报告
        3. 创建使用示例代码
        
        参数:
            models: 训练好的模型字典
            df: 原始特征数据
        """
        
        logger.info("保存结果和生成报告...")
        
        # 创建结果目录
        results_dir = self.project_root / "results"
        results_dir.mkdir(exist_ok=True)
        
        models_dir = results_dir / "models"
        models_dir.mkdir(exist_ok=True)
        
        # 保存模型文件
        import joblib
        for model_type, model_data in models.items():
            if model_data and 'model' in model_data:
                model_file = models_dir / f"{model_type}_final.joblib"
                joblib.dump(model_data['model'], model_file)
                logger.info(f"[OK] {model_type}模型保存到: {model_file}")
        
        # 创建详细的分析报告
        report_file = results_dir / "final_ml_report.md"
        report_content = f"""# FedUWAComm 机器学习最终报告

## 项目概述
本项目基于BELLHOP水声传播仿真数据，构建机器学习模型预测声学传播特性。

## 数据集信息
- **样本数量**: {len(df)}
- **特征数量**: {len(df.columns)}
- **数据完整性**: {((df.notna().sum().sum()) / (len(df) * len(df.columns)) * 100):.2f}%
- **数据来源**: BELLHOP声学仿真软件输出

## 特征类型
1. **环境特征**: 频率、深度、声源配置等
2. **声速剖面特征**: 最小/最大/平均声速、层数等
3. **到达时间特征**: 最早/最晚到达、时间分布统计
4. **射线路径特征**: 路径复杂度、转向点、反射次数
5. **传输损失特征**: 能量分布、衰减梯度

## 模型性能
"""
        
        # 添加模型性能信息
        if 'regressor' in models and models['regressor']:
            reg_score = models['regressor']['best_score']
            report_content += f"""
### 随机森林回归模型
- **任务**: 预测声学传播的连续数值指标
- **性能**: R² = {reg_score:.4f}
- **算法**: 随机森林 + 网格搜索优化
"""
        
        if 'classifier' in models and models['classifier']:
            clf_score = models['classifier']['best_score']
            report_content += f"""
### 随机森林分类模型
- **任务**: 预测声学传播质量的分类标签
- **性能**: 准确率 = {clf_score:.4f}
- **算法**: 随机森林 + 网格搜索优化
"""
        
        # 添加技术信息
        report_content += f"""
## 技术信息
- **BELLHOP环境文件**: {len(list(self.bellhop_dir.glob('*.env')))}个
- **输出文件**: {len(list(self.bellhop_dir.glob('*.prt')))}个
- **特征提取时间**: {pd.Timestamp.now()}
- **项目目录**: {self.project_root}

## 工作流程
1. **环境生成**: `python tools/generate_environments.py` - 生成100组海洋环境配置
2. **仿真执行**: `python run_bellhop_batch.py` - 运行BELLHOP声学仿真
3. **特征提取**: `python final_database_ml.py` - 提取特征并训练模型

## 模型使用方法
```python
import joblib
import pandas as pd
from pathlib import Path

# 加载训练好的模型
models_dir = Path("results/models")
regressor = joblib.load(models_dir / "regressor_final.joblib")
classifier = joblib.load(models_dir / "classifier_final.joblib")

# 准备新数据（与训练数据格式相同）
# X_new = pd.DataFrame(...)  # 新的特征数据

# 进行预测
# reg_predictions = regressor.predict(X_new)      # 回归预测
# clf_predictions = classifier.predict(X_new)     # 分类预测
# clf_probabilities = classifier.predict_proba(X_new)  # 分类概率
```

## 文件说明
- `models/regressor_final.joblib`: 回归模型文件
- `models/classifier_final.joblib`: 分类模型文件
- `bellhop_features_final.csv`: 特征数据CSV文件
- `final_ml_report.md`: 本报告文件

## 应用场景
1. **声学设计**: 优化声纳系统参数
2. **环境评估**: 评估海洋环境对声传播的影响
3. **预测分析**: 预测不同条件下的声学性能
4. **系统优化**: 优化水声通信系统配置

---
*报告生成时间: {pd.Timestamp.now()}*
"""
        
        # 保存报告
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        logger.info(f"[OK] 详细报告保存到: {report_file}")
        logger.info(f"[OK] 模型文件保存到: {models_dir}")

def main():
    """
    主函数
    
    执行完整的机器学习工作流程：
    1. 初始化工作流实例
    2. 运行完整流程
    3. 输出结果摘要
    """
    
    print("=" * 60)
    print("=== FedUWAComm 水声机器学习工作流 ===")
    print("=" * 60)
    print("功能：基于BELLHOP仿真数据训练声学传播预测模型")
    print("作者：FedUWAComm项目组")
    print("=" * 60)
    
    # 创建工作流实例
    workflow = FinalDatabaseMLWorkflow()
    
    # 运行完整工作流
    success = workflow.run_complete_workflow()
    
    # 输出最终结果
    print("\n" + "=" * 60)
    if success:
        print("[成功] 完整的ML工作流执行成功！")
        print("结果摘要:")
        print("  [OK] 特征已提取并存储到数据库")
        print("  [OK] 随机森林模型已训练和评估")
        print("  [OK] 模型和报告已保存到 results/ 目录")
        print("输出文件:")
        print("  - models/regressor_final.joblib  (回归模型)")
        print("  - models/classifier_final.joblib (分类模型)")
        print("  - final_ml_report.md            (详细报告)")
        print("  - bellhop_features_final.csv    (特征数据)")
    else:
        print("[失败] ML工作流执行失败")
        print("请检查:")
        print("  - BELLHOP输出文件是否存在")
        print("  - 数据库连接是否正常")
        print("  - 特征提取模块是否正常工作")
    print("=" * 60)

if __name__ == "__main__":
    main()