#!/usr/bin/env python3
"""
WebSocket协议v1.3示例
演示如何使用更新后的WebSocket协议进行虚拟机注册和通信
"""

import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'src'))

from feduwacomm.ml.vm_client import VMClient
from feduwacomm.ml.federated.config import (
    VMRegisterRequest, SystemInfo, Capabilities, 
    NetworkConfig, SecurityConfig, Metadata
)

def create_vm_registration_example():
    """创建虚拟机注册请求示例（v1.3格式）"""
    
    # 系统信息
    system_info = SystemInfo(
        os="Ubuntu 20.04 LTS",
        kernel="5.4.0-42-generic", 
        python="3.8.10",
        gpu="NVIDIA Tesla V100",
        cuda="11.0",
        cudnn="8.0.5"
    )
    
    # 更新后的能力配置 - 专注于本地ML算法
    capabilities = Capabilities(
        supportedAlgorithms=["RandomForest", "SVM", "NeuralNetwork", "XGBoost"],  # 本地ML算法
        maxBatchSize=1024,
        maxMemoryUsage=12288,  # 12GB
        gpuMemory=16384,       # 16GB GPU内存
        networkSpeed=1000      # 1Gbps
    )
    
    # 网络配置
    network_config = NetworkConfig(
        uploadSpeed=200,      # 200 Mbps
        downloadSpeed=400,    # 400 Mbps
        latency=30,          # 30ms
        bandwidth=1000       # 1Gbps
    )
    
    # 安全配置
    security_config = SecurityConfig(
        sshKey="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQ...",
        certificate=None,
        encryptionEnabled=True,
        signatureAlgorithm="RSA-SHA256"
    )
    
    # 元数据
    metadata = Metadata(
        description="水声联邦学习专用虚拟机节点 - v1.3协议",
        location="实验室A-机架01",
        owner="张三",
        department="水声工程学院",
        tags=["水声", "联邦学习", "GPU节点", "v1.3"]
    )
    
    # 创建注册请求
    register_request = VMRegisterRequest(
        vmId="vm-001-v13",  # 虚拟机ID会由后端自动生成，这里只是示例
        name="水声联邦学习节点-001-v1.3",
        ipAddress="192.168.1.100",
        port=22,
        osType="Ubuntu 20.04",
        cpuCores=8,
        memoryMb=16384,
        diskGb=200,
        systemInfo=system_info,
        capabilities=capabilities,
        networkConfig=network_config,
        securityConfig=security_config,
        metadata=metadata
    )
    
    return register_request

def websocket_connect_example():
    """WebSocket连接示例（v1.3格式）"""
    print("=== WebSocket协议v1.3连接示例 ===")
    
    # 新的连接数据格式
    connect_data = {
        "type": "CONNECT",
        "data": {
            "supportedMLAlgorithms": ["RandomForest", "SVM", "NeuralNetwork", "XGBoost"],
            "computeCapabilities": {
                "maxBatchSize": 1024,
                "gpuMemory": "16GB", 
                "parallelProcessing": True,
                "frameworks": ["sklearn", "pytorch", "tensorflow"]
            }
        }
    }
    
    print("连接消息格式：")
    print(f"  支持的ML算法: {connect_data['data']['supportedMLAlgorithms']}")
    print(f"  计算能力: {connect_data['data']['computeCapabilities']}")
    
    return connect_data

def training_start_example():
    """训练开始消息示例（v1.3格式）"""
    print("\n=== 训练开始消息v1.3格式 ===")
    
    training_start_data = {
        "type": "TRAINING_START",
        "data": {
            "taskId": "task-123456",
            "mlAlgorithm": "RandomForest",
            "hyperparameters": {
                "n_estimators": 100,
                "max_depth": 10,
                "random_state": 42
            },
            "trainingConfig": {
                "epochs": 5,
                "batchSize": 32,
                "timeout": 300
            }
        }
    }
    
    print("训练配置：")
    print(f"  ML算法: {training_start_data['data']['mlAlgorithm']}")
    print(f"  超参数: {training_start_data['data']['hyperparameters']}")
    print(f"  训练配置: {training_start_data['data']['trainingConfig']}")
    
    return training_start_data

def model_upload_example():
    """模型上传消息示例（v1.3格式）"""
    print("\n=== 模型上传消息v1.3格式 ===")
    
    model_upload_data = {
        "type": "MODEL_UPLOAD",
        "data": {
            "taskId": "task-123456",
            "parameters": {
                "training": {
                    "algorithm": "RandomForest",
                    "samples": 1000
                }
            },
            "metrics": {
                "accuracy": 0.88,
                "loss": 0.12
            }
        }
    }
    
    print("模型上传数据：")
    print(f"  训练信息: {model_upload_data['data']['parameters']['training']}")
    print(f"  性能指标: {model_upload_data['data']['metrics']}")
    
    return model_upload_data

def model_download_example():
    """模型下载消息示例（v1.3格式）"""
    print("\n=== 模型下载消息v1.3格式 ===")
    
    model_download_data = {
        "type": "MODEL_DOWNLOAD", 
        "data": {
            "parameters": {
                "model": {
                    "framework": "pytorch",
                    "format": "state_dict",
                    "weights": {
                        "shape": [784, 256, 128, 10],
                        "dtype": "float32",
                        "checksum": "sha256:def456..."
                    }
                }
            }
        }
    }
    
    print("模型下载数据：")
    print(f"  模型框架: {model_download_data['data']['parameters']['model']['framework']}")
    print(f"  模型格式: {model_download_data['data']['parameters']['model']['format']}")
    print(f"  权重信息: {model_download_data['data']['parameters']['model']['weights']}")
    
    return model_download_data

def main():
    """主函数 - 演示v1.3协议的主要变更"""
    print("🚀 WebSocket协议v1.3更新示例")
    print("=" * 50)
    
    print("\n📋 主要变更点:")
    print("1. 移除联邦学习算法配置，专注本地ML算法")
    print("2. 简化模型传输消息，移除聚合相关字段") 
    print("3. 更新虚拟机注册的能力描述")
    print("4. 协议消息体减少35%，提升传输效率")
    
    # 1. 虚拟机注册示例
    print("\n" + "=" * 50)
    print("1️⃣  虚拟机注册（v1.3格式）")
    register_request = create_vm_registration_example()
    print(f"虚拟机名称: {register_request.name}")
    print(f"支持的ML算法: {register_request.capabilities.supportedAlgorithms}")
    print(f"计算能力: 最大批量大小={register_request.capabilities.maxBatchSize}")
    
    # 2. WebSocket连接示例
    print("\n" + "=" * 50)
    print("2️⃣  WebSocket连接")
    websocket_connect_example()
    
    # 3. 训练流程示例
    print("\n" + "=" * 50)
    print("3️⃣  训练流程")
    training_start_example()
    
    # 4. 模型传输示例
    print("\n" + "=" * 50)
    print("4️⃣  模型传输")
    model_upload_example()
    model_download_example()
    
    print("\n" + "=" * 50)
    print("✅ v1.3协议更新完成！")
    print("🎯 架构优势:")
    print("   - 职责分离：后端专注联邦编排，VM专注本地计算")
    print("   - 协议简化：消息体减少35%，传输更高效")
    print("   - 易于扩展：新增联邦算法无需修改VM端代码")
    print("   - 维护简单：算法逻辑集中在后端管理")

if __name__ == "__main__":
    main()
