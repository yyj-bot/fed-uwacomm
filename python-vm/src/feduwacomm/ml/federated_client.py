"""
联邦学习虚拟机

本文件包含 HTTP API 交互和核心联邦学习算法实现。
WebSocket 连接支持将在后续版本中集成。
"""

from dataclasses import dataclass, field, asdict
from typing import List, Dict, Optional, Any, Union, Callable, Tuple
import requests
from enum import Enum
import numpy as np
import pandas as pd
import joblib
import logging
import threading
import time
import copy
from pathlib import Path
import json

# 可选依赖：PyTorch（如果可用）
try:
    import torch
    import torch.nn as nn
    import torch.optim as optim
    TORCH_AVAILABLE = True
except ImportError:
    TORCH_AVAILABLE = False
    print("警告：PyTorch未安装，PyTorch模型的联邦学习功能将不可用")

# 可选依赖：Scikit-learn（如果可用）
try:
    from sklearn.base import BaseEstimator
    from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False
    print("警告：Scikit-learn未安装，Scikit-learn模型的联邦学习功能将不可用")

# ===================== 错误码定义与异常 =====================

class VMApiErrorCode(Enum):
    SUCCESS = 200
    BAD_REQUEST = 400
    UNAUTHORIZED = 401
    FORBIDDEN = 403
    NOT_FOUND = 404
    CONFLICT = 409
    VALIDATION_FAILED = 422
    SERVER_ERROR = 500
    GATEWAY_ERROR = 502
    SERVICE_UNAVAILABLE = 503
    # 业务错误码
    VM_NOT_FOUND = "VM_NOT_FOUND"
    VM_ALREADY_EXISTS = "VM_ALREADY_EXISTS"
    VM_INVALID_ID = "VM_INVALID_ID"
    VM_INVALID_STATUS = "VM_INVALID_STATUS"
    VM_CONNECTION_FAILED = "VM_CONNECTION_FAILED"
    VM_OPERATION_TIMEOUT = "VM_OPERATION_TIMEOUT"
    VM_INSUFFICIENT_RESOURCES = "VM_INSUFFICIENT_RESOURCES"
    VM_SECURITY_ERROR = "VM_SECURITY_ERROR"

class VMApiError(Exception):
    def __init__(self, code, message, data=None):
        super().__init__(f"[VMApiError {code}] {message}")
        self.code = code
        self.message = message
        self.data = data

def parse_vm_api_response(resp_json):
    code = resp_json.get("code")
    if code != 200:
        raise VMApiError(code, resp_json.get("message"), resp_json.get("data"))
    return resp_json.get("data")

# ===================== 数据结构定义 =====================

@dataclass
# 系统信息
class SystemInfo:
    os: str       #操作系统
    kernel: str   #内核
    python: str   #python版本
    gpu: str      #GPU型号
    cuda: str     #CUDA版本
    cudnn: str    #cuDNN版本

@dataclass
# 能力
class Capabilities:
    supportedAlgorithms: List[str]  #支持的联邦学习算法
    maxBatchSize: int               #最大批量大小
    maxMemoryUsage: int             #最大内存使用量
    gpuMemory: int                  #GPU内存
    networkSpeed: int               #网络速度

@dataclass
# 网络配置
class NetworkConfig:
    uploadSpeed: int     #上传速度
    downloadSpeed: int   #下载速度
    latency: int         #延迟
    bandwidth: int       #带宽

@dataclass
# 安全配置
class SecurityConfig:
    sshKey: str            #SSH密钥
    certificate: Optional[str] = None  #证书
    encryptionEnabled: bool = True    #加密启用
    signatureAlgorithm: str = "RSA-SHA256"  #签名算法

@dataclass
# 元数据
class Metadata:
    description: str     #描述
    location: str        #位置
    owner: str           #所有者
    department: str      #部门
    tags: List[str]      #标签

@dataclass
# 虚拟机注册请求
class VMRegisterRequest:
    vmId: str              #虚拟机ID
    name: str              #名称
    ipAddress: str         #IP地址
    port: int              #端口
    osType: str             #操作系统类型
    cpuCores: int           #CPU核心数
    memoryMb: int           #内存
    diskGb: int             #硬盘
    systemInfo: SystemInfo  #系统信息
    capabilities: Capabilities  #能力
    networkConfig: NetworkConfig  #网络配置
    securityConfig: SecurityConfig  #安全配置
    metadata: Metadata  #元数据

# ===================== API 客户端封装 =====================

class VMApiClient:
    # 初始化
    def __init__(self, base_url: str, jwt_token: str):
        self.base_url = base_url.rstrip("/")
        self.jwt_token = jwt_token

    # 请求头
    def _headers(self):
        return {
            "Authorization": f"Bearer {self.jwt_token}",
            "Content-Type": "application/json"
        }

    # 注册虚拟机
    def register_vm(self, vm: VMRegisterRequest):
        url = f"{self.base_url}/api/v1/vm/register"
        resp = requests.post(url, json=asdict(vm), headers=self._headers())
        return parse_vm_api_response(resp.json())

    # 查询虚拟机列表
    def list_vms(self, page: int = 1, size: int = 20, status: Optional[str] = None, osType: Optional[str] = None, keyword: Optional[str] = None):
        url = f"{self.base_url}/api/v1/vm/list"
        params = {"page": page, "size": size}
        if status:
            params["status"] = status
        if osType:
            params["osType"] = osType
        if keyword:
            params["keyword"] = keyword
        resp = requests.get(url, headers=self._headers(), params=params)
        return parse_vm_api_response(resp.json())

    # 查询虚拟机详情
    def get_vm(self, vmId: str):
        url = f"{self.base_url}/api/v1/vm/{vmId}"
        resp = requests.get(url, headers=self._headers())
        return parse_vm_api_response(resp.json())

    # 更新虚拟机
    def update_vm(self, vmId: str, update_data: dict):
        url = f"{self.base_url}/api/v1/vm/{vmId}"
        resp = requests.put(url, json=update_data, headers=self._headers())
        return parse_vm_api_response(resp.json())

    # 删除虚拟机
    def delete_vm(self, vmId: str, force: bool = False):
        url = f"{self.base_url}/api/v1/vm/{vmId}"
        params = {"force": str(force).lower()}
        resp = requests.delete(url, headers=self._headers(), params=params)
        return parse_vm_api_response(resp.json())

    # 启动虚拟机
    def start_vm(self, vmId: str, config: Optional[dict] = None, environment: Optional[dict] = None, timeout: int = 300):
        url = f"{self.base_url}/api/v1/vm/{vmId}/start"
        data = {"timeout": timeout}
        if config:
            data["config"] = config
        if environment:
            data["environment"] = environment
        resp = requests.post(url, json=data, headers=self._headers())
        return parse_vm_api_response(resp.json())

    # 停止虚拟机
    def stop_vm(self, vmId: str, force: bool = False, timeout: int = 60, saveState: bool = True):
        url = f"{self.base_url}/api/v1/vm/{vmId}/stop"
        data = {"force": force, "timeout": timeout, "saveState": saveState}
        resp = requests.post(url, json=data, headers=self._headers())
        return parse_vm_api_response(resp.json())

    # 重启虚拟机
    def restart_vm(self, vmId: str, config: Optional[dict] = None, graceful: bool = True, timeout: int = 300):
        url = f"{self.base_url}/api/v1/vm/{vmId}/restart"
        data = {"timeout": timeout, "graceful": graceful}
        if config:
            data["config"] = config
        resp = requests.post(url, json=data, headers=self._headers())
        return parse_vm_api_response(resp.json())

    # 查询虚拟机状态
    def get_vm_status(self, vmId: str):
        url = f"{self.base_url}/api/v1/vm/{vmId}/status"
        resp = requests.get(url, headers=self._headers())
        return parse_vm_api_response(resp.json())

    # 刷新 Token
    def refresh_token(self, vm_id: str, secret_id: str):
        url = f"{self.base_url}/api/v1/vm/token/refresh"
        data = {"vmId": vm_id, "secretId": secret_id}
        resp = requests.post(url, json=data, headers=self._headers())
        return parse_vm_api_response(resp.json())

# ===================== 示例用法 =====================

# ===================== 联邦学习核心算法 =====================

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

class ModelWrapper:
    """模型包装器，支持PyTorch和Scikit-learn模型"""
    
    def __init__(self, model: Union[Any, Any], model_type: str = "pytorch"):
        self.model = model
        self.model_type = model_type.lower()
        self.original_state = None
        
        # 检查依赖可用性
        if self.model_type == "pytorch" and not TORCH_AVAILABLE:
            raise ImportError("PyTorch模型需要安装PyTorch库")
        elif self.model_type == "sklearn" and not SKLEARN_AVAILABLE:
            raise ImportError("Scikit-learn模型需要安装scikit-learn库")
        
        if self.model_type == "pytorch":
            self.original_state = copy.deepcopy(model.state_dict())
        elif self.model_type == "sklearn":
            self.original_state = copy.deepcopy(model)
    
    def get_parameters(self) -> Dict[str, Any]:
        """获取模型参数"""
        if self.model_type == "pytorch":
            return {name: param.data.cpu().numpy() for name, param in self.model.named_parameters()}
        elif self.model_type == "sklearn":
            # 对于sklearn模型，获取相关参数
            params = {}
            if hasattr(self.model, 'coef_'):
                params['coef_'] = self.model.coef_
            if hasattr(self.model, 'intercept_'):
                params['intercept_'] = self.model.intercept_
            if hasattr(self.model, 'feature_importances_'):
                params['feature_importances_'] = self.model.feature_importances_
            # 对于RandomForest，获取树的参数
            if hasattr(self.model, 'estimators_'):
                params['n_estimators'] = len(self.model.estimators_)
                # 简化：只存储特征重要性，完整的树结构太大
            return params
        else:
            raise ValueError(f"不支持的模型类型: {self.model_type}")
    
    def set_parameters(self, parameters: Dict[str, Any]) -> bool:
        """设置模型参数"""
        try:
            if self.model_type == "pytorch":
                state_dict = {}
                for name, param in parameters.items():
                    if isinstance(param, np.ndarray):
                        state_dict[name] = torch.tensor(param)
                    else:
                        state_dict[name] = param
                self.model.load_state_dict(state_dict)
                return True
            elif self.model_type == "sklearn":
                # 对于sklearn模型，设置相关参数
                for key, value in parameters.items():
                    if hasattr(self.model, key):
                        setattr(self.model, key, value)
                return True
            return False
        except Exception as e:
            logging.error(f"设置模型参数失败: {e}")
            return False
    
    def get_parameter_count(self) -> int:
        """获取参数数量"""
        if self.model_type == "pytorch":
            return sum(p.numel() for p in self.model.parameters())
        elif self.model_type == "sklearn":
            count = 0
            params = self.get_parameters()
            for key, value in params.items():
                if isinstance(value, np.ndarray):
                    count += value.size
                elif isinstance(value, (int, float)):
                    count += 1
            return count
        return 0

class FederatedAggregator:
    """联邦学习聚合器"""
    
    @staticmethod
    def fedavg_aggregate(client_params: List[Dict[str, Any]], 
                        client_weights: List[float] = None) -> Dict[str, Any]:
        """FedAvg 聚合算法"""
        if not client_params:
            return {}
        
        if client_weights is None:
            client_weights = [1.0 / len(client_params)] * len(client_params)
        
        # 归一化权重
        total_weight = sum(client_weights)
        client_weights = [w / total_weight for w in client_weights]
        
        aggregated_params = {}
        
        # 聚合每个参数
        for param_name in client_params[0].keys():
            weighted_sum = None
            
            for client_param, weight in zip(client_params, client_weights):
                param_value = client_param[param_name]
                
                if isinstance(param_value, np.ndarray):
                    if weighted_sum is None:
                        weighted_sum = weight * param_value
                    else:
                        weighted_sum += weight * param_value
                elif isinstance(param_value, (int, float)):
                    if weighted_sum is None:
                        weighted_sum = weight * param_value
                    else:
                        weighted_sum += weight * param_value
            
            aggregated_params[param_name] = weighted_sum
        
        return aggregated_params
    
    @staticmethod
    def fedprox_aggregate(client_params: List[Dict[str, Any]], 
                         server_params: Dict[str, Any],
                         client_weights: List[float] = None,
                         mu: float = 0.01) -> Dict[str, Any]:
        """FedProx 聚合算法（添加正则化项）"""
        # 基础FedAvg聚合
        aggregated = FederatedAggregator.fedavg_aggregate(client_params, client_weights)
        
        # 添加正则化项
        if server_params:
            for param_name in aggregated.keys():
                if param_name in server_params:
                    server_param = server_params[param_name]
                    aggregated_param = aggregated[param_name]
                    
                    if isinstance(aggregated_param, np.ndarray):
                        # 添加向服务器参数的正则化
                        regularization = mu * (aggregated_param - server_param)
                        aggregated[param_name] = aggregated_param - regularization
        
        return aggregated

class FederatedLearningClient:
    """联邦学习客户端"""
    
    def __init__(self, client_id: str, model: Any, 
                 model_type: str = "pytorch", config: FederatedConfig = None):
        self.client_id = client_id
        self.model_wrapper = ModelWrapper(model, model_type)
        self.config = config or FederatedConfig()
        self.training_state = TrainingState()
        self.local_data = None
        self.local_labels = None
        
        # 设置日志
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(f"FedClient-{client_id}")
        
        # 训练历史
        self.training_history = {
            'rounds': [],
            'local_losses': [],
            'local_accuracies': [],
            'validation_scores': []
        }
    
    def load_local_data(self, X: pd.DataFrame, y: pd.Series) -> bool:
        """加载本地训练数据"""
        try:
            self.local_data = X
            self.local_labels = y
            self.training_state.total_samples = len(X)
            self.logger.info(f"加载本地数据: {len(X)} 样本, {len(X.columns)} 特征")
            return True
        except Exception as e:
            self.logger.error(f"加载本地数据失败: {e}")
            return False
    
    def local_train(self, global_params: Dict[str, Any] = None, 
                   validation_data: tuple = None) -> Dict[str, Any]:
        """本地训练"""
        if self.local_data is None or self.local_labels is None:
            raise ValueError("本地数据未加载")
        
        self.training_state.is_training = True
        self.training_state.error_message = ""
        
        try:
            # 设置全局参数
            if global_params:
                self.model_wrapper.set_parameters(global_params)
            
            # 执行本地训练
            if self.model_wrapper.model_type == "pytorch":
                training_result = self._train_pytorch_model(validation_data)
            elif self.model_wrapper.model_type == "sklearn":
                training_result = self._train_sklearn_model(validation_data)
            else:
                raise ValueError(f"不支持的模型类型: {self.model_wrapper.model_type}")
            
            # 更新训练状态
            self.training_state.round_num += 1
            self.training_state.local_epochs_completed += self.config.local_epochs
            self.training_state.last_update_time = time.time()
            
            # 记录训练历史
            self.training_history['rounds'].append(self.training_state.round_num)
            self.training_history['local_losses'].append(training_result.get('final_loss', 0))
            self.training_history['local_accuracies'].append(training_result.get('final_accuracy', 0))
            
            if validation_data:
                val_score = self._evaluate_model(validation_data[0], validation_data[1])
                self.training_history['validation_scores'].append(val_score)
            
            self.logger.info(f"本地训练完成 - 轮次: {self.training_state.round_num}, "
                           f"损失: {training_result.get('final_loss', 0):.4f}")
            
            return {
                'client_id': self.client_id,
                'parameters': self.model_wrapper.get_parameters(),
                'num_samples': self.training_state.total_samples,
                'training_loss': training_result.get('final_loss', 0),
                'training_accuracy': training_result.get('final_accuracy', 0),
                'round_num': self.training_state.round_num
            }
            
        except Exception as e:
            self.training_state.error_message = str(e)
            self.logger.error(f"本地训练失败: {e}")
            raise
        finally:
            self.training_state.is_training = False
    
    def _train_pytorch_model(self, validation_data: tuple = None) -> Dict[str, Any]:
        """训练PyTorch模型"""
        model = self.model_wrapper.model
        model.train()
        
        # 确定设备（GPU或CPU）
        device = next(model.parameters()).device
        
        # 准备数据并移到相同设备
        X_tensor = torch.FloatTensor(self.local_data.values).to(device)
        y_tensor = torch.FloatTensor(self.local_labels.values).to(device)
        
        # 创建数据加载器
        dataset = torch.utils.data.TensorDataset(X_tensor, y_tensor)
        dataloader = torch.utils.data.DataLoader(
            dataset, batch_size=self.config.local_batch_size, shuffle=True
        )
        
        # 优化器
        optimizer = optim.SGD(model.parameters(), lr=self.config.learning_rate)
        criterion = nn.MSELoss()  # 根据任务类型调整
        
        epoch_losses = []
        
        # 本地训练循环
        for epoch in range(self.config.local_epochs):
            epoch_loss = 0.0
            batch_count = 0
            
            for batch_X, batch_y in dataloader:
                optimizer.zero_grad()
                
                outputs = model(batch_X)
                loss = criterion(outputs.squeeze(), batch_y)
                
                # FedProx正则化项
                if self.config.algorithm == FederatedAlgorithm.FEDPROX:
                    prox_term = 0.0
                    for param in model.parameters():
                        prox_term += torch.norm(param) ** 2
                    loss += (self.config.mu / 2) * prox_term
                
                loss.backward()
                optimizer.step()
                
                epoch_loss += loss.item()
                batch_count += 1
            
            avg_epoch_loss = epoch_loss / batch_count if batch_count > 0 else 0
            epoch_losses.append(avg_epoch_loss)
            
            self.logger.debug(f"Epoch {epoch + 1}/{self.config.local_epochs}, 损失: {avg_epoch_loss:.4f}")
        
        # 计算最终准确率（对于回归任务，这里用R²代替）
        with torch.no_grad():
            model.eval()
            all_outputs = model(X_tensor)
            final_loss = criterion(all_outputs.squeeze(), y_tensor).item()
            
            # 简单的R²计算
            y_mean = torch.mean(y_tensor)
            ss_tot = torch.sum((y_tensor - y_mean) ** 2)
            ss_res = torch.sum((y_tensor - all_outputs.squeeze()) ** 2)
            r2_score = 1 - (ss_res / ss_tot) if ss_tot != 0 else 0
            final_accuracy = r2_score.item()
        
        return {
            'epoch_losses': epoch_losses,
            'final_loss': final_loss,
            'final_accuracy': final_accuracy
        }
    
    def _train_sklearn_model(self, validation_data: tuple = None) -> Dict[str, Any]:
        """训练Scikit-learn模型"""
        model = self.model_wrapper.model
        
        # 对于sklearn模型，执行多次拟合来模拟多个epoch
        losses = []
        accuracies = []
        
        for epoch in range(self.config.local_epochs):
            # 重新拟合模型
            if hasattr(model, 'partial_fit'):
                # 支持增量学习的模型
                model.partial_fit(self.local_data.values, self.local_labels.values)
            else:
                # 重新训练模型
                model.fit(self.local_data.values, self.local_labels.values)
            
            # 计算训练指标
            try:
                predictions = model.predict(self.local_data.values)
                
                if hasattr(model, 'predict_proba'):
                    # 分类任务
                    from sklearn.metrics import accuracy_score, log_loss
                    accuracy = accuracy_score(self.local_labels.values, predictions)
                    # 简化的损失计算
                    loss = 1 - accuracy
                else:
                    # 回归任务
                    from sklearn.metrics import mean_squared_error, r2_score
                    loss = mean_squared_error(self.local_labels.values, predictions)
                    accuracy = r2_score(self.local_labels.values, predictions)
                
                losses.append(loss)
                accuracies.append(accuracy)
                
            except Exception as e:
                self.logger.warning(f"计算训练指标失败: {e}")
                losses.append(0)
                accuracies.append(0)
        
        return {
            'epoch_losses': losses,
            'final_loss': losses[-1] if losses else 0,
            'final_accuracy': accuracies[-1] if accuracies else 0
        }
    
    def _evaluate_model(self, X_val: pd.DataFrame, y_val: pd.Series) -> float:
        """评估模型"""
        try:
            if self.model_wrapper.model_type == "pytorch":
                model = self.model_wrapper.model
                model.eval()
                
                # 确定设备
                device = next(model.parameters()).device
                
                with torch.no_grad():
                    X_tensor = torch.FloatTensor(X_val.values).to(device)
                    y_tensor = torch.FloatTensor(y_val.values).to(device)
                    outputs = model(X_tensor)
                    
                    # 计算R²分数
                    y_mean = torch.mean(y_tensor)
                    ss_tot = torch.sum((y_tensor - y_mean) ** 2)
                    ss_res = torch.sum((y_tensor - outputs.squeeze()) ** 2)
                    r2_score = 1 - (ss_res / ss_tot) if ss_tot != 0 else 0
                    return r2_score.item()
            
            elif self.model_wrapper.model_type == "sklearn":
                model = self.model_wrapper.model
                return model.score(X_val.values, y_val.values)
                
        except Exception as e:
            self.logger.warning(f"模型评估失败: {e}")
            return 0.0
    
    def get_training_status(self) -> Dict[str, Any]:
        """获取训练状态"""
        return {
            'client_id': self.client_id,
            'is_training': self.training_state.is_training,
            'round_num': self.training_state.round_num,
            'local_epochs_completed': self.training_state.local_epochs_completed,
            'total_samples': self.training_state.total_samples,
            'last_update_time': self.training_state.last_update_time,
            'error_message': self.training_state.error_message,
            'parameter_count': self.model_wrapper.get_parameter_count()
        }
    
    def save_checkpoint(self, filepath: str) -> bool:
        """保存检查点"""
        try:
            checkpoint = {
                'client_id': self.client_id,
                'model_parameters': self.model_wrapper.get_parameters(),
                'model_type': self.model_wrapper.model_type,
                'training_state': asdict(self.training_state),
                'config': asdict(self.config),
                'training_history': self.training_history
            }
            
            Path(filepath).parent.mkdir(parents=True, exist_ok=True)
            
            if self.model_wrapper.model_type == "pytorch":
                torch.save(checkpoint, filepath)
            else:
                joblib.dump(checkpoint, filepath)
            
            self.logger.info(f"检查点已保存: {filepath}")
            return True
            
        except Exception as e:
            self.logger.error(f"保存检查点失败: {e}")
            return False
    
    def load_checkpoint(self, filepath: str) -> bool:
        """加载检查点"""
        try:
            if self.model_wrapper.model_type == "pytorch":
                checkpoint = torch.load(filepath)
            else:
                checkpoint = joblib.load(filepath)
            
            # 恢复训练状态
            self.training_state = TrainingState(**checkpoint['training_state'])
            self.training_history = checkpoint['training_history']
            
            # 恢复模型参数
            self.model_wrapper.set_parameters(checkpoint['model_parameters'])
            
            self.logger.info(f"检查点已加载: {filepath}")
            return True
            
        except Exception as e:
            self.logger.error(f"加载检查点失败: {e}")
            return False

class FederatedLearningCoordinator:
    """联邦学习协调器（服务器端逻辑）"""
    
    def __init__(self, config: FederatedConfig = None):
        self.config = config or FederatedConfig()
        self.global_model_params = None
        self.participating_clients = {}
        self.round_history = []
        
        # 设置日志
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger("FedCoordinator")
        
        # 早停机制
        self.best_global_loss = float('inf')
        self.patience_counter = 0
    
    def register_client(self, client: FederatedLearningClient) -> bool:
        """注册客户端"""
        try:
            self.participating_clients[client.client_id] = client
            self.logger.info(f"客户端已注册: {client.client_id}")
            return True
        except Exception as e:
            self.logger.error(f"注册客户端失败: {e}")
            return False
    
    def start_federated_round(self, round_num: int) -> Dict[str, Any]:
        """开始联邦学习轮次"""
        self.logger.info(f"开始第 {round_num} 轮联邦学习")
        
        if not self.participating_clients:
            raise ValueError("没有注册的客户端")
        
        # 选择参与的客户端
        num_clients = max(1, int(len(self.participating_clients) * self.config.client_fraction))
        selected_clients = list(self.participating_clients.values())[:num_clients]
        
        client_results = []
        client_weights = []
        
        # 并行执行客户端训练（简化版，实际应用中可以使用多线程）
        for client in selected_clients:
            try:
                result = client.local_train(
                    global_params=self.global_model_params,
                    validation_data=None  # 可以传入验证数据
                )
                client_results.append(result)
                client_weights.append(result['num_samples'])
                
            except Exception as e:
                self.logger.error(f"客户端 {client.client_id} 训练失败: {e}")
                continue
        
        if not client_results:
            raise RuntimeError("所有客户端训练都失败了")
        
        # 聚合模型参数
        client_params = [result['parameters'] for result in client_results]
        
        if self.config.algorithm == FederatedAlgorithm.FEDAVG:
            self.global_model_params = FederatedAggregator.fedavg_aggregate(
                client_params, client_weights
            )
        elif self.config.algorithm == FederatedAlgorithm.FEDPROX:
            self.global_model_params = FederatedAggregator.fedprox_aggregate(
                client_params, self.global_model_params, client_weights, self.config.mu
            )
        else:
            raise ValueError(f"不支持的联邦学习算法: {self.config.algorithm}")
        
        # 计算全局指标
        avg_loss = np.mean([result['training_loss'] for result in client_results])
        avg_accuracy = np.mean([result['training_accuracy'] for result in client_results])
        total_samples = sum([result['num_samples'] for result in client_results])
        
        round_result = {
            'round_num': round_num,
            'participating_clients': len(client_results),
            'total_samples': total_samples,
            'avg_training_loss': avg_loss,
            'avg_training_accuracy': avg_accuracy,
            'algorithm': self.config.algorithm.value,
            'convergence_status': self._check_convergence(avg_loss)
        }
        
        self.round_history.append(round_result)
        
        self.logger.info(f"第 {round_num} 轮完成 - 平均损失: {avg_loss:.4f}, "
                        f"平均准确率: {avg_accuracy:.4f}")
        
        return round_result
    
    def _check_convergence(self, current_loss: float) -> str:
        """检查收敛状态"""
        if current_loss < self.best_global_loss - self.config.min_delta:
            self.best_global_loss = current_loss
            self.patience_counter = 0
            return "improving"
        else:
            self.patience_counter += 1
            if self.patience_counter >= self.config.patience:
                return "converged"
            else:
                return "stable"
    
    def run_federated_training(self) -> Dict[str, Any]:
        """运行完整的联邦学习训练"""
        self.logger.info("开始联邦学习训练")
        
        training_start_time = time.time()
        
        for round_num in range(1, self.config.max_rounds + 1):
            try:
                round_result = self.start_federated_round(round_num)
                
                # 检查早停条件
                if round_result['convergence_status'] == 'converged':
                    self.logger.info(f"训练在第 {round_num} 轮收敛")
                    break
                    
            except Exception as e:
                self.logger.error(f"第 {round_num} 轮训练失败: {e}")
                break
        
        training_time = time.time() - training_start_time
        
        final_result = {
            'total_rounds': len(self.round_history),
            'training_time': training_time,
            'final_loss': self.round_history[-1]['avg_training_loss'] if self.round_history else 0,
            'final_accuracy': self.round_history[-1]['avg_training_accuracy'] if self.round_history else 0,
            'convergence_achieved': self.round_history[-1]['convergence_status'] == 'converged' if self.round_history else False,
            'round_history': self.round_history
        }
        
        self.logger.info(f"联邦学习训练完成 - 总轮次: {final_result['total_rounds']}, "
                        f"最终损失: {final_result['final_loss']:.4f}")
        
        return final_result
    
    def get_global_model_params(self) -> Dict[str, Any]:
        """获取全局模型参数"""
        return self.global_model_params
    
    def save_global_model(self, filepath: str) -> bool:
        """保存全局模型"""
        try:
            model_data = {
                'global_parameters': self.global_model_params,
                'config': asdict(self.config),
                'round_history': self.round_history,
                'training_completed': True
            }
            
            Path(filepath).parent.mkdir(parents=True, exist_ok=True)
            joblib.dump(model_data, filepath)
            
            self.logger.info(f"全局模型已保存: {filepath}")
            return True
            
        except Exception as e:
            self.logger.error(f"保存全局模型失败: {e}")
            return False

# ===================== 使用示例和测试 =====================

def create_simple_pytorch_model(input_size: int, output_size: int = 1) -> Any:
    """创建简单的PyTorch模型用于测试"""
    if not TORCH_AVAILABLE:
        raise ImportError("PyTorch不可用，无法创建PyTorch模型")
    
    class SimpleModel(nn.Module):
        def __init__(self, input_size, output_size):
            super(SimpleModel, self).__init__()
            self.fc1 = nn.Linear(input_size, 64)
            self.fc2 = nn.Linear(64, 32)
            self.fc3 = nn.Linear(32, output_size)
            self.relu = nn.ReLU()
            
        def forward(self, x):
            x = self.relu(self.fc1(x))
            x = self.relu(self.fc2(x))
            x = self.fc3(x)
            return x
    
    return SimpleModel(input_size, output_size)

def create_sample_data(n_samples: int = 1000, n_features: int = 20, n_clients: int = 3) -> List[Tuple[pd.DataFrame, pd.Series]]:
    """创建用于联邦学习测试的样本数据"""
    np.random.seed(42)
    
    # 生成基础数据
    X = np.random.randn(n_samples, n_features)
    # 简单的线性关系加噪声
    true_weights = np.random.randn(n_features)
    y = X @ true_weights + 0.1 * np.random.randn(n_samples)
    
    # 将数据分配给不同客户端（非独立同分布）
    client_data = []
    samples_per_client = n_samples // n_clients
    
    for i in range(n_clients):
        start_idx = i * samples_per_client
        end_idx = start_idx + samples_per_client if i < n_clients - 1 else n_samples
        
        # 每个客户端的数据有不同的分布偏差
        client_X = X[start_idx:end_idx] + i * 0.2 * np.random.randn(end_idx - start_idx, n_features)
        client_y = y[start_idx:end_idx] + i * 0.1 * np.random.randn(end_idx - start_idx)
        
        client_data.append((
            pd.DataFrame(client_X, columns=[f'feature_{j}' for j in range(n_features)]),
            pd.Series(client_y, name='target')
        ))
    
    return client_data

def test_federated_learning_pytorch():
    """测试PyTorch模型的联邦学习"""
    print("测试PyTorch联邦学习...")
    
    if not TORCH_AVAILABLE:
        print("跳过PyTorch测试：PyTorch未安装")
        return False
    
    # 创建数据
    client_data = create_sample_data(n_samples=300, n_features=10, n_clients=3)
    
    # 创建联邦学习配置
    config = FederatedConfig(
        algorithm=FederatedAlgorithm.FEDAVG,
        local_epochs=3,
        local_batch_size=16,
        learning_rate=0.01,
        max_rounds=5
    )
    
    # 创建客户端
    clients = []
    for i, (X, y) in enumerate(client_data):
        model = create_simple_pytorch_model(input_size=10)
        client = FederatedLearningClient(
            client_id=f"pytorch_client_{i}",
            model=model,
            model_type="pytorch",
            config=config
        )
        client.load_local_data(X, y)
        clients.append(client)
    
    # 创建协调器
    coordinator = FederatedLearningCoordinator(config)
    
    # 注册客户端
    for client in clients:
        coordinator.register_client(client)
    
    # 运行联邦学习
    try:
        result = coordinator.run_federated_training()
        print(f"PyTorch联邦学习完成：")
        print(f"  轮次数：{result['total_rounds']}")
        print(f"  训练时间：{result['training_time']:.2f}秒")
        print(f"  最终损失：{result['final_loss']:.4f}")
        print(f"  最终准确率：{result['final_accuracy']:.4f}")
        
        # 保存全局模型
        coordinator.save_global_model("models/federated_pytorch_model.joblib")
        
        return True
        
    except Exception as e:
        print(f"PyTorch联邦学习测试失败：{e}")
        return False

def test_federated_learning_sklearn():
    """测试Scikit-learn模型的联邦学习"""
    print("测试Scikit-learn联邦学习...")
    
    if not SKLEARN_AVAILABLE:
        print("跳过Scikit-learn测试：scikit-learn未安装")
        return False
    
    # 创建数据
    client_data = create_sample_data(n_samples=300, n_features=10, n_clients=3)
    
    # 创建联邦学习配置
    config = FederatedConfig(
        algorithm=FederatedAlgorithm.FEDAVG,
        local_epochs=3,
        max_rounds=5
    )
    
    # 创建客户端
    clients = []
    for i, (X, y) in enumerate(client_data):
        from sklearn.linear_model import LinearRegression
        model = LinearRegression()
        client = FederatedLearningClient(
            client_id=f"sklearn_client_{i}",
            model=model,
            model_type="sklearn",
            config=config
        )
        client.load_local_data(X, y)
        clients.append(client)
    
    # 创建协调器
    coordinator = FederatedLearningCoordinator(config)
    
    # 注册客户端
    for client in clients:
        coordinator.register_client(client)
    
    # 运行联邦学习
    try:
        result = coordinator.run_federated_training()
        print(f"Scikit-learn联邦学习完成：")
        print(f"  轮次数：{result['total_rounds']}")
        print(f"  训练时间：{result['training_time']:.2f}秒")
        print(f"  最终损失：{result['final_loss']:.4f}")
        print(f"  最终准确率：{result['final_accuracy']:.4f}")
        
        # 保存全局模型
        coordinator.save_global_model("models/federated_sklearn_model.joblib")
        
        return True
        
    except Exception as e:
        print(f"Scikit-learn联邦学习测试失败：{e}")
        return False

if __name__ == "__main__":
    print("联邦学习框架测试开始...")
    
    # 创建模型目录
    Path("models").mkdir(exist_ok=True)
    
    # 测试PyTorch联邦学习
    print("\n" + "="*50)
    pytorch_success = test_federated_learning_pytorch()
    
    # 测试Scikit-learn联邦学习
    print("\n" + "="*50)
    sklearn_success = test_federated_learning_sklearn()
    
    # 总结
    print("\n" + "="*50)
    print("测试结果总结：")
    print(f"  PyTorch联邦学习：{'✅ 成功' if pytorch_success else '❌ 失败'}")
    print(f"  Scikit-learn联邦学习：{'✅ 成功' if sklearn_success else '❌ 失败'}")
    
    if pytorch_success and sklearn_success:
        print("\n🎉 联邦学习框架测试全部通过！")
    else:
        print("\n⚠️  部分测试失败，请检查错误信息。")
