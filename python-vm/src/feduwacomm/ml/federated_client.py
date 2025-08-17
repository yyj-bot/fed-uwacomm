"""
联邦学习客户端

注意：本文件仅用于 HTTP API 交互，不包含 WebSocket 连接支持。WebSocket 连接应在其他模块中实现。
"""

from dataclasses import dataclass, field, asdict
from typing import List, Dict, Optional
import requests
from enum import Enum

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

if __name__ == "__main__":
    # 示例：构造注册请求
    system_info = SystemInfo(
        os="Ubuntu 20.04 LTS",
        kernel="5.4.0-42-generic",
        python="3.8.10",
        gpu="NVIDIA Tesla V100",
        cuda="11.0",
        cudnn="8.0.5"
    )
    capabilities = Capabilities(
        supportedAlgorithms=["FEDAVG", "FEDPROX", "FEDNOVA", "SCAFFOLD"],
        maxBatchSize=128,
        maxMemoryUsage=6144,
        gpuMemory=16384,
        networkSpeed=1000
    )
    network_config = NetworkConfig(
        uploadSpeed=100,
        downloadSpeed=200,
        latency=50,
        bandwidth=1000
    )
    security_config = SecurityConfig(
        sshKey="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC...",
        certificate=None,
        encryptionEnabled=True,
        signatureAlgorithm="RSA-SHA256"
    )
    metadata = Metadata(
        description="水声联邦学习专用虚拟机节点",
        location="实验室A-机架01",
        owner="张三",
        department="水声工程学院",
        tags=["水声", "联邦学习", "GPU节点"]
    )
    vm_req = VMRegisterRequest(
        vmId="a1b2c3d4e5f678901234567890123456",
        name="水声联邦学习节点-001",
        ipAddress="192.168.1.100",
        port=22,
        osType="Ubuntu 20.04",
        cpuCores=4,
        memoryMb=8192,
        diskGb=100,
        systemInfo=system_info,
        capabilities=capabilities,
        networkConfig=network_config,
        securityConfig=security_config,
        metadata=metadata
    )

    # API客户端示例
    client = VMApiClient(base_url="http://localhost:8080", jwt_token="your_jwt_token")
    try:
        # 注册虚拟机
        print(client.register_vm(vm_req))
        # 查询虚拟机列表
        print(client.list_vms())
        # 查询虚拟机详情
        print(client.get_vm(vm_req.vmId))
        # 启动虚拟机
        print(client.start_vm(vm_req.vmId))
        # 停止虚拟机
        print(client.stop_vm(vm_req.vmId))
        # 重启虚拟机
        print(client.restart_vm(vm_req.vmId))
        # 查询虚拟机状态
        print(client.get_vm_status(vm_req.vmId))
    except VMApiError as e:
        print(f"API调用出错: {e}")
        if e.data:
            print(f"详细信息: {e.data}")
