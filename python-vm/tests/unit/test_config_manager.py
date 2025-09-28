"""
配置管理系统单元测试
"""

import pytest
import tempfile
import shutil
import yaml
import json
import os
from pathlib import Path
from unittest.mock import Mock, patch, mock_open

from feduwacomm.ml.config.config_manager import ConfigManager


@pytest.mark.unit
class TestConfigManager:
    """配置管理器测试类"""

    @pytest.fixture
    def temp_config_dir(self):
        """临时配置目录fixture"""
        temp_dir = tempfile.mkdtemp()
        yield Path(temp_dir)
        shutil.rmtree(temp_dir)

    @pytest.fixture
    def sample_config(self):
        """示例配置数据"""
        return {
            'websocket': {
                'server_url': 'ws://localhost:8080/websocket',
                'connection_timeout': 30,
                'heartbeat_interval': 60,
                'max_reconnect_attempts': 5,
                'reconnect_delay': 2
            },
            'federated': {
                'supported_algorithms': ['FEDERATED_AVERAGING', 'FEDERATED_PROXIMAL'],
                'max_concurrent_tasks': 3,
                'default_local_epochs': 5,
                'default_batch_size': 32,
                'default_learning_rate': 0.01
            },
            'logging': {
                'level': 'INFO',
                'format': '%(asctime)s [%(levelname)s] %(name)s: %(message)s',
                'file': 'feduwa.log',
                'max_size': '10MB',
                'backup_count': 5
            },
            'database': {
                'type': 'sqlite',
                'path': 'data/feduwa.db',
                'pool_size': 10,
                'timeout': 30
            }
        }

    def test_config_manager_initialization(self, temp_config_dir):
        """测试配置管理器初始化"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        
        assert config_manager.config_dir == Path(temp_config_dir)
        assert config_manager.environment == 'test'
        assert config_manager.config == {}

    def test_load_config_from_yaml(self, temp_config_dir, sample_config):
        """测试从YAML文件加载配置"""
        # 创建配置文件
        config_file = temp_config_dir / 'base.yaml'
        with open(config_file, 'w', encoding='utf-8') as f:
            yaml.dump(sample_config, f)
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        
        result = config_manager.load_config()
        
        assert result == True
        assert config_manager.config == sample_config

    def test_load_config_from_json(self, temp_config_dir, sample_config):
        """测试从JSON文件加载配置"""
        # 创建配置文件
        config_file = temp_config_dir / 'base.json'
        with open(config_file, 'w', encoding='utf-8') as f:
            json.dump(sample_config, f)
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        
        result = config_manager.load_config()
        
        assert result == True
        assert config_manager.config == sample_config

    def test_load_environment_specific_config(self, temp_config_dir, sample_config):
        """测试加载环境特定配置"""
        # 创建基础配置
        base_config_file = temp_config_dir / 'base.yaml'
        with open(base_config_file, 'w', encoding='utf-8') as f:
            yaml.dump(sample_config, f)
        
        # 创建测试环境配置
        test_config = {
            'websocket': {
                'server_url': 'ws://test-server:8080/websocket'
            },
            'logging': {
                'level': 'DEBUG'
            }
        }
        test_config_file = temp_config_dir / 'test.yaml'
        with open(test_config_file, 'w', encoding='utf-8') as f:
            yaml.dump(test_config, f)
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        
        result = config_manager.load_config()
        
        assert result == True
        # 验证配置合并
        assert config_manager.config['websocket']['server_url'] == 'ws://test-server:8080/websocket'
        assert config_manager.config['logging']['level'] == 'DEBUG'
        assert config_manager.config['federated']['max_concurrent_tasks'] == 3  # 基础配置保留

    def test_get_config_value(self, temp_config_dir, sample_config):
        """测试获取配置值"""
        # 创建配置文件
        config_file = temp_config_dir / 'base.yaml'
        with open(config_file, 'w', encoding='utf-8') as f:
            yaml.dump(sample_config, f)
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.load_config()
        
        # 测试获取嵌套配置值
        assert config_manager.get('websocket.server_url') == 'ws://localhost:8080/websocket'
        assert config_manager.get('federated.max_concurrent_tasks') == 3
        assert config_manager.get('logging.level') == 'INFO'
        
        # 测试默认值
        assert config_manager.get('nonexistent.key', 'default') == 'default'

    def test_set_config_value(self, temp_config_dir, sample_config):
        """测试设置配置值"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sample_config.copy()
        
        # 设置新值
        config_manager.set('websocket.server_url', 'ws://new-server:8080/websocket')
        config_manager.set('new.nested.key', 'new_value')
        
        assert config_manager.get('websocket.server_url') == 'ws://new-server:8080/websocket'
        assert config_manager.get('new.nested.key') == 'new_value'

    def test_validate_config_success(self, temp_config_dir, sample_config):
        """测试配置验证成功"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sample_config
        
        result = config_manager.validate_config()
        
        assert result['valid'] == True
        assert result['errors'] == []

    def test_validate_config_failure(self, temp_config_dir):
        """测试配置验证失败"""
        invalid_config = {
            'websocket': {
                # 缺少必需的server_url
                'connection_timeout': 30
            }
        }
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = invalid_config
        
        result = config_manager.validate_config()
        
        assert result['valid'] == False
        assert len(result['errors']) > 0

    def test_save_config(self, temp_config_dir, sample_config):
        """测试保存配置"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sample_config
        
        result = config_manager.save_config()
        
        assert result == True
        
        # 验证文件被创建
        config_file = temp_config_dir / 'test.yaml'
        assert config_file.exists()
        
        # 验证内容正确
        with open(config_file, 'r', encoding='utf-8') as f:
            saved_config = yaml.safe_load(f)
        assert saved_config == sample_config

    def test_reload_config(self, temp_config_dir, sample_config):
        """测试重新加载配置"""
        # 创建初始配置
        config_file = temp_config_dir / 'base.yaml'
        with open(config_file, 'w', encoding='utf-8') as f:
            yaml.dump(sample_config, f)
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.load_config()
        
        original_url = config_manager.get('websocket.server_url')
        
        # 修改配置文件
        sample_config['websocket']['server_url'] = 'ws://modified-server:8080/websocket'
        with open(config_file, 'w', encoding='utf-8') as f:
            yaml.dump(sample_config, f)
        
        # 重新加载
        result = config_manager.reload_config()
        
        assert result == True
        assert config_manager.get('websocket.server_url') != original_url
        assert config_manager.get('websocket.server_url') == 'ws://modified-server:8080/websocket'

    def test_environment_variable_substitution(self, temp_config_dir):
        """测试环境变量替换"""
        config_with_env = {
            'websocket': {
                'server_url': '${WEBSOCKET_SERVER_URL:ws://localhost:8080/websocket}',
                'connection_timeout': '${CONNECTION_TIMEOUT:30}'
            }
        }
        
        config_file = temp_config_dir / 'base.yaml'
        with open(config_file, 'w', encoding='utf-8') as f:
            yaml.dump(config_with_env, f)
        
        # 设置环境变量
        with patch.dict(os.environ, {
            'WEBSOCKET_SERVER_URL': 'ws://env-server:8080/websocket',
            'CONNECTION_TIMEOUT': '60'
        }):
            config_manager = ConfigManager(
                config_dir=str(temp_config_dir),
                environment='test'
            )
            config_manager.load_config()
            
            assert config_manager.get('websocket.server_url') == 'ws://env-server:8080/websocket'
            assert config_manager.get('websocket.connection_timeout') == 60

    def test_config_encryption_decryption(self, temp_config_dir):
        """测试配置加密和解密"""
        sensitive_config = {
            'database': {
                'password': 'secret_password',
                'api_key': 'secret_api_key'
            }
        }
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sensitive_config
        
        # 加密敏感配置
        encrypted_config = config_manager.encrypt_sensitive_data()
        
        assert encrypted_config != sensitive_config
        assert 'password' not in str(encrypted_config)
        
        # 解密配置
        decrypted_config = config_manager.decrypt_sensitive_data(encrypted_config)
        
        assert decrypted_config == sensitive_config

    def test_config_versioning(self, temp_config_dir, sample_config):
        """测试配置版本管理"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sample_config
        
        # 保存初始版本
        config_manager.save_config()
        version1 = config_manager.get_config_version()
        
        # 修改配置
        config_manager.set('websocket.server_url', 'ws://new-server:8080/websocket')
        config_manager.save_config()
        version2 = config_manager.get_config_version()
        
        assert version1 != version2
        
        # 回滚到之前版本
        result = config_manager.rollback_to_version(version1)
        
        assert result == True
        assert config_manager.get('websocket.server_url') == 'ws://localhost:8080/websocket'

    def test_config_schema_validation(self, temp_config_dir):
        """测试配置模式验证"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        
        # 定义配置模式
        schema = {
            'websocket': {
                'required': ['server_url'],
                'properties': {
                    'server_url': {'type': 'string', 'pattern': r'^wss?://'},
                    'connection_timeout': {'type': 'integer', 'minimum': 1}
                }
            }
        }
        
        config_manager.set_schema(schema)
        
        # 测试有效配置
        valid_config = {
            'websocket': {
                'server_url': 'ws://localhost:8080/websocket',
                'connection_timeout': 30
            }
        }
        config_manager.config = valid_config
        
        result = config_manager.validate_against_schema()
        assert result['valid'] == True
        
        # 测试无效配置
        invalid_config = {
            'websocket': {
                'server_url': 'invalid-url',  # 不符合模式
                'connection_timeout': -1      # 小于最小值
            }
        }
        config_manager.config = invalid_config
        
        result = config_manager.validate_against_schema()
        assert result['valid'] == False

    def test_config_merge_strategies(self, temp_config_dir):
        """测试配置合并策略"""
        base_config = {
            'websocket': {
                'server_url': 'ws://localhost:8080/websocket',
                'connection_timeout': 30
            },
            'logging': {
                'level': 'INFO'
            }
        }
        
        override_config = {
            'websocket': {
                'connection_timeout': 60,
                'heartbeat_interval': 30
            },
            'database': {
                'type': 'sqlite'
            }
        }
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        
        # 测试深度合并
        merged = config_manager.merge_configs(base_config, override_config, strategy='deep')
        
        assert merged['websocket']['server_url'] == 'ws://localhost:8080/websocket'  # 保留
        assert merged['websocket']['connection_timeout'] == 60  # 覆盖
        assert merged['websocket']['heartbeat_interval'] == 30  # 新增
        assert merged['logging']['level'] == 'INFO'  # 保留
        assert merged['database']['type'] == 'sqlite'  # 新增

    def test_config_change_notifications(self, temp_config_dir, sample_config):
        """测试配置变更通知"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sample_config.copy()
        
        # 注册变更监听器
        change_events = []
        
        def on_config_change(key, old_value, new_value):
            change_events.append((key, old_value, new_value))
        
        config_manager.register_change_listener(on_config_change)
        
        # 修改配置
        old_url = config_manager.get('websocket.server_url')
        new_url = 'ws://new-server:8080/websocket'
        config_manager.set('websocket.server_url', new_url)
        
        # 验证通知被触发
        assert len(change_events) == 1
        assert change_events[0] == ('websocket.server_url', old_url, new_url)

    def test_config_hot_reload(self, temp_config_dir, sample_config):
        """测试配置热重载"""
        # 创建配置文件
        config_file = temp_config_dir / 'base.yaml'
        with open(config_file, 'w', encoding='utf-8') as f:
            yaml.dump(sample_config, f)
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.load_config()
        
        # 启用热重载
        config_manager.enable_hot_reload(interval=0.1)
        
        original_url = config_manager.get('websocket.server_url')
        
        # 修改配置文件
        import time
        time.sleep(0.2)  # 等待文件监控
        
        sample_config['websocket']['server_url'] = 'ws://hot-reload-server:8080/websocket'
        with open(config_file, 'w', encoding='utf-8') as f:
            yaml.dump(sample_config, f)
        
        time.sleep(0.2)  # 等待热重载
        
        # 验证配置被自动更新
        new_url = config_manager.get('websocket.server_url')
        assert new_url != original_url
        
        # 清理
        config_manager.disable_hot_reload()

    def test_config_export_import(self, temp_config_dir, sample_config):
        """测试配置导出和导入"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sample_config
        
        # 导出配置
        export_file = temp_config_dir / 'exported_config.yaml'
        result = config_manager.export_config(str(export_file))
        
        assert result == True
        assert export_file.exists()
        
        # 创建新的配置管理器
        new_config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='prod'
        )
        
        # 导入配置
        result = new_config_manager.import_config(str(export_file))
        
        assert result == True
        assert new_config_manager.config == sample_config

    def test_config_performance(self, temp_config_dir, sample_config):
        """测试配置性能"""
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sample_config
        
        # 测试大量读取操作的性能
        import time
        
        start_time = time.time()
        for i in range(1000):
            config_manager.get('websocket.server_url')
        end_time = time.time()
        
        # 1000次读取应该在合理时间内完成
        assert end_time - start_time < 1.0

    def test_config_thread_safety(self, temp_config_dir, sample_config):
        """测试配置线程安全"""
        import threading
        
        config_manager = ConfigManager(
            config_dir=str(temp_config_dir),
            environment='test'
        )
        config_manager.config = sample_config.copy()
        
        results = []
        errors = []
        
        def read_config():
            try:
                for i in range(100):
                    value = config_manager.get('websocket.server_url')
                    results.append(value)
            except Exception as e:
                errors.append(e)
        
        def write_config():
            try:
                for i in range(100):
                    config_manager.set('websocket.server_url', f'ws://server-{i}:8080/websocket')
            except Exception as e:
                errors.append(e)
        
        # 创建多个读写线程
        threads = []
        for i in range(5):
            threads.append(threading.Thread(target=read_config))
            threads.append(threading.Thread(target=write_config))
        
        # 启动所有线程
        for thread in threads:
            thread.start()
        
        # 等待所有线程完成
        for thread in threads:
            thread.join()
        
        # 验证没有错误
        assert len(errors) == 0
        assert len(results) == 500  # 5个读线程 * 100次读取
