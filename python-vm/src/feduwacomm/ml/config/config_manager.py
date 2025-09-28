"""
配置管理器 - 支持多环境配置管理和动态配置更新
基于v1.4协议的Python VM配置系统
"""

import os
import json
import logging
import threading
from typing import Dict, Any, Optional, List
from pathlib import Path
from enum import Enum
import yaml

logger = logging.getLogger(__name__)


class Environment(Enum):
    """环境类型枚举"""
    DEVELOPMENT = "dev"
    TESTING = "test"
    PRODUCTION = "prod"


class ConfigManager:
    """配置管理器 - 支持多环境配置和动态更新"""

    def __init__(self, config_dir: Optional[str] = None, environment: Optional[str] = None):
        """
        初始化配置管理器
        
        Args:
            config_dir: 配置文件目录，默认为项目根目录的config文件夹
            environment: 环境名称，默认从环境变量获取
        """
        self.config_dir = Path(config_dir) if config_dir else self._get_default_config_dir()
        self.environment = Environment(environment or os.getenv('FEDUWA_ENV', 'dev'))
        
        # 配置存储
        self.configs: Dict[str, Any] = {}
        self.config_version = 0
        self.config_lock = threading.RLock()
        
        # 配置文件监听
        self.file_watchers: Dict[str, float] = {}  # 文件路径 -> 最后修改时间
        self.watch_thread: Optional[threading.Thread] = None
        self.watching = False
        
        # 配置变更回调
        self.change_callbacks: List[callable] = []
        
        # 加载配置
        self._load_all_configs()
        
        logger.info(f"配置管理器初始化完成 - 环境: {self.environment.value}")

    def _get_default_config_dir(self) -> Path:
        """获取默认配置目录"""
        # 从当前文件向上查找项目根目录
        current_dir = Path(__file__).parent
        while current_dir.parent != current_dir:
            if (current_dir / 'config').exists():
                return current_dir / 'config'
            current_dir = current_dir.parent
        
        # 如果找不到，使用当前目录下的config
        config_dir = Path.cwd() / 'config'
        config_dir.mkdir(exist_ok=True)
        return config_dir

    def _load_all_configs(self):
        """加载所有配置文件"""
        with self.config_lock:
            try:
                # 加载基础配置
                self._load_base_config()
                
                # 加载环境特定配置
                self._load_environment_config()
                
                # 加载模块特定配置
                self._load_module_configs()
                
                # 应用环境变量覆盖
                self._apply_env_overrides()
                
                # 验证配置
                self._validate_configs()
                
                self.config_version += 1
                logger.info(f"配置加载完成 - 版本: {self.config_version}")
                
            except Exception as e:
                logger.error(f"配置加载失败: {e}")
                raise

    def _load_base_config(self):
        """加载基础配置"""
        base_config_file = self.config_dir / 'base.yaml'
        if base_config_file.exists():
            self.configs.update(self._load_config_file(base_config_file))
        else:
            # 创建默认基础配置
            default_config = self._get_default_base_config()
            self.configs.update(default_config)
            self._save_config_file(base_config_file, default_config)

    def _load_environment_config(self):
        """加载环境特定配置"""
        env_config_file = self.config_dir / f'{self.environment.value}.yaml'
        if env_config_file.exists():
            env_config = self._load_config_file(env_config_file)
            self._merge_configs(self.configs, env_config)
        else:
            # 创建默认环境配置
            default_env_config = self._get_default_env_config()
            self._merge_configs(self.configs, default_env_config)
            self._save_config_file(env_config_file, default_env_config)

    def _load_module_configs(self):
        """加载模块特定配置"""
        modules_dir = self.config_dir / 'modules'
        if modules_dir.exists():
            for config_file in modules_dir.glob('*.yaml'):
                module_name = config_file.stem
                module_config = self._load_config_file(config_file)
                if 'modules' not in self.configs:
                    self.configs['modules'] = {}
                self.configs['modules'][module_name] = module_config

    def _load_config_file(self, file_path: Path) -> Dict[str, Any]:
        """加载配置文件"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                if file_path.suffix.lower() == '.json':
                    config = json.load(f)
                else:  # YAML
                    config = yaml.safe_load(f) or {}
            
            # 记录文件监听
            self.file_watchers[str(file_path)] = file_path.stat().st_mtime
            
            logger.debug(f"加载配置文件: {file_path}")
            return config
            
        except Exception as e:
            logger.error(f"加载配置文件失败 {file_path}: {e}")
            return {}

    def _save_config_file(self, file_path: Path, config: Dict[str, Any]):
        """保存配置文件"""
        try:
            file_path.parent.mkdir(parents=True, exist_ok=True)
            
            with open(file_path, 'w', encoding='utf-8') as f:
                if file_path.suffix.lower() == '.json':
                    json.dump(config, f, indent=2, ensure_ascii=False)
                else:  # YAML
                    yaml.dump(config, f, default_flow_style=False, allow_unicode=True)
            
            logger.debug(f"保存配置文件: {file_path}")
            
        except Exception as e:
            logger.error(f"保存配置文件失败 {file_path}: {e}")

    def _merge_configs(self, base: Dict[str, Any], override: Dict[str, Any]):
        """合并配置（深度合并）"""
        for key, value in override.items():
            if key in base and isinstance(base[key], dict) and isinstance(value, dict):
                self._merge_configs(base[key], value)
            else:
                base[key] = value

    def _apply_env_overrides(self):
        """应用环境变量覆盖"""
        # 支持通过环境变量覆盖配置
        # 格式: FEDUWA_CONFIG_<section>_<key>=value
        prefix = "FEDUWA_CONFIG_"
        
        for env_key, env_value in os.environ.items():
            if env_key.startswith(prefix):
                config_path = env_key[len(prefix):].lower().split('_')
                self._set_nested_config(self.configs, config_path, self._parse_env_value(env_value))

    def _set_nested_config(self, config: Dict[str, Any], path: List[str], value: Any):
        """设置嵌套配置值"""
        current = config
        for key in path[:-1]:
            if key not in current:
                current[key] = {}
            current = current[key]
        current[path[-1]] = value

    def _parse_env_value(self, value: str) -> Any:
        """解析环境变量值"""
        # 尝试解析为JSON
        try:
            return json.loads(value)
        except:
            pass
        
        # 尝试解析为布尔值
        if value.lower() in ('true', 'false'):
            return value.lower() == 'true'
        
        # 尝试解析为数字
        try:
            if '.' in value:
                return float(value)
            else:
                return int(value)
        except:
            pass
        
        # 返回字符串
        return value

    def _validate_configs(self):
        """验证配置完整性"""
        required_sections = ['websocket', 'federated', 'logging']
        
        for section in required_sections:
            if section not in self.configs:
                logger.warning(f"缺少必需的配置节: {section}")
        
        # 验证WebSocket配置
        websocket_config = self.configs.get('websocket', {})
        if 'server_url' not in websocket_config:
            logger.warning("WebSocket配置缺少server_url")
        
        # 验证联邦学习配置
        federated_config = self.configs.get('federated', {})
        if 'supported_algorithms' not in federated_config:
            logger.warning("联邦学习配置缺少supported_algorithms")

    def get(self, key: str, default: Any = None) -> Any:
        """
        获取配置值
        
        Args:
            key: 配置键，支持点分隔的嵌套键（如 'websocket.server_url'）
            default: 默认值
        
        Returns:
            配置值
        """
        with self.config_lock:
            keys = key.split('.')
            current = self.configs
            
            try:
                for k in keys:
                    current = current[k]
                return current
            except (KeyError, TypeError):
                return default

    def set(self, key: str, value: Any, persist: bool = False):
        """
        设置配置值
        
        Args:
            key: 配置键
            value: 配置值
            persist: 是否持久化到文件
        """
        with self.config_lock:
            keys = key.split('.')
            current = self.configs
            
            # 导航到目标位置
            for k in keys[:-1]:
                if k not in current:
                    current[k] = {}
                current = current[k]
            
            # 设置值
            old_value = current.get(keys[-1])
            current[keys[-1]] = value
            
            # 更新版本
            self.config_version += 1
            
            # 触发变更回调
            self._notify_config_change(key, old_value, value)
            
            # 持久化
            if persist:
                self._persist_config_change(key, value)
            
            logger.debug(f"配置更新: {key} = {value}")

    def _persist_config_change(self, key: str, value: Any):
        """持久化配置变更"""
        try:
            # 确定配置应该保存到哪个文件
            if key.startswith('modules.'):
                # 模块配置
                module_name = key.split('.')[1]
                config_file = self.config_dir / 'modules' / f'{module_name}.yaml'
                # 获取模块配置
                module_config = self.configs.get('modules', {}).get(module_name, {})
                self._save_config_file(config_file, module_config)
            else:
                # 环境配置
                env_config_file = self.config_dir / f'{self.environment.value}.yaml'
                # 只保存环境特定的配置
                env_config = self._extract_env_config()
                self._save_config_file(env_config_file, env_config)
                
        except Exception as e:
            logger.error(f"持久化配置失败: {e}")

    def _extract_env_config(self) -> Dict[str, Any]:
        """提取环境特定配置"""
        # 这里可以实现更复杂的逻辑来区分哪些配置应该保存到环境文件
        # 现在简单地返回除了modules之外的所有配置
        env_config = {}
        for key, value in self.configs.items():
            if key != 'modules':
                env_config[key] = value
        return env_config

    def reload(self):
        """重新加载配置"""
        logger.info("重新加载配置")
        old_version = self.config_version
        self._load_all_configs()
        
        if self.config_version != old_version:
            self._notify_config_reload()

    def _notify_config_change(self, key: str, old_value: Any, new_value: Any):
        """通知配置变更"""
        for callback in self.change_callbacks:
            try:
                callback(key, old_value, new_value)
            except Exception as e:
                logger.error(f"配置变更回调执行失败: {e}")

    def _notify_config_reload(self):
        """通知配置重新加载"""
        for callback in self.change_callbacks:
            try:
                callback('__reload__', None, None)
            except Exception as e:
                logger.error(f"配置重载回调执行失败: {e}")

    def add_change_callback(self, callback: callable):
        """添加配置变更回调"""
        self.change_callbacks.append(callback)

    def remove_change_callback(self, callback: callable):
        """移除配置变更回调"""
        if callback in self.change_callbacks:
            self.change_callbacks.remove(callback)

    def start_file_watching(self):
        """启动文件监听"""
        if self.watching:
            return
        
        self.watching = True
        self.watch_thread = threading.Thread(target=self._file_watch_loop, daemon=True)
        self.watch_thread.start()
        logger.info("启动配置文件监听")

    def stop_file_watching(self):
        """停止文件监听"""
        self.watching = False
        if self.watch_thread:
            self.watch_thread.join(timeout=1.0)
        logger.info("停止配置文件监听")

    def _file_watch_loop(self):
        """文件监听循环"""
        import time
        
        while self.watching:
            try:
                changed_files = []
                
                for file_path, last_mtime in list(self.file_watchers.items()):
                    try:
                        current_mtime = Path(file_path).stat().st_mtime
                        if current_mtime > last_mtime:
                            changed_files.append(file_path)
                            self.file_watchers[file_path] = current_mtime
                    except FileNotFoundError:
                        # 文件被删除
                        del self.file_watchers[file_path]
                
                if changed_files:
                    logger.info(f"检测到配置文件变更: {changed_files}")
                    self.reload()
                
                time.sleep(1.0)  # 每秒检查一次
                
            except Exception as e:
                logger.error(f"文件监听异常: {e}")
                time.sleep(5.0)  # 出错时等待更长时间

    def get_websocket_config(self) -> Dict[str, Any]:
        """获取WebSocket配置"""
        return self.get('websocket', {})

    def get_federated_config(self) -> Dict[str, Any]:
        """获取联邦学习配置"""
        return self.get('federated', {})

    def get_logging_config(self) -> Dict[str, Any]:
        """获取日志配置"""
        return self.get('logging', {})

    def get_module_config(self, module_name: str) -> Dict[str, Any]:
        """获取模块配置"""
        return self.get(f'modules.{module_name}', {})

    def get_all_configs(self) -> Dict[str, Any]:
        """获取所有配置"""
        with self.config_lock:
            return self.configs.copy()

    def get_config_info(self) -> Dict[str, Any]:
        """获取配置信息"""
        return {
            'environment': self.environment.value,
            'config_dir': str(self.config_dir),
            'version': self.config_version,
            'watched_files': list(self.file_watchers.keys()),
            'watching': self.watching
        }

    def _get_default_base_config(self) -> Dict[str, Any]:
        """获取默认基础配置"""
        return {
            'websocket': {
                'server_url': 'ws://localhost:8080/websocket',
                'connection_timeout': 30,
                'heartbeat_interval': 30,
                'max_reconnect_attempts': 5,
                'reconnect_delay': 5,
                'message_queue_size': 1000
            },
            'federated': {
                'supported_algorithms': [
                    'FEDERATED_AVERAGING',
                    'FEDERATED_PROXIMAL',
                    'FEDERATED_NOVA',
                    'SCAFFOLD'
                ],
                'max_concurrent_tasks': 3,
                'default_local_epochs': 5,
                'default_batch_size': 32,
                'model_compression': {
                    'enabled': True,
                    'algorithm': 'gzip',
                    'level': 6
                }
            },
            'logging': {
                'level': 'INFO',
                'format': '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
                'file': {
                    'enabled': True,
                    'path': 'logs/feduwa-vm.log',
                    'max_size': '10MB',
                    'backup_count': 5
                },
                'console': {
                    'enabled': True,
                    'level': 'INFO'
                }
            },
            'performance': {
                'monitoring': {
                    'enabled': True,
                    'interval': 60,
                    'metrics': ['cpu', 'memory', 'disk', 'network']
                },
                'optimization': {
                    'thread_pool_size': 4,
                    'async_processing': True,
                    'cache_size': 100
                }
            },
            'security': {
                'encryption': {
                    'enabled': False,
                    'algorithm': 'AES-256-GCM'
                },
                'authentication': {
                    'enabled': False,
                    'token_expiry': 3600
                }
            }
        }

    def _get_default_env_config(self) -> Dict[str, Any]:
        """获取默认环境配置"""
        if self.environment == Environment.DEVELOPMENT:
            return {
                'logging': {
                    'level': 'DEBUG',
                    'console': {'level': 'DEBUG'}
                },
                'performance': {
                    'monitoring': {'interval': 30}
                }
            }
        elif self.environment == Environment.TESTING:
            return {
                'websocket': {
                    'server_url': 'ws://test-server:8080/websocket'
                },
                'logging': {
                    'level': 'INFO'
                }
            }
        else:  # PRODUCTION
            return {
                'websocket': {
                    'server_url': 'ws://prod-server:8080/websocket',
                    'heartbeat_interval': 60
                },
                'logging': {
                    'level': 'WARNING',
                    'console': {'enabled': False}
                },
                'security': {
                    'encryption': {'enabled': True},
                    'authentication': {'enabled': True}
                }
            }


# 全局配置管理器实例
_config_manager: Optional[ConfigManager] = None


def get_config_manager() -> ConfigManager:
    """获取全局配置管理器实例"""
    global _config_manager
    if _config_manager is None:
        _config_manager = ConfigManager()
    return _config_manager


def init_config_manager(config_dir: Optional[str] = None, environment: Optional[str] = None) -> ConfigManager:
    """初始化全局配置管理器"""
    global _config_manager
    _config_manager = ConfigManager(config_dir, environment)
    return _config_manager
