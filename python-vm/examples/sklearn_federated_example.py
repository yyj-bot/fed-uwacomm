#!/usr/bin/env python3
"""
FedUWAComm 纯Scikit-learn联邦学习示例

这个示例展示了如何使用简化后的纯Scikit-learn框架进行联邦学习。
移除了PyTorch依赖，使协作者更容易理解和部署。
"""

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split
from sklearn.datasets import make_regression

# 导入FedUWAComm联邦学习模块
import sys
sys.path.append('../src')

from feduwacomm.ml import (
    FederatedLearningClient,
    FederatedLearningCoordinator,
    MLConfig,
    MLAlgorithm,
    VMClient
)


def create_sample_data(n_samples=1000, n_features=10, noise=0.1):
    """创建示例数据集"""
    X, y = make_regression(
        n_samples=n_samples,
        n_features=n_features,
        noise=noise,
        random_state=42
    )
    
    # 转换为DataFrame格式
    feature_names = [f'feature_{i}' for i in range(n_features)]
    X_df = pd.DataFrame(X, columns=feature_names)
    y_series = pd.Series(y, name='target')
    
    return X_df, y_series


def example_random_forest_federation():
    """随机森林联邦学习示例"""
    print("🌲 随机森林联邦学习示例")
    print("=" * 50)
    
    # 1. 创建配置
    config = MLConfig(
        algorithm=MLAlgorithm.RANDOM_FOREST,
        epochs=3,  # 对于sklearn，表示重复训练次数
        hyperparameters={
            'n_estimators': 50,
            'random_state': 42
        }
    )
    
    # 2. 创建示例数据
    X, y = create_sample_data()
    
    # 3. 模拟多个客户端的数据分片
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    # 将训练数据分成3个客户端
    n_clients = 3
    client_data = []
    samples_per_client = len(X_train) // n_clients
    
    for i in range(n_clients):
        start_idx = i * samples_per_client
        end_idx = start_idx + samples_per_client if i < n_clients - 1 else len(X_train)
        
        client_X = X_train.iloc[start_idx:end_idx]
        client_y = y_train.iloc[start_idx:end_idx]
        client_data.append((client_X, client_y))
    
    # 4. 创建联邦学习客户端
    clients = []
    for i, (client_X, client_y) in enumerate(client_data):
        # 为每个客户端创建独立的模型
        model = RandomForestRegressor(
            n_estimators=config.hyperparameters['n_estimators'],
            random_state=config.hyperparameters['random_state']
        )
        
        client = FederatedLearningClient(
            client_id=f"client_{i+1}",
            model=model,
            config=config
        )
        
        # 加载本地数据
        client.load_local_data(client_X, client_y)
        clients.append(client)
        print(f"✅ 客户端 {i+1} 创建成功，数据量: {len(client_X)}")
    
    # 5. 创建协调器并注册客户端
    coordinator = FederatedLearningCoordinator(config)
    for client in clients:
        coordinator.register_client(client)
    
    print(f"\n📊 联邦学习设置完成:")
    print(f"   - 客户端数量: {len(clients)}")
    print(f"   - 总训练样本: {len(X_train)}")
    print(f"   - 测试样本: {len(X_test)}")
    print(f"   - 特征数量: {X.shape[1]}")
    
    # 6. 运行联邦学习
    print(f"\n🚀 开始联邦学习训练...")
    result = coordinator.run_federated_training()
    
    print(f"\n🎉 联邦学习完成!")
    print(f"   - 训练轮数: {result.get('rounds_completed', 0)}")
    print(f"   - 最终损失: {result.get('final_loss', 0):.4f}")
    
    return result


def example_linear_regression_federation():
    """线性回归联邦学习示例"""
    print("\n📈 线性回归联邦学习示例")
    print("=" * 50)
    
    # 1. 创建配置
    config = MLConfig(
        algorithm=MLAlgorithm.LINEAR_REGRESSION,
        epochs=2,
        hyperparameters={}
    )
    
    # 2. 创建简单的线性数据
    X, y = create_sample_data(n_samples=500, n_features=5, noise=0.05)
    
    # 3. 创建两个客户端
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
    
    mid_point = len(X_train) // 2
    client1_X, client1_y = X_train.iloc[:mid_point], y_train.iloc[:mid_point]
    client2_X, client2_y = X_train.iloc[mid_point:], y_train.iloc[mid_point:]
    
    # 4. 创建联邦学习客户端
    clients = []
    for i, (client_X, client_y) in enumerate([(client1_X, client1_y), (client2_X, client2_y)]):
        model = LinearRegression()
        
        client = FederatedLearningClient(
            client_id=f"linear_client_{i+1}",
            model=model,
            config=config
        )
        
        client.load_local_data(client_X, client_y)
        clients.append(client)
        print(f"✅ 线性回归客户端 {i+1} 创建成功，数据量: {len(client_X)}")
    
    # 5. 运行联邦学习
    coordinator = FederatedLearningCoordinator(config)
    for client in clients:
        coordinator.register_client(client)
    
    print(f"\n🚀 开始线性回归联邦学习...")
    result = coordinator.run_federated_training()
    
    print(f"🎉 线性回归联邦学习完成!")
    print(f"   - 最终R²得分: {result.get('final_accuracy', 0):.4f}")
    
    return result


def example_vm_client_usage():
    """VMClient使用示例"""
    print("\n🖥️ VMClient使用示例")
    print("=" * 50)
    
    # 1. 创建VM客户端
    vm_client = VMClient(
        vm_id="demo_vm_001",
        base_url="http://localhost:8080"  # 示例URL
    )
    
    # 2. 创建模型
    model = RandomForestRegressor(n_estimators=30, random_state=42)
    
    # 3. 创建联邦学习客户端
    config = MLConfig(algorithm=MLAlgorithm.RANDOM_FOREST, epochs=2)
    success = vm_client.create_federated_client(model, config)
    
    if success:
        print("✅ VM客户端联邦学习功能创建成功")
        print("   - 模型类型: RandomForest")
        print("   - 框架: 纯Scikit-learn")
        print("   - 部署友好: 无需GPU，纯CPU计算")
    else:
        print("❌ VM客户端创建失败")
    
    return success


def main():
    """主函数"""
    print("🎯 FedUWAComm 纯Scikit-learn联邦学习演示")
    print("=" * 60)
    print("✨ 特点:")
    print("   - 移除PyTorch依赖，简化协作")
    print("   - 纯CPU计算，无需GPU")
    print("   - 支持多种Scikit-learn算法")
    print("   - 易于理解和部署")
    print()
    
    try:
        # 运行示例
        example_random_forest_federation()
        example_linear_regression_federation()
        example_vm_client_usage()
        
        print(f"\n🎊 所有示例运行完成!")
        print("💡 提示: 现在您可以轻松地与协作者分享这个简化的框架了!")
        
    except Exception as e:
        print(f"\n❌ 运行示例时出错: {e}")
        print("请检查依赖是否正确安装")


if __name__ == "__main__":
    main()
