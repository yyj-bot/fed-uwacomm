"""
系统监控工具

提供系统资源监控、性能统计等功能，用于WebSocket状态查询和心跳消息。
"""

import time
import platform
import logging
from typing import Dict, Any, List
from datetime import datetime

# 可选依赖：psutil
try:
    import psutil
    PSUTIL_AVAILABLE = True
except ImportError:
    PSUTIL_AVAILABLE = False


class SystemMonitor:
    """系统监控器"""
    
    def __init__(self):
        """初始化系统监控器"""
        self.logger = logging.getLogger("SystemMonitor")
        
        if not PSUTIL_AVAILABLE:
            self.logger.warning("psutil未安装，系统监控功能受限")
        
        # 缓存系统信息
        self._system_info_cache = None
        self._cache_time = 0
        self._cache_duration = 300  # 缓存5分钟
    
    def get_resource_usage(self) -> Dict[str, float]:
        """获取系统资源使用情况
        
        Returns:
            dict: 包含CPU、内存、磁盘、GPU使用率的字典
        """
        if not PSUTIL_AVAILABLE:
            return {"cpu": 0.0, "memory": 0.0, "disk": 0.0, "gpu": 0.0}
        
        try:
            return {
                "cpu": round(psutil.cpu_percent(interval=0.1), 2),
                "memory": round(psutil.virtual_memory().percent, 2),
                "disk": round(psutil.disk_usage('/').percent, 2) if platform.system() != "Windows" 
                       else round(psutil.disk_usage('C:\\').percent, 2),
                "gpu": self._get_gpu_usage()
            }
        except Exception as e:
            self.logger.error(f"获取资源使用情况失败: {e}")
            return {"cpu": 0.0, "memory": 0.0, "disk": 0.0, "gpu": 0.0}
    
    def get_memory_info(self) -> Dict[str, Any]:
        """获取详细内存信息
        
        Returns:
            dict: 内存详细信息
        """
        if not PSUTIL_AVAILABLE:
            return {"total": 0, "available": 0, "used": 0, "percent": 0.0}
        
        try:
            mem = psutil.virtual_memory()
            return {
                "total": mem.total,
                "available": mem.available,
                "used": mem.used,
                "percent": round(mem.percent, 2)
            }
        except Exception as e:
            self.logger.error(f"获取内存信息失败: {e}")
            return {"total": 0, "available": 0, "used": 0, "percent": 0.0}
    
    def get_cpu_info(self) -> Dict[str, Any]:
        """获取CPU信息
        
        Returns:
            dict: CPU详细信息
        """
        try:
            cpu_info = {
                "count": psutil.cpu_count() if PSUTIL_AVAILABLE else 1,
                "usage": round(psutil.cpu_percent(interval=0.1), 2) if PSUTIL_AVAILABLE else 0.0,
                "frequency": self._get_cpu_frequency(),
                "architecture": platform.machine(),
                "processor": platform.processor()
            }
            
            if PSUTIL_AVAILABLE:
                cpu_info["per_cpu"] = [round(x, 2) for x in psutil.cpu_percent(interval=0.1, percpu=True)]
            
            return cpu_info
            
        except Exception as e:
            self.logger.error(f"获取CPU信息失败: {e}")
            return {"count": 1, "usage": 0.0, "frequency": 0, "architecture": "unknown", "processor": "unknown"}
    
    def get_disk_info(self) -> List[Dict[str, Any]]:
        """获取磁盘信息
        
        Returns:
            list: 磁盘信息列表
        """
        if not PSUTIL_AVAILABLE:
            return []
        
        try:
            disk_info = []
            partitions = psutil.disk_partitions()
            
            for partition in partitions:
                try:
                    usage = psutil.disk_usage(partition.mountpoint)
                    disk_info.append({
                        "device": partition.device,
                        "mountpoint": partition.mountpoint,
                        "fstype": partition.fstype,
                        "total": usage.total,
                        "used": usage.used,
                        "free": usage.free,
                        "percent": round(usage.percent, 2)
                    })
                except PermissionError:
                    # 某些分区可能没有访问权限
                    continue
            
            return disk_info
            
        except Exception as e:
            self.logger.error(f"获取磁盘信息失败: {e}")
            return []
    
    def get_network_info(self) -> Dict[str, Any]:
        """获取网络信息
        
        Returns:
            dict: 网络信息
        """
        if not PSUTIL_AVAILABLE:
            return {"interfaces": [], "io_counters": {}}
        
        try:
            network_info = {
                "interfaces": [],
                "io_counters": {}
            }
            
            # 获取网络接口信息
            interfaces = psutil.net_if_addrs()
            for interface, addrs in interfaces.items():
                interface_info = {
                    "name": interface,
                    "addresses": []
                }
                
                for addr in addrs:
                    interface_info["addresses"].append({
                        "family": str(addr.family),
                        "address": addr.address,
                        "netmask": addr.netmask,
                        "broadcast": addr.broadcast
                    })
                
                network_info["interfaces"].append(interface_info)
            
            # 获取网络IO统计
            io_counters = psutil.net_io_counters()
            if io_counters:
                network_info["io_counters"] = {
                    "bytes_sent": io_counters.bytes_sent,
                    "bytes_recv": io_counters.bytes_recv,
                    "packets_sent": io_counters.packets_sent,
                    "packets_recv": io_counters.packets_recv
                }
            
            return network_info
            
        except Exception as e:
            self.logger.error(f"获取网络信息失败: {e}")
            return {"interfaces": [], "io_counters": {}}
    
    def get_process_info(self) -> Dict[str, Any]:
        """获取进程信息
        
        Returns:
            dict: 进程统计信息
        """
        if not PSUTIL_AVAILABLE:
            return {"total": 0, "running": 0, "sleeping": 0, "zombie": 0}
        
        try:
            processes = list(psutil.process_iter(['pid', 'name', 'status']))
            status_count = {}
            
            for proc in processes:
                status = proc.info['status']
                status_count[status] = status_count.get(status, 0) + 1
            
            return {
                "total": len(processes),
                "running": status_count.get(psutil.STATUS_RUNNING, 0),
                "sleeping": status_count.get(psutil.STATUS_SLEEPING, 0),
                "zombie": status_count.get(psutil.STATUS_ZOMBIE, 0),
                "status_breakdown": status_count
            }
            
        except Exception as e:
            self.logger.error(f"获取进程信息失败: {e}")
            return {"total": 0, "running": 0, "sleeping": 0, "zombie": 0}
    
    def get_system_info(self) -> Dict[str, Any]:
        """获取系统基本信息
        
        Returns:
            dict: 系统信息
        """
        current_time = time.time()
        
        # 使用缓存避免频繁获取系统信息
        if (self._system_info_cache and 
            current_time - self._cache_time < self._cache_duration):
            return self._system_info_cache
        
        try:
            system_info = {
                "system": platform.system(),
                "release": platform.release(),
                "version": platform.version(),
                "machine": platform.machine(),
                "processor": platform.processor(),
                "python_version": platform.python_version(),
                "hostname": platform.node(),
                "uptime": self._get_uptime(),
                "boot_time": self._get_boot_time()
            }
            
            # 更新缓存
            self._system_info_cache = system_info
            self._cache_time = current_time
            
            return system_info
            
        except Exception as e:
            self.logger.error(f"获取系统信息失败: {e}")
            return {
                "system": "Unknown",
                "release": "Unknown",
                "version": "Unknown",
                "machine": "Unknown",
                "processor": "Unknown",
                "python_version": platform.python_version(),
                "hostname": "Unknown",
                "uptime": 0,
                "boot_time": "Unknown"
            }
    
    def get_full_status(self) -> Dict[str, Any]:
        """获取完整的系统状态信息
        
        Returns:
            dict: 完整状态信息
        """
        return {
            "timestamp": datetime.now().isoformat(),
            "system_info": self.get_system_info(),
            "resource_usage": self.get_resource_usage(),
            "memory_info": self.get_memory_info(),
            "cpu_info": self.get_cpu_info(),
            "disk_info": self.get_disk_info(),
            "network_info": self.get_network_info(),
            "process_info": self.get_process_info()
        }
    
    def _get_gpu_usage(self) -> float:
        """获取GPU使用率
        
        Returns:
            float: GPU使用率百分比
        """
        # TODO: 实现GPU使用率获取
        # 可以使用nvidia-ml-py3库或其他GPU监控工具
        return 0.0
    
    def _get_cpu_frequency(self) -> Dict[str, float]:
        """获取CPU频率信息
        
        Returns:
            dict: CPU频率信息
        """
        if not PSUTIL_AVAILABLE:
            return {"current": 0.0, "min": 0.0, "max": 0.0}
        
        try:
            freq = psutil.cpu_freq()
            if freq:
                return {
                    "current": round(freq.current, 2),
                    "min": round(freq.min, 2),
                    "max": round(freq.max, 2)
                }
        except Exception:
            pass
        
        return {"current": 0.0, "min": 0.0, "max": 0.0}
    
    def _get_uptime(self) -> float:
        """获取系统运行时间（秒）
        
        Returns:
            float: 运行时间
        """
        if not PSUTIL_AVAILABLE:
            return 0.0
        
        try:
            return time.time() - psutil.boot_time()
        except Exception:
            return 0.0
    
    def _get_boot_time(self) -> str:
        """获取系统启动时间
        
        Returns:
            str: 启动时间的ISO格式字符串
        """
        if not PSUTIL_AVAILABLE:
            return "Unknown"
        
        try:
            boot_time = psutil.boot_time()
            return datetime.fromtimestamp(boot_time).isoformat()
        except Exception:
            return "Unknown"
