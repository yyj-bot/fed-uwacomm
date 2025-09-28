#!/usr/bin/env python3
"""
Python VM 启动脚本
生产级启动脚本，支持配置管理、日志记录、健康检查
"""

import os
import sys
import argparse
import logging
import signal
import time
import json
import subprocess
from pathlib import Path
from typing import Optional, Dict, Any
import threading
import atexit

# 添加项目路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / 'src'))

try:
    from feduwacomm.ml.websocket.client import WebSocketClient
    from feduwacomm.ml.federated.client import FederatedClient
    from feduwacomm.ml.config.config_manager import ConfigManager, init_config_manager
    from feduwacomm.ml.utils.error_handler import ErrorHandler
    from feduwacomm.ml.utils.monitor import Monitor
except ImportError as e:
    print(f"导入模块失败: {e}")
    print("请确保已正确安装依赖包")
    sys.exit(1)


class VMService:
    """VM服务管理器"""
    
    def __init__(self, config_dir: Optional[str] = None, environment: Optional[str] = None):
        """
        初始化VM服务
        
        Args:
            config_dir: 配置目录
            environment: 环境名称
        """
        self.config_manager = init_config_manager(config_dir, environment)
        self.logger = self._setup_logging()
        
        # 服务组件
        self.websocket_client: Optional[WebSocketClient] = None
        self.federated_client: Optional[FederatedClient] = None
        self.error_handler: Optional[ErrorHandler] = None
        self.monitor: Optional[Monitor] = None
        
        # 服务状态
        self.is_running = False
        self.shutdown_event = threading.Event()
        
        # 进程信息
        self.pid_file = Path('/var/run/feduwa-vm.pid')
        self.status_file = Path('/var/run/feduwa-vm-status.json')
        
        self.logger.info("VM服务初始化完成")

    def _setup_logging(self) -> logging.Logger:
        """设置日志"""
        logging_config = self.config_manager.get_logging_config()
        
        # 配置根日志器
        logger = logging.getLogger('feduwa.vm')
        logger.setLevel(getattr(logging, logging_config.get('level', 'INFO')))
        
        # 清除现有处理器
        for handler in logger.handlers[:]:
            logger.removeHandler(handler)
        
        # 文件日志
        if logging_config.get('file', {}).get('enabled', True):
            file_config = logging_config['file']
            log_file = Path(file_config.get('path', 'logs/feduwa-vm.log'))
            log_file.parent.mkdir(parents=True, exist_ok=True)
            
            from logging.handlers import RotatingFileHandler
            file_handler = RotatingFileHandler(
                log_file,
                maxBytes=self._parse_size(file_config.get('max_size', '10MB')),
                backupCount=file_config.get('backup_count', 5)
            )
            file_handler.setLevel(logging.DEBUG)
            
            formatter = logging.Formatter(
                logging_config.get('format', '%(asctime)s - %(name)s - %(levelname)s - %(message)s')
            )
            file_handler.setFormatter(formatter)
            logger.addHandler(file_handler)
        
        # 控制台日志
        if logging_config.get('console', {}).get('enabled', True):
            console_handler = logging.StreamHandler()
            console_level = logging_config.get('console', {}).get('level', 'INFO')
            console_handler.setLevel(getattr(logging, console_level))
            
            formatter = logging.Formatter('%(asctime)s [%(levelname)s] %(message)s')
            console_handler.setFormatter(formatter)
            logger.addHandler(console_handler)
        
        return logger

    def _parse_size(self, size_str: str) -> int:
        """解析大小字符串"""
        size_str = size_str.upper()
        if size_str.endswith('KB'):
            return int(size_str[:-2]) * 1024
        elif size_str.endswith('MB'):
            return int(size_str[:-2]) * 1024 * 1024
        elif size_str.endswith('GB'):
            return int(size_str[:-2]) * 1024 * 1024 * 1024
        else:
            return int(size_str)

    def _create_pid_file(self):
        """创建PID文件"""
        try:
            self.pid_file.parent.mkdir(parents=True, exist_ok=True)
            with open(self.pid_file, 'w') as f:
                f.write(str(os.getpid()))
            self.logger.info(f"PID文件创建: {self.pid_file}")
        except Exception as e:
            self.logger.error(f"创建PID文件失败: {e}")

    def _remove_pid_file(self):
        """删除PID文件"""
        try:
            if self.pid_file.exists():
                self.pid_file.unlink()
                self.logger.info("PID文件已删除")
        except Exception as e:
            self.logger.error(f"删除PID文件失败: {e}")

    def _update_status_file(self, status: Dict[str, Any]):
        """更新状态文件"""
        try:
            self.status_file.parent.mkdir(parents=True, exist_ok=True)
            with open(self.status_file, 'w') as f:
                json.dump(status, f, indent=2)
        except Exception as e:
            self.logger.error(f"更新状态文件失败: {e}")

    def _check_dependencies(self) -> bool:
        """检查依赖"""
        self.logger.info("检查系统依赖...")
        
        # 检查Python版本
        if sys.version_info < (3, 8):
            self.logger.error("需要Python 3.8或更高版本")
            return False
        
        # 检查必需的包
        required_packages = ['websockets', 'numpy', 'scikit-learn']
        missing_packages = []
        
        for package in required_packages:
            try:
                __import__(package)
            except ImportError:
                missing_packages.append(package)
        
        if missing_packages:
            self.logger.error(f"缺少必需的包: {missing_packages}")
            return False
        
        # 检查配置文件
        websocket_config = self.config_manager.get_websocket_config()
        if not websocket_config.get('server_url'):
            self.logger.error("WebSocket服务器URL未配置")
            return False
        
        self.logger.info("依赖检查通过")
        return True

    def _setup_signal_handlers(self):
        """设置信号处理器"""
        def signal_handler(signum, frame):
            self.logger.info(f"收到信号 {signum}，开始优雅关闭...")
            self.shutdown()
        
        signal.signal(signal.SIGTERM, signal_handler)
        signal.signal(signal.SIGINT, signal_handler)
        
        # 注册退出处理器
        atexit.register(self._cleanup)

    def _initialize_components(self):
        """初始化组件"""
        self.logger.info("初始化服务组件...")
        
        # WebSocket客户端
        websocket_config = self.config_manager.get_websocket_config()
        vm_id = os.getenv('FEDUWA_VM_ID') or f"vm-{os.getpid()}"
        
        self.websocket_client = WebSocketClient(
            server_url=websocket_config['server_url'],
            vm_id=vm_id,
            config=websocket_config
        )
        
        # 联邦学习客户端
        self.federated_client = FederatedClient(self.websocket_client)
        
        # 错误处理器
        self.error_handler = ErrorHandler(self.websocket_client)
        self.error_handler.start_error_processing()
        
        # 监控器
        monitor_config = self.config_manager.get('performance.monitoring', {})
        self.monitor = Monitor(self.websocket_client, monitor_config)
        if monitor_config.get('enabled', True):
            self.monitor.start_monitoring()
        
        # 启动配置文件监听
        self.config_manager.start_file_watching()
        
        self.logger.info("服务组件初始化完成")

    async def _connect_to_server(self) -> bool:
        """连接到服务器"""
        self.logger.info("连接到服务器...")
        
        max_attempts = 5
        for attempt in range(1, max_attempts + 1):
            try:
                result = await self.websocket_client.connect()
                if result:
                    self.logger.info("服务器连接成功")
                    return True
                else:
                    self.logger.warning(f"连接失败，尝试 {attempt}/{max_attempts}")
                    if attempt < max_attempts:
                        await asyncio.sleep(5)
            except Exception as e:
                self.logger.error(f"连接异常: {e}")
                if attempt < max_attempts:
                    await asyncio.sleep(5)
        
        self.logger.error("无法连接到服务器")
        return False

    def _run_health_check(self) -> Dict[str, Any]:
        """运行健康检查"""
        health_status = {
            'timestamp': time.time(),
            'status': 'HEALTHY',
            'checks': {}
        }
        
        try:
            # WebSocket连接检查
            if self.websocket_client:
                health_status['checks']['websocket'] = {
                    'status': 'CONNECTED' if self.websocket_client.is_connected else 'DISCONNECTED',
                    'vm_id': self.websocket_client.vm_id
                }
            
            # 监控器检查
            if self.monitor:
                monitor_health = self.monitor.get_health_status()
                health_status['checks']['monitor'] = monitor_health
            
            # 系统资源检查
            if self.websocket_client:
                resource_usage = self.websocket_client._get_resource_usage()
                health_status['checks']['resources'] = resource_usage
                
                # 检查资源阈值
                if resource_usage.get('memory_percent', 0) > 90:
                    health_status['status'] = 'WARNING'
                if resource_usage.get('cpu_percent', 0) > 95:
                    health_status['status'] = 'CRITICAL'
            
            # 活跃任务检查
            if self.federated_client:
                active_tasks = len(self.federated_client.active_tasks)
                health_status['checks']['tasks'] = {
                    'active_count': active_tasks,
                    'status': 'OK' if active_tasks < 10 else 'WARNING'
                }
        
        except Exception as e:
            self.logger.error(f"健康检查失败: {e}")
            health_status['status'] = 'ERROR'
            health_status['error'] = str(e)
        
        return health_status

    async def start(self):
        """启动服务"""
        self.logger.info("启动Python VM服务...")
        
        # 检查依赖
        if not self._check_dependencies():
            return False
        
        # 创建PID文件
        self._create_pid_file()
        
        # 设置信号处理器
        self._setup_signal_handlers()
        
        # 初始化组件
        self._initialize_components()
        
        # 连接到服务器
        if not await self._connect_to_server():
            return False
        
        self.is_running = True
        
        # 更新状态
        status = {
            'status': 'RUNNING',
            'pid': os.getpid(),
            'start_time': time.time(),
            'vm_id': self.websocket_client.vm_id if self.websocket_client else None
        }
        self._update_status_file(status)
        
        self.logger.info("Python VM服务启动成功")
        
        # 主循环
        await self._main_loop()
        
        return True

    async def _main_loop(self):
        """主循环"""
        self.logger.info("进入主循环...")
        
        health_check_interval = 60  # 60秒健康检查间隔
        last_health_check = 0
        
        while self.is_running and not self.shutdown_event.is_set():
            try:
                current_time = time.time()
                
                # 定期健康检查
                if current_time - last_health_check >= health_check_interval:
                    health_status = self._run_health_check()
                    self._update_status_file({
                        'status': 'RUNNING',
                        'health': health_status,
                        'last_update': current_time
                    })
                    last_health_check = current_time
                
                # 检查WebSocket连接
                if self.websocket_client and not self.websocket_client.is_connected:
                    self.logger.warning("WebSocket连接断开，尝试重连...")
                    await self.websocket_client.reconnect()
                
                # 短暂等待
                await asyncio.sleep(1)
                
            except Exception as e:
                self.logger.error(f"主循环异常: {e}")
                if self.error_handler:
                    self.error_handler.handle_error(
                        error_type=self.error_handler.ErrorType.SYSTEM_ERROR,
                        message=f"主循环异常: {e}",
                        exception=e
                    )
                await asyncio.sleep(5)

    def shutdown(self):
        """关闭服务"""
        if not self.is_running:
            return
        
        self.logger.info("开始关闭服务...")
        self.is_running = False
        self.shutdown_event.set()
        
        # 更新状态
        self._update_status_file({
            'status': 'SHUTTING_DOWN',
            'shutdown_time': time.time()
        })
        
        self._cleanup()
        
        self.logger.info("服务关闭完成")

    def _cleanup(self):
        """清理资源"""
        try:
            # 停止监控
            if self.monitor:
                self.monitor.stop_monitoring()
            
            # 停止错误处理
            if self.error_handler:
                self.error_handler.stop_error_processing()
            
            # 停止配置监听
            if self.config_manager:
                self.config_manager.stop_file_watching()
            
            # 断开WebSocket连接
            if self.websocket_client:
                self.websocket_client.disconnect()
            
            # 删除PID文件
            self._remove_pid_file()
            
            # 更新最终状态
            self._update_status_file({
                'status': 'STOPPED',
                'stop_time': time.time()
            })
            
        except Exception as e:
            self.logger.error(f"清理资源时发生异常: {e}")


def check_status(pid_file: Path, status_file: Path) -> Dict[str, Any]:
    """检查服务状态"""
    status = {'running': False}
    
    # 检查PID文件
    if pid_file.exists():
        try:
            with open(pid_file, 'r') as f:
                pid = int(f.read().strip())
            
            # 检查进程是否存在
            try:
                os.kill(pid, 0)  # 发送信号0检查进程
                status['running'] = True
                status['pid'] = pid
            except OSError:
                # 进程不存在，删除过期的PID文件
                pid_file.unlink()
                status['running'] = False
        except Exception as e:
            status['error'] = f"读取PID文件失败: {e}"
    
    # 读取状态文件
    if status_file.exists():
        try:
            with open(status_file, 'r') as f:
                file_status = json.load(f)
            status.update(file_status)
        except Exception as e:
            status['status_file_error'] = str(e)
    
    return status


def stop_service(pid_file: Path) -> bool:
    """停止服务"""
    if not pid_file.exists():
        print("服务未运行")
        return True
    
    try:
        with open(pid_file, 'r') as f:
            pid = int(f.read().strip())
        
        print(f"停止服务 (PID: {pid})...")
        
        # 发送SIGTERM信号
        os.kill(pid, signal.SIGTERM)
        
        # 等待进程结束
        for i in range(30):  # 最多等待30秒
            try:
                os.kill(pid, 0)
                time.sleep(1)
            except OSError:
                print("服务已停止")
                return True
        
        # 如果进程仍然存在，发送SIGKILL
        print("强制停止服务...")
        os.kill(pid, signal.SIGKILL)
        time.sleep(2)
        
        return True
        
    except Exception as e:
        print(f"停止服务失败: {e}")
        return False


async def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='Python VM 服务管理')
    parser.add_argument('action', choices=['start', 'stop', 'restart', 'status', 'health'],
                       help='服务操作')
    parser.add_argument('--config-dir', '-c', help='配置目录路径')
    parser.add_argument('--environment', '-e', help='环境名称 (dev/test/prod)')
    parser.add_argument('--daemon', '-d', action='store_true', help='后台运行')
    parser.add_argument('--pid-file', help='PID文件路径', 
                       default='/var/run/feduwa-vm.pid')
    parser.add_argument('--status-file', help='状态文件路径',
                       default='/var/run/feduwa-vm-status.json')
    
    args = parser.parse_args()
    
    pid_file = Path(args.pid_file)
    status_file = Path(args.status_file)
    
    if args.action == 'start':
        # 检查是否已经运行
        status = check_status(pid_file, status_file)
        if status.get('running'):
            print(f"服务已在运行 (PID: {status.get('pid')})")
            return 1
        
        if args.daemon:
            # 后台运行
            if os.fork() > 0:
                return 0  # 父进程退出
            
            # 子进程继续
            os.setsid()
            os.chdir('/')
            os.umask(0)
            
            # 重定向标准输入输出
            with open('/dev/null', 'r') as f:
                os.dup2(f.fileno(), sys.stdin.fileno())
            with open('/dev/null', 'w') as f:
                os.dup2(f.fileno(), sys.stdout.fileno())
                os.dup2(f.fileno(), sys.stderr.fileno())
        
        # 启动服务
        service = VMService(args.config_dir, args.environment)
        try:
            success = await service.start()
            return 0 if success else 1
        except KeyboardInterrupt:
            service.shutdown()
            return 0
    
    elif args.action == 'stop':
        return 0 if stop_service(pid_file) else 1
    
    elif args.action == 'restart':
        print("重启服务...")
        stop_service(pid_file)
        time.sleep(2)
        
        service = VMService(args.config_dir, args.environment)
        success = await service.start()
        return 0 if success else 1
    
    elif args.action == 'status':
        status = check_status(pid_file, status_file)
        print(json.dumps(status, indent=2))
        return 0
    
    elif args.action == 'health':
        status = check_status(pid_file, status_file)
        if not status.get('running'):
            print("服务未运行")
            return 1
        
        health = status.get('health', {})
        print(json.dumps(health, indent=2))
        return 0 if health.get('status') == 'HEALTHY' else 1


if __name__ == '__main__':
    import asyncio
    
    try:
        exit_code = asyncio.run(main())
        sys.exit(exit_code)
    except KeyboardInterrupt:
        print("\n服务被中断")
        sys.exit(0)
    except Exception as e:
        print(f"启动失败: {e}")
        sys.exit(1)
