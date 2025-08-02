#!/usr/bin/env python3
"""
FedUWAComm 完整标准工作流
按照标准流程：环境生成 → BELLHOP仿真 → 特征提取 → 数据库存储 → 机器学习训练
"""

import sys
import os
import time
from pathlib import Path
import logging

# 添加src目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / 'src'))

from feduwacomm.acoustic.generate_environments import BellhopEnvGenerator
from feduwacomm.acoustic.run_bellhop import BellhopProcessor
from feduwacomm.ml.feature_extractor import BellhopFeatureExtractor
from feduwacomm.database.database_v2 import DatabaseManagerV2
from feduwacomm.ml.random_forest_trainer import RandomForestTrainer
from feduwacomm.ml.model_evaluator import ModelEvaluator

# 设置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def check_environment():
    """检查环境配置"""
    print("🔍 检查环境配置...")
    
    # 检查.env文件
    env_file = Path('.env')
    if not env_file.exists():
        print("❌ .env文件不存在")
        print("请复制 .env.example 为 .env 并配置数据库信息")
        return False
    
    print("✅ .env文件存在")
    
    # 检查数据目录
    data_dir = Path('data/bellhop')
    data_dir.mkdir(parents=True, exist_ok=True)
    print("✅ 数据目录已准备")
    
    return True

def step1_generate_bellhop_environments(num_environments=100):
    """步骤1: 生成BELLHOP环境文件"""
    print("\n" + "="*60)
    print("📁 步骤1: 生成BELLHOP环境文件")
    print("="*60)
    
    try:
        generator = BellhopEnvGenerator()
        
        print(f"🔄 开始生成 {num_environments} 个环境文件...")
        
        # 使用内置的方法生成所有环境文件
        files_generated = generator.generate_all_environments(num_environments)
        
        if files_generated and len(files_generated) > 0:
            print(f"✅ 成功生成 {len(files_generated)} 个环境文件")
            return True
        else:
            print("❌ 环境文件生成失败：未生成任何文件")
            return False
        
    except Exception as e:
        print(f"❌ 环境文件生成失败: {e}")
        return False

def step2_run_bellhop_simulation():
    """步骤2: 运行BELLHOP仿真"""
    print("\n" + "="*60)
    print("🌊 步骤2: 运行BELLHOP仿真")
    print("="*60)
    
    try:
        processor = BellhopProcessor()
        
        # 获取所有环境文件
        env_files = list(Path('data/bellhop').glob('*.env'))
        
        if not env_files:
            print("❌ 未找到环境文件，请先运行步骤1")
            return False
        
        print(f"🔄 找到 {len(env_files)} 个环境文件，开始仿真...")
        
        success_count = 0
        total_count = len(env_files)
        
        for i, env_file in enumerate(env_files):
            try:
                # 运行BELLHOP仿真
                success = processor.run_bellhop(str(env_file))
                
                if success:
                    success_count += 1
                
                if (i + 1) % 20 == 0:
                    print(f"   已处理 {i+1}/{total_count} 个文件...")
                    
            except Exception as e:
                logger.warning(f"处理 {env_file.name} 失败: {e}")
                continue
        
        print(f"✅ BELLHOP仿真完成: {success_count}/{total_count} 成功")
        
        if success_count == 0:
            print("❌ 没有成功的仿真结果")
            return False
            
        return True
        
    except Exception as e:
        print(f"❌ BELLHOP仿真失败: {e}")
        return False

def step3_extract_features():
    """步骤3: 从BELLHOP输出文件中提取特征"""
    print("\n" + "="*60)
    print("🔍 步骤3: 特征提取")
    print("="*60)
    
    try:
        extractor = BellhopFeatureExtractor()
        
        print("🔄 开始特征提取...")
        
        # 从data/bellhop目录提取特征
        features_df = extractor.batch_extract_features()
        
        if features_df is None or len(features_df) == 0:
            print("❌ 特征提取失败，未获得有效特征")
            return False
        
        # 保存特征到CSV
        csv_file = "bellhop_features_extracted.csv"
        features_df.to_csv(csv_file, index=False)
        
        print(f"✅ 特征提取完成:")
        print(f"   - 样本数: {len(features_df)}")
        print(f"   - 特征数: {len(features_df.columns)}")
        print(f"   - 保存到: {csv_file}")
        
        return csv_file
        
    except Exception as e:
        print(f"❌ 特征提取失败: {e}")
        return False

def step4_database_storage(csv_file):
    """步骤4: 数据库存储"""
    print("\n" + "="*60)
    print("💾 步骤4: 数据库存储")
    print("="*60)
    
    try:
        db = DatabaseManagerV2()
        
        print("🔄 连接数据库...")
        if not db.connect():
            print("❌ 数据库连接失败")
            return False
        
        print("✅ 数据库连接成功")
        
        print("🔄 创建表并导入数据...")
        success = db.create_and_populate_from_csv(csv_file)
        
        if success:
            print("✅ 数据库存储完成")
        else:
            print("❌ 数据库存储失败")
            
        db.disconnect()
        return success
        
    except Exception as e:
        print(f"❌ 数据库存储失败: {e}")
        return False

def step5_machine_learning():
    """步骤5: 机器学习训练"""
    print("\n" + "="*60)
    print("🤖 步骤5: 机器学习训练")
    print("="*60)
    
    try:
        # 使用数据库中的数据进行训练
        db = DatabaseManagerV2()
        db.connect()
        
        # 获取数据
        features_df = db.get_features_for_training()
        db.disconnect()
        
        if features_df is None or len(features_df) == 0:
            print("❌ 无法从数据库获取训练数据")
            return False
        
        print(f"✅ 从数据库获取了 {len(features_df)} 条训练数据")
        
        # 初始化训练器
        trainer = RandomForestTrainer()
        
        # 数据准备 - 使用实际存在的数值型目标变量
        target_cols = [
            'shd_mean_tl',           # 传播损失(传输损失)
            'arr_mean_amplitude',    # 信号强度
            'arr_mean_time',         # 到达时间
            'ray_path_complexity',   # 路径复杂度
            'shd_energy_distribution' # 能量分布
        ]
        available_targets = [col for col in target_cols if col in features_df.columns]
        
        if not available_targets:
            print("❌ 未找到可用的目标变量")
            return False
        
        print(f"🎯 目标变量: {available_targets}")
        
        # 数据清理：移除包含NaN的行
        print("🧹 清理数据中的空值...")
        original_size = len(features_df)
        
        # 选择数值型特征列
        numeric_cols = features_df.select_dtypes(include=['number']).columns.tolist()
        # 移除id列
        if 'id' in numeric_cols:
            numeric_cols.remove('id')
        
        # 移除包含NaN的行
        clean_df = features_df.dropna(subset=available_targets + numeric_cols[:20])  # 只检查前20个数值列
        
        print(f"   原始数据: {original_size} 行")
        print(f"   清理后: {len(clean_df)} 行")
        print(f"   移除了 {original_size - len(clean_df)} 行包含空值的数据")
        
        if len(clean_df) < 10:
            print("❌ 清理后数据太少，无法训练模型")
            return False
        
        # 训练模型
        X, y = trainer.prepare_data(clean_df, target_cols=available_targets)
        
        print("🔄 开始训练随机森林模型...")
        trainer.train_regressor(X, y)
        
        # 保存模型
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        model_file = f"models/rf_model_{timestamp}.joblib"
        trainer.save_model(model_file)
        
        print(f"✅ 模型训练完成，保存到: {model_file}")
        
        # 模型评估
        evaluator = ModelEvaluator()
        metrics = evaluator.evaluate_model(trainer.regressor, X, y)
        
        print("📊 模型性能:")
        for metric, value in metrics.items():
            print(f"   - {metric}: {value:.4f}")
        
        return True
        
    except Exception as e:
        print(f"❌ 机器学习训练失败: {e}")
        return False

def main():
    """主工作流程"""
    print("="*60)
    print("🚀 FedUWAComm 完整标准工作流")
    print("="*60)
    print("流程: 环境生成 → BELLHOP仿真 → 特征提取 → 数据库存储 → 机器学习")
    print("="*60)
    
    # 检查环境
    if not check_environment():
        return False
    
    # 询问是否要生成新的环境文件
    print("\n选择运行模式:")
    print("1. 完整流程 (生成环境 → 仿真 → 特征提取 → 训练)")
    print("2. 从仿真开始 (使用现有环境文件)")
    print("3. 从特征提取开始")
    print("4. 仅机器学习训练")
    
    try:
        choice = input("请选择 (1-4): ").strip()
    except KeyboardInterrupt:
        print("\n用户取消操作")
        return False
    
    start_step = 1
    if choice == "2":
        start_step = 2
    elif choice == "3":
        start_step = 3
    elif choice == "4":
        start_step = 5
    elif choice != "1":
        print("无效选择，使用完整流程")
        start_step = 1
    
    # 执行工作流
    start_time = time.time()
    
    try:
        if start_step <= 1:
            if not step1_generate_bellhop_environments():
                return False
        
        if start_step <= 2:
            if not step2_run_bellhop_simulation():
                return False
        
        csv_file = None
        if start_step <= 3:
            csv_file = step3_extract_features()
            if not csv_file:
                return False
        
        if start_step <= 4:
            if csv_file and not step4_database_storage(csv_file):
                return False
        
        if start_step <= 5:
            if not step5_machine_learning():
                return False
        
        # 工作流完成
        elapsed_time = time.time() - start_time
        print("\n" + "="*60)
        print("🎉 工作流完成!")
        print(f"⏱️  总耗时: {elapsed_time:.1f} 秒")
        print("="*60)
        
        return True
        
    except KeyboardInterrupt:
        print("\n❌ 用户中断工作流")
        return False
    except Exception as e:
        print(f"\n❌ 工作流执行失败: {e}")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1) 