"""
pytest配置文件 - 测试框架配置和共享fixtures
"""

import pytest
import asyncio
import logging
import tempfile
import shutil
from pathlib import Path
from unittest.mock import Mock, MagicMock
import sys
import os

# 添加项目路径到sys.path
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / 'src'))

# 导入项目模块
from feduwacomm.ml.websocket.client import WebSocketClient
from feduwacomm.ml.websocket.message_router import MessageRouter
from feduwacomm.ml.websocket.task_manager import TaskManager
from feduwacomm.ml.websocket.task_context import TaskContext
from feduwacomm.ml.config.config_manager import ConfigManager
from feduwacomm.ml.utils.error_handler import ErrorHandler
from feduwacomm.ml.utils.monitor import Monitor


# 配置日志
logging.basicConfig(level=logging.DEBUG)


@pytest.fixture(scope="session")
def event_loop():
    """创建事件循环"""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
def temp_dir():
    """创建临时目录"""
    temp_path = tempfile.mkdtemp()
    yield Path(temp_path)
    shutil.rmtree(temp_path)


@pytest.fixture
def mock_websocket():
    """模拟WebSocket连接"""
    mock_ws = Mock()
    mock_ws.send = Mock()
    mock_ws.recv = Mock()
    mock_ws.close = Mock()
    mock_ws.ping = Mock(return_value=True)
    return mock_ws


@pytest.fixture
def test_config():
    """测试配置"""
    return {
        'websocket': {
            'server_url': 'ws://localhost:8080/websocket',
            'connection_timeout': 5,
            'heartbeat_interval': 10,
            'max_reconnect_attempts': 3,
            'reconnect_delay': 1
        },
        'federated': {
            'supported_algorithms': ['FEDERATED_AVERAGING'],
            'max_concurrent_tasks': 2,
            'default_local_epochs': 3,
            'default_batch_size': 16
        },
        'logging': {
            'level': 'DEBUG'
        }
    }


@pytest.fixture
def config_manager(temp_dir, test_config):
    """配置管理器fixture"""
    config_dir = temp_dir / 'config'
    config_dir.mkdir()
    
    # 创建测试配置文件
    import yaml
    with open(config_dir / 'base.yaml', 'w') as f:
        yaml.dump(test_config, f)
    
    return ConfigManager(str(config_dir), 'test')


@pytest.fixture
def mock_client():
    """模拟WebSocket客户端"""
    client = Mock(spec=WebSocketClient)
    client.vm_id = "test-vm-001"
    client.is_connected = True
    client.active_tasks = {}
    client.max_concurrent_tasks = 3
    client.heartbeat_interval = 30
    client._generate_message_id = Mock(return_value="test-msg-001")
    client._get_current_timestamp = Mock(return_value=1640995200000)
    client._send_message = Mock(return_value=True)
    client._get_resource_usage = Mock(return_value={
        'cpu_percent': 50.0,
        'memory_percent': 60.0,
        'disk_percent': 70.0
    })
    client._get_active_tasks_status = Mock(return_value={})
    client._get_vm_capabilities = Mock(return_value={
        'supported_algorithms': ['FEDERATED_AVERAGING'],
        'max_concurrent_tasks': 3
    })
    client._get_performance_metrics = Mock(return_value={})
    return client


@pytest.fixture
def message_router(mock_client):
    """消息路由器fixture"""
    return MessageRouter(mock_client)


@pytest.fixture
def task_manager(mock_client):
    """任务管理器fixture"""
    return TaskManager(mock_client)


@pytest.fixture
def error_handler(mock_client):
    """错误处理器fixture"""
    return ErrorHandler(mock_client)


@pytest.fixture
def monitor(mock_client):
    """监控器fixture"""
    config = {
        'enabled': True,
        'interval': 1,  # 1秒间隔用于测试
        'retention_hours': 1,
        'debug': True
    }
    return Monitor(mock_client, config)


@pytest.fixture
def sample_task_data():
    """示例任务数据"""
    return {
        'taskId': 'test-task-001',
        'federatedAlgorithm': 'FEDERATED_AVERAGING',
        'totalRounds': 5,
        'localTrainingConfig': {
            'datasetId': 'test-dataset-001',
            'epochs': 3,
            'batchSize': 16,
            'learningRate': 0.01
        },
        'modelConfig': {
            'modelType': 'RandomForest',
            'parameters': {
                'n_estimators': 100,
                'max_depth': 10
            }
        }
    }


@pytest.fixture
def sample_message():
    """示例消息"""
    return {
        'type': 'FEDERATED_TASK_START',
        'id': 'msg-001',
        'timestamp': 1640995200000,
        'vmId': 'test-vm-001',
        'data': {
            'taskId': 'test-task-001',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 5
        }
    }


@pytest.fixture
def sample_global_model():
    """示例全局模型"""
    return {
        'model_type': 'RandomForest',
        'parameters': {
            'n_estimators': 100,
            'max_depth': 10
        },
        'weights': [0.1, 0.2, 0.3, 0.4],
        'metadata': {
            'version': '1.0',
            'created_at': '2024-01-01T00:00:00Z'
        }
    }


@pytest.fixture
def sample_training_data():
    """示例训练数据"""
    import numpy as np
    return {
        'features': np.random.rand(100, 10).tolist(),
        'labels': np.random.randint(0, 2, 100).tolist(),
        'metadata': {
            'feature_names': [f'feature_{i}' for i in range(10)],
            'label_name': 'target'
        }
    }


# 测试标记
def pytest_configure(config):
    """配置pytest标记"""
    config.addinivalue_line(
        "markers", "unit: 单元测试"
    )
    config.addinivalue_line(
        "markers", "integration: 集成测试"
    )
    config.addinivalue_line(
        "markers", "e2e: 端到端测试"
    )
    config.addinivalue_line(
        "markers", "slow: 慢速测试"
    )
    config.addinivalue_line(
        "markers", "network: 需要网络连接的测试"
    )


# 测试数据清理
@pytest.fixture(autouse=True)
def cleanup_test_data():
    """自动清理测试数据"""
    yield
    # 测试后清理工作
    pass


# 异步测试支持
@pytest.fixture
def async_mock():
    """异步模拟对象"""
    async def async_return(result):
        return result
    
    mock = Mock()
    mock.side_effect = async_return
    return mock


# 性能测试工具
@pytest.fixture
def performance_timer():
    """性能计时器"""
    import time
    
    class Timer:
        def __init__(self):
            self.start_time = None
            self.end_time = None
        
        def start(self):
            self.start_time = time.time()
        
        def stop(self):
            self.end_time = time.time()
            return self.end_time - self.start_time if self.start_time else 0
        
        @property
        def elapsed(self):
            if self.start_time and self.end_time:
                return self.end_time - self.start_time
            return 0
    
    return Timer()


# 数据库测试支持
@pytest.fixture
def test_database():
    """测试数据库"""
    import sqlite3
    import tempfile
    
    # 创建临时数据库
    db_file = tempfile.NamedTemporaryFile(delete=False, suffix='.db')
    db_path = db_file.name
    db_file.close()
    
    # 初始化数据库
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 创建测试表
    cursor.execute('''
        CREATE TABLE test_data (
            id INTEGER PRIMARY KEY,
            feature1 REAL,
            feature2 REAL,
            label INTEGER
        )
    ''')
    
    # 插入测试数据
    test_data = [(i, i * 0.1, i * 0.2, i % 2) for i in range(100)]
    cursor.executemany('INSERT INTO test_data (id, feature1, feature2, label) VALUES (?, ?, ?, ?)', test_data)
    
    conn.commit()
    conn.close()
    
    yield db_path
    
    # 清理
    os.unlink(db_path)


# 网络模拟
@pytest.fixture
def mock_server():
    """模拟服务器"""
    class MockServer:
        def __init__(self):
            self.messages = []
            self.connected_clients = []
            self.is_running = False
        
        def start(self):
            self.is_running = True
        
        def stop(self):
            self.is_running = False
            self.connected_clients.clear()
        
        def add_client(self, client_id):
            if client_id not in self.connected_clients:
                self.connected_clients.append(client_id)
        
        def remove_client(self, client_id):
            if client_id in self.connected_clients:
                self.connected_clients.remove(client_id)
        
        def broadcast_message(self, message):
            self.messages.append(message)
            return len(self.connected_clients)
        
        def get_messages(self):
            return self.messages.copy()
        
        def clear_messages(self):
            self.messages.clear()
    
    return MockServer()


# 日志捕获
@pytest.fixture
def log_capture():
    """日志捕获器"""
    import logging
    from io import StringIO
    
    log_stream = StringIO()
    handler = logging.StreamHandler(log_stream)
    handler.setLevel(logging.DEBUG)
    
    # 添加到根日志器
    root_logger = logging.getLogger()
    root_logger.addHandler(handler)
    
    yield log_stream
    
    # 清理
    root_logger.removeHandler(handler)


# 环境变量管理
@pytest.fixture
def env_vars():
    """环境变量管理器"""
    original_env = os.environ.copy()
    
    class EnvManager:
        def set(self, key, value):
            os.environ[key] = str(value)
        
        def get(self, key, default=None):
            return os.environ.get(key, default)
        
        def delete(self, key):
            if key in os.environ:
                del os.environ[key]
        
        def clear_test_vars(self):
            # 清理测试相关的环境变量
            test_keys = [k for k in os.environ.keys() if k.startswith('FEDUWA_TEST_')]
            for key in test_keys:
                del os.environ[key]
    
    yield EnvManager()
    
    # 恢复原始环境变量
    os.environ.clear()
    os.environ.update(original_env)
