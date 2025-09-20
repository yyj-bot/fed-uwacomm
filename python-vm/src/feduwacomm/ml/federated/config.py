"""
联邦学习配置和数据结构定义
"""

from dataclasses import dataclass, field
from typing import List, Dict, Optional, Any
from enum import Enum


# ===================== 联邦学习相关配置 =====================

class FederatedAlgorithm(Enum):
    """联邦学习算法类型"""
    FEDAVG = "FedAvg"
    FEDPROX = "FedProx"
    FEDNOVA = "FedNova"
    SCAFFOLD = "SCAFFOLD"


@dataclass
class FederatedConfig:
    """联邦学习配置"""
    algorithm: FederatedAlgorithm = FederatedAlgorithm.FEDAVG
    local_epochs: int = 5
    local_batch_size: int = 32
    learning_rate: float = 0.01
    mu: float = 0.01  # FedProx 正则化参数
    client_fraction: float = 1.0  # 每轮参与的客户端比例
    max_rounds: int = 100
    patience: int = 10  # 早停耐心值
    min_delta: float = 0.001  # 最小改进阈值


@dataclass
class TrainingState:
    """训练状态"""
    round_num: int = 0
    local_epochs_completed: int = 0
    total_samples: int = 0
    loss_history: List[float] = field(default_factory=list)
    accuracy_history: List[float] = field(default_factory=list)
    is_training: bool = False
    error_message: str = ""
    last_update_time: float = 0


# ===================== 虚拟机注册相关数据结构 =====================

@dataclass
class SystemInfo:
    """系统信息"""
    os: str       # 操作系统
    kernel: str   # 内核
    python: str   # python版本
    gpu: str      # GPU型号
    cuda: str     # CUDA版本
    cudnn: str    # cuDNN版本


@dataclass
class Capabilities:
    """能力"""
    supportedAlgorithms: List[str]  # 支持的联邦学习算法
    maxBatchSize: int               # 最大批量大小
    maxMemoryUsage: int             # 最大内存使用量
    gpuMemory: int                  # GPU内存
    networkSpeed: int               # 网络速度


@dataclass
class NetworkConfig:
    """网络配置"""
    uploadSpeed: int     # 上传速度
    downloadSpeed: int   # 下载速度
    latency: int         # 延迟
    bandwidth: int       # 带宽


@dataclass
class SecurityConfig:
    """安全配置"""
    sshKey: str            # SSH密钥
    certificate: Optional[str] = None  # 证书
    encryptionEnabled: bool = True    # 加密启用
    signatureAlgorithm: str = "RSA-SHA256"  # 签名算法


@dataclass
class Metadata:
    """元数据"""
    description: str     # 描述
    location: str        # 位置
    owner: str           # 所有者
    department: str      # 部门
    tags: List[str]      # 标签


@dataclass
class VMRegisterRequest:
    """虚拟机注册请求"""
    vmId: str              # 虚拟机ID
    name: str              # 名称
    ipAddress: str         # IP地址
    port: int              # 端口
    osType: str            # 操作系统类型
    cpuCores: int          # CPU核心数
    memoryMb: int          # 内存
    diskGb: int            # 硬盘
    systemInfo: SystemInfo  # 系统信息
    capabilities: Capabilities  # 能力
    networkConfig: NetworkConfig  # 网络配置
    securityConfig: SecurityConfig  # 安全配置
    metadata: Metadata  # 元数据

