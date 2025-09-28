#!/usr/bin/env python3
"""
健康检查脚本
用于监控Python VM服务的健康状态
"""

import sys
import json
import time
import argparse
import requests
import subprocess
from pathlib import Path
from typing import Dict, Any, List
import logging

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s] %(message)s')
logger = logging.getLogger(__name__)


class HealthChecker:
    """健康检查器"""
    
    def __init__(self, config: Dict[str, Any]):
        """
        初始化健康检查器
        
        Args:
            config: 检查配置
        """
        self.config = config
        self.checks = []
        self.results = {}
        
    def add_check(self, name: str, check_func: callable, **kwargs):
        """添加检查项"""
        self.checks.append({
            'name': name,
            'func': check_func,
            'kwargs': kwargs
        })
    
    def run_all_checks(self) -> Dict[str, Any]:
        """运行所有检查"""
        logger.info("开始健康检查...")
        
        overall_status = 'HEALTHY'
        check_results = {}
        
        for check in self.checks:
            try:
                logger.debug(f"执行检查: {check['name']}")
                result = check['func'](**check['kwargs'])
                check_results[check['name']] = result
                
                # 更新整体状态
                if result.get('status') == 'CRITICAL':
                    overall_status = 'CRITICAL'
                elif result.get('status') == 'WARNING' and overall_status != 'CRITICAL':
                    overall_status = 'WARNING'
                    
            except Exception as e:
                logger.error(f"检查 {check['name']} 失败: {e}")
                check_results[check['name']] = {
                    'status': 'ERROR',
                    'error': str(e),
                    'timestamp': time.time()
                }
                overall_status = 'CRITICAL'
        
        self.results = {
            'overall_status': overall_status,
            'timestamp': time.time(),
            'checks': check_results
        }
        
        logger.info(f"健康检查完成，状态: {overall_status}")
        return self.results
    
    def get_exit_code(self) -> int:
        """获取退出码"""
        status = self.results.get('overall_status', 'ERROR')
        if status == 'HEALTHY':
            return 0
        elif status == 'WARNING':
            return 1
        else:  # CRITICAL or ERROR
            return 2


def check_service_process(pid_file: str = '/var/run/feduwa-vm.pid') -> Dict[str, Any]:
    """检查服务进程"""
    result = {
        'status': 'HEALTHY',
        'timestamp': time.time(),
        'details': {}
    }
    
    try:
        pid_path = Path(pid_file)
        
        if not pid_path.exists():
            result['status'] = 'CRITICAL'
            result['details']['error'] = 'PID文件不存在'
            return result
        
        with open(pid_path, 'r') as f:
            pid = int(f.read().strip())
        
        result['details']['pid'] = pid
        
        # 检查进程是否存在
        try:
            import os
            os.kill(pid, 0)
            result['details']['process_exists'] = True
        except OSError:
            result['status'] = 'CRITICAL'
            result['details']['process_exists'] = False
            result['details']['error'] = '进程不存在'
        
    except Exception as e:
        result['status'] = 'ERROR'
        result['details']['error'] = str(e)
    
    return result


def check_websocket_connection(server_url: str, timeout: int = 10) -> Dict[str, Any]:
    """检查WebSocket连接"""
    result = {
        'status': 'HEALTHY',
        'timestamp': time.time(),
        'details': {}
    }
    
    try:
        import websockets
        import asyncio
        
        async def test_connection():
            try:
                async with websockets.connect(server_url, timeout=timeout) as websocket:
                    # 发送简单的ping消息
                    await websocket.send('{"type": "ping"}')
                    response = await websocket.recv()
                    return True, response
            except Exception as e:
                return False, str(e)
        
        success, response = asyncio.run(test_connection())
        
        if success:
            result['details']['connection'] = 'OK'
            result['details']['response'] = response[:100] if response else None
        else:
            result['status'] = 'CRITICAL'
            result['details']['error'] = response
            
    except ImportError:
        result['status'] = 'WARNING'
        result['details']['error'] = 'websockets模块未安装，跳过连接检查'
    except Exception as e:
        result['status'] = 'ERROR'
        result['details']['error'] = str(e)
    
    return result


def check_system_resources(
    cpu_threshold: float = 90.0,
    memory_threshold: float = 90.0,
    disk_threshold: float = 95.0
) -> Dict[str, Any]:
    """检查系统资源"""
    result = {
        'status': 'HEALTHY',
        'timestamp': time.time(),
        'details': {}
    }
    
    try:
        import psutil
        
        # CPU使用率
        cpu_percent = psutil.cpu_percent(interval=1)
        result['details']['cpu_percent'] = cpu_percent
        
        if cpu_percent > cpu_threshold:
            result['status'] = 'WARNING'
            result['details']['cpu_warning'] = f'CPU使用率过高: {cpu_percent:.1f}%'
        
        # 内存使用率
        memory = psutil.virtual_memory()
        result['details']['memory_percent'] = memory.percent
        result['details']['memory_available_gb'] = memory.available / (1024**3)
        
        if memory.percent > memory_threshold:
            if result['status'] != 'CRITICAL':
                result['status'] = 'WARNING'
            result['details']['memory_warning'] = f'内存使用率过高: {memory.percent:.1f}%'
        
        # 磁盘使用率
        disk = psutil.disk_usage('/')
        result['details']['disk_percent'] = disk.percent
        result['details']['disk_free_gb'] = disk.free / (1024**3)
        
        if disk.percent > disk_threshold:
            result['status'] = 'CRITICAL'
            result['details']['disk_critical'] = f'磁盘使用率过高: {disk.percent:.1f}%'
        
        # 网络连接数
        connections = psutil.net_connections()
        result['details']['network_connections'] = len(connections)
        
    except ImportError:
        result['status'] = 'WARNING'
        result['details']['error'] = 'psutil模块未安装，跳过资源检查'
    except Exception as e:
        result['status'] = 'ERROR'
        result['details']['error'] = str(e)
    
    return result


def check_log_files(log_dir: str = '/var/log/feduwa', max_size_mb: float = 100.0) -> Dict[str, Any]:
    """检查日志文件"""
    result = {
        'status': 'HEALTHY',
        'timestamp': time.time(),
        'details': {}
    }
    
    try:
        log_path = Path(log_dir)
        
        if not log_path.exists():
            result['status'] = 'WARNING'
            result['details']['warning'] = '日志目录不存在'
            return result
        
        log_files = []
        total_size = 0
        
        for log_file in log_path.glob('*.log'):
            size_mb = log_file.stat().st_size / (1024 * 1024)
            total_size += size_mb
            
            log_files.append({
                'name': log_file.name,
                'size_mb': round(size_mb, 2),
                'modified': log_file.stat().st_mtime
            })
            
            if size_mb > max_size_mb:
                result['status'] = 'WARNING'
                result['details']['large_files'] = result['details'].get('large_files', [])
                result['details']['large_files'].append(log_file.name)
        
        result['details']['log_files'] = log_files
        result['details']['total_size_mb'] = round(total_size, 2)
        
        # 检查最近是否有日志更新
        if log_files:
            latest_modified = max(f['modified'] for f in log_files)
            if time.time() - latest_modified > 3600:  # 1小时没有更新
                result['status'] = 'WARNING'
                result['details']['warning'] = '日志文件超过1小时未更新'
        
    except Exception as e:
        result['status'] = 'ERROR'
        result['details']['error'] = str(e)
    
    return result


def check_configuration(config_dir: str = '/etc/feduwa') -> Dict[str, Any]:
    """检查配置文件"""
    result = {
        'status': 'HEALTHY',
        'timestamp': time.time(),
        'details': {}
    }
    
    try:
        config_path = Path(config_dir)
        
        if not config_path.exists():
            result['status'] = 'CRITICAL'
            result['details']['error'] = '配置目录不存在'
            return result
        
        # 检查必需的配置文件
        required_files = ['base.yaml', 'prod.yaml']
        missing_files = []
        
        for file_name in required_files:
            file_path = config_path / file_name
            if not file_path.exists():
                missing_files.append(file_name)
        
        if missing_files:
            result['status'] = 'CRITICAL'
            result['details']['missing_files'] = missing_files
        
        # 检查配置文件语法
        try:
            import yaml
            
            for config_file in config_path.glob('*.yaml'):
                with open(config_file, 'r', encoding='utf-8') as f:
                    yaml.safe_load(f)
            
            result['details']['syntax_check'] = 'OK'
            
        except Exception as e:
            result['status'] = 'ERROR'
            result['details']['syntax_error'] = str(e)
        
    except Exception as e:
        result['status'] = 'ERROR'
        result['details']['error'] = str(e)
    
    return result


def check_dependencies() -> Dict[str, Any]:
    """检查Python依赖"""
    result = {
        'status': 'HEALTHY',
        'timestamp': time.time(),
        'details': {}
    }
    
    required_packages = [
        'websockets',
        'numpy',
        'scikit-learn',
        'psutil',
        'yaml'
    ]
    
    missing_packages = []
    installed_packages = {}
    
    for package in required_packages:
        try:
            module = __import__(package)
            version = getattr(module, '__version__', 'unknown')
            installed_packages[package] = version
        except ImportError:
            missing_packages.append(package)
    
    result['details']['installed_packages'] = installed_packages
    
    if missing_packages:
        result['status'] = 'CRITICAL'
        result['details']['missing_packages'] = missing_packages
    
    # 检查Python版本
    python_version = f"{sys.version_info.major}.{sys.version_info.minor}.{sys.version_info.micro}"
    result['details']['python_version'] = python_version
    
    if sys.version_info < (3, 8):
        result['status'] = 'CRITICAL'
        result['details']['python_version_error'] = 'Python版本过低，需要3.8或更高版本'
    
    return result


def check_network_connectivity(hosts: List[str] = None) -> Dict[str, Any]:
    """检查网络连通性"""
    result = {
        'status': 'HEALTHY',
        'timestamp': time.time(),
        'details': {}
    }
    
    if hosts is None:
        hosts = ['8.8.8.8', 'google.com']
    
    connectivity_results = {}
    
    for host in hosts:
        try:
            # 使用ping命令检查连通性
            if sys.platform.startswith('win'):
                cmd = ['ping', '-n', '1', '-w', '3000', host]
            else:
                cmd = ['ping', '-c', '1', '-W', '3', host]
            
            process = subprocess.run(cmd, capture_output=True, timeout=10)
            
            connectivity_results[host] = {
                'reachable': process.returncode == 0,
                'response_time': 'unknown'
            }
            
        except subprocess.TimeoutExpired:
            connectivity_results[host] = {
                'reachable': False,
                'error': 'timeout'
            }
        except Exception as e:
            connectivity_results[host] = {
                'reachable': False,
                'error': str(e)
            }
    
    result['details']['connectivity'] = connectivity_results
    
    # 检查是否有任何主机可达
    reachable_hosts = [host for host, info in connectivity_results.items() if info.get('reachable')]
    
    if not reachable_hosts:
        result['status'] = 'CRITICAL'
        result['details']['error'] = '无法连接到任何外部主机'
    elif len(reachable_hosts) < len(hosts):
        result['status'] = 'WARNING'
        result['details']['warning'] = f'部分主机不可达: {len(reachable_hosts)}/{len(hosts)}'
    
    return result


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='FedUWA Python VM 健康检查')
    parser.add_argument('--config', '-c', help='配置文件路径')
    parser.add_argument('--output', '-o', choices=['json', 'text'], default='text',
                       help='输出格式')
    parser.add_argument('--verbose', '-v', action='store_true', help='详细输出')
    parser.add_argument('--checks', nargs='+', 
                       choices=['process', 'websocket', 'resources', 'logs', 'config', 'deps', 'network'],
                       help='指定要执行的检查项')
    
    # 检查参数
    parser.add_argument('--pid-file', default='/var/run/feduwa-vm.pid', help='PID文件路径')
    parser.add_argument('--server-url', default='ws://localhost:8080/websocket', help='WebSocket服务器URL')
    parser.add_argument('--log-dir', default='/var/log/feduwa', help='日志目录')
    parser.add_argument('--config-dir', default='/etc/feduwa', help='配置目录')
    
    args = parser.parse_args()
    
    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)
    
    # 创建健康检查器
    checker = HealthChecker({})
    
    # 添加检查项
    checks_to_run = args.checks or ['process', 'resources', 'logs', 'config', 'deps']
    
    if 'process' in checks_to_run:
        checker.add_check('process', check_service_process, pid_file=args.pid_file)
    
    if 'websocket' in checks_to_run:
        checker.add_check('websocket', check_websocket_connection, 
                         server_url=args.server_url, timeout=10)
    
    if 'resources' in checks_to_run:
        checker.add_check('resources', check_system_resources,
                         cpu_threshold=90.0, memory_threshold=90.0, disk_threshold=95.0)
    
    if 'logs' in checks_to_run:
        checker.add_check('logs', check_log_files, log_dir=args.log_dir, max_size_mb=100.0)
    
    if 'config' in checks_to_run:
        checker.add_check('config', check_configuration, config_dir=args.config_dir)
    
    if 'deps' in checks_to_run:
        checker.add_check('dependencies', check_dependencies)
    
    if 'network' in checks_to_run:
        checker.add_check('network', check_network_connectivity)
    
    # 执行检查
    results = checker.run_all_checks()
    
    # 输出结果
    if args.output == 'json':
        print(json.dumps(results, indent=2))
    else:
        # 文本格式输出
        print(f"健康检查结果: {results['overall_status']}")
        print(f"检查时间: {time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(results['timestamp']))}")
        print()
        
        for check_name, check_result in results['checks'].items():
            status = check_result.get('status', 'UNKNOWN')
            print(f"[{status}] {check_name}")
            
            if args.verbose or status != 'HEALTHY':
                details = check_result.get('details', {})
                for key, value in details.items():
                    print(f"  {key}: {value}")
                print()
    
    # 返回适当的退出码
    return checker.get_exit_code()


if __name__ == '__main__':
    try:
        exit_code = main()
        sys.exit(exit_code)
    except KeyboardInterrupt:
        print("\n健康检查被中断")
        sys.exit(1)
    except Exception as e:
        logger.error(f"健康检查失败: {e}")
        sys.exit(2)
