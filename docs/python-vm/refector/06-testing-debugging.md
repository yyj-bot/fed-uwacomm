# 测试和调试指导

本文档提供 Python VM WebSocket 协议重构的全面测试策略和调试方法，确保重构后系统的稳定性和可靠性。

## 1. 测试策略概述

### 1.1 测试金字塔

```
        ┌─────────────┐
        │  E2E 测试   │ 10%
        │   (集成)    │
    ┌───┴─────────────┴───┐
    │    集成测试 (API)    │ 20%
    │   (组件间通信)       │
┌───┴─────────────────────┴───┐
│        单元测试              │ 70%
│    (类和方法级别)           │
└─────────────────────────────┘
```

#### 测试分层策略
- **单元测试 (70%)**: 覆盖核心逻辑、消息处理、任务管理
- **集成测试 (20%)**: WebSocket通信、数据库交互、算法集成
- **端到端测试 (10%)**: 完整联邦学习流程验证

### 1.2 测试覆盖率目标

#### 覆盖率要求
- **核心业务逻辑**: ≥95%
- **WebSocket通信**: ≥90%
- **消息处理**: ≥95%
- **任务管理**: ≥90%
- **错误处理**: ≥85%
- **总体覆盖率**: ≥90%

## 2. 单元测试实现

### 2.1 WebSocket客户端测试

#### 连接管理测试
```python
import unittest
from unittest.mock import Mock, patch, MagicMock
import threading
import time
import json

from feduwacomm.websocket.client import FederatedLearningClient
from feduwacomm.websocket.exceptions import ConnectionError, AuthenticationError

class TestFederatedLearningClient(unittest.TestCase):

    def setUp(self):
        self.server_url = "ws://localhost:8080/websocket"
        self.access_token = "test_token"
        self.vm_id = "test_vm_001"
        self.config = {
            'federated_learning': {
                'performance': {
                    'max_concurrent_tasks': 3
                }
            }
        }

    @patch('websocket.WebSocketApp')
    def test_client_initialization(self, mock_ws_app):
        """测试客户端初始化"""
        client = FederatedLearningClient(
            self.server_url, self.access_token, self.vm_id, self.config
        )

        self.assertEqual(client.server_url, self.server_url)
        self.assertEqual(client.access_token, self.access_token)
        self.assertEqual(client.vm_id, self.vm_id)
        self.assertFalse(client.connected)
        self.assertEqual(len(client.active_tasks), 0)

    @patch('websocket.WebSocketApp')
    def test_connection_success(self, mock_ws_app):
        """测试连接成功"""
        mock_ws = Mock()
        mock_ws_app.return_value = mock_ws

        client = FederatedLearningClient(
            self.server_url, self.access_token, self.vm_id, self.config
        )

        # 模拟连接成功
        client._on_open(mock_ws)

        self.assertTrue(client.connected)
        mock_ws.send.assert_called_once()

        # 验证发送的连接消息
        sent_data = mock_ws.send.call_args[0][0]
        connect_msg = json.loads(sent_data)
        self.assertEqual(connect_msg['type'], 'CONNECT')
        self.assertEqual(connect_msg['data']['vmId'], self.vm_id)

    @patch('websocket.WebSocketApp')
    def test_connection_failure(self, mock_ws_app):
        """测试连接失败"""
        mock_ws = Mock()
        mock_ws_app.return_value = mock_ws

        client = FederatedLearningClient(
            self.server_url, self.access_token, self.vm_id, self.config
        )

        # 模拟连接错误
        error = Exception("Connection failed")
        client._on_error(mock_ws, error)

        self.assertFalse(client.connected)

    @patch('websocket.WebSocketApp')
    def test_message_handling(self, mock_ws_app):
        """测试消息处理"""
        mock_ws = Mock()
        mock_ws_app.return_value = mock_ws

        client = FederatedLearningClient(
            self.server_url, self.access_token, self.vm_id, self.config
        )
        client.connected = True

        # 模拟接收CONNECT_ACK消息
        connect_ack_msg = json.dumps({
            'type': 'CONNECT_ACK',
            'data': {'status': 'success', 'serverVersion': '1.4.0'}
        })

        client._on_message(mock_ws, connect_ack_msg)

        # 验证连接确认处理
        self.assertTrue(client.connected)

    def test_heartbeat_mechanism(self):
        """测试心跳机制"""
        with patch('websocket.WebSocketApp') as mock_ws_app:
            mock_ws = Mock()
            mock_ws_app.return_value = mock_ws

            client = FederatedLearningClient(
                self.server_url, self.access_token, self.vm_id, self.config
            )
            client.connected = True
            client.heartbeat_interval = 0.1  # 快速心跳用于测试

            # 启动心跳
            client._start_heartbeat()
            time.sleep(0.3)  # 等待几次心跳
            client._stop_heartbeat()

            # 验证心跳消息发送
            self.assertTrue(mock_ws.send.call_count >= 2)

            # 检查心跳消息格式
            heartbeat_calls = [call for call in mock_ws.send.call_args_list
                             if 'HEARTBEAT' in str(call)]
            self.assertTrue(len(heartbeat_calls) >= 2)

    @patch('websocket.WebSocketApp')
    def test_reconnection_logic(self, mock_ws_app):
        """测试重连逻辑"""
        mock_ws = Mock()
        mock_ws_app.return_value = mock_ws

        client = FederatedLearningClient(
            self.server_url, self.access_token, self.vm_id, self.config
        )

        # 模拟连接断开
        client.connected = True
        client._on_close(mock_ws, 1006, "Connection lost")

        self.assertFalse(client.connected)

        # 验证重连尝试
        time.sleep(0.1)
        self.assertTrue(mock_ws_app.call_count >= 1)
```

### 2.2 消息路由测试

#### MessageRouter测试
```python
class TestMessageRouter(unittest.TestCase):

    def setUp(self):
        self.mock_client = Mock()
        self.router = MessageRouter(self.mock_client)

    def test_task_message_routing(self):
        """测试任务消息路由"""
        task_message = {
            'type': 'ROUND_START',
            'data': {
                'taskId': 'test_task_001',
                'roundNumber': 1,
                'globalModel': 'base64_encoded_model'
            }
        }

        # 模拟任务上下文
        mock_context = Mock()
        self.mock_client.active_tasks = {'test_task_001': mock_context}

        result = self.router.route_message(task_message)

        self.assertTrue(result)
        # 验证消息被路由到正确的任务上下文
        self.assertEqual(mock_context.handle_message.call_count, 1)

    def test_global_message_routing(self):
        """测试全局消息路由"""
        global_message = {
            'type': 'FEDERATED_TASK_START',
            'data': {
                'taskId': 'new_task_001',
                'algorithm': 'FEDERATED_AVERAGING',
                'config': {}
            }
        }

        result = self.router.route_message(global_message)

        self.assertTrue(result)
        # 验证全局消息处理
        self.mock_client.handle_global_message.assert_called_once_with(global_message)

    def test_invalid_message_handling(self):
        """测试无效消息处理"""
        invalid_messages = [
            {},  # 空消息
            {'type': 'INVALID_TYPE'},  # 无效类型
            {'data': {}},  # 缺少type字段
            'not_a_dict',  # 非字典类型
        ]

        for invalid_msg in invalid_messages:
            with self.subTest(message=invalid_msg):
                result = self.router.route_message(invalid_msg)
                self.assertFalse(result)

    def test_message_type_handlers(self):
        """测试各种消息类型处理器"""
        message_types = [
            'CONNECT_ACK', 'FEDERATED_TASK_START', 'FEDERATED_TASK_STOP',
            'ROUND_START', 'ROUND_COMPLETE', 'MODEL_AGGREGATE',
            'HEARTBEAT_ACK', 'ERROR'
        ]

        for msg_type in message_types:
            with self.subTest(message_type=msg_type):
                # 验证处理器存在
                handler = self.router._get_handler(msg_type)
                self.assertIsNotNone(handler)
                self.assertTrue(callable(handler))
```

### 2.3 任务上下文测试

#### TaskContext测试
```python
class TestTaskContext(unittest.TestCase):

    def setUp(self):
        self.task_id = "test_task_001"
        self.config = {
            'algorithm': 'FEDERATED_AVERAGING',
            'localEpochs': 5,
            'batchSize': 32,
            'dataConfig': {
                'type': 'ACOUSTIC',
                'datasetId': 'test_dataset'
            }
        }
        self.mock_client = Mock()

    def test_context_initialization(self):
        """测试上下文初始化"""
        context = TaskContext(self.task_id, self.config, self.mock_client)

        self.assertEqual(context.task_id, self.task_id)
        self.assertEqual(context.status, 'INITIALIZING')
        self.assertIsNone(context.executor)
        self.assertIsNone(context.training_thread)

    @patch('feduwacomm.federated.FederatedTrainingExecutor')
    def test_context_start(self, mock_executor_class):
        """测试上下文启动"""
        mock_executor = Mock()
        mock_executor_class.return_value = mock_executor

        context = TaskContext(self.task_id, self.config, self.mock_client)
        context.start()

        self.assertEqual(context.status, 'READY')
        self.assertIsNotNone(context.executor)
        mock_executor.initialize.assert_called_once()

    def test_context_stop(self):
        """测试上下文停止"""
        context = TaskContext(self.task_id, self.config, self.mock_client)
        context.status = 'TRAINING'
        context.training_thread = Mock()

        context.stop()

        self.assertEqual(context.status, 'STOPPED')
        self.assertIsNone(context.training_thread)

    def test_round_execution(self):
        """测试轮次执行"""
        context = TaskContext(self.task_id, self.config, self.mock_client)
        context.executor = Mock()

        # 模拟轮次开始消息
        round_message = {
            'type': 'ROUND_START',
            'data': {
                'roundNumber': 1,
                'globalModel': b'mock_model_data',
                'parameters': {'learningRate': 0.01}
            }
        }

        context.handle_message(round_message)

        # 验证执行器被调用
        self.assertIsNotNone(context.training_thread)
        self.assertEqual(context.current_round, 1)

    def test_error_handling(self):
        """测试错误处理"""
        context = TaskContext(self.task_id, self.config, self.mock_client)
        context.executor = Mock()
        context.executor.execute_round.side_effect = Exception("Training error")

        # 模拟训练错误
        round_message = {
            'type': 'ROUND_START',
            'data': {
                'roundNumber': 1,
                'globalModel': b'mock_model_data'
            }
        }

        context.handle_message(round_message)

        # 等待错误处理
        time.sleep(0.1)

        # 验证错误状态
        self.assertEqual(context.status, 'ERROR')
```

### 2.4 联邦学习执行器测试

#### FederatedTrainingExecutor测试
```python
class TestFederatedTrainingExecutor(unittest.TestCase):

    def setUp(self):
        self.task_id = "test_task_001"
        self.config = {
            'algorithm': 'FEDERATED_AVERAGING',
            'localEpochs': 3,
            'batchSize': 16,
            'learningRate': 0.01,
            'dataConfig': {
                'type': 'ACOUSTIC',
                'datasetId': 'test_dataset'
            }
        }

    @patch('feduwacomm.database.DatabaseManager')
    def test_executor_initialization(self, mock_db_manager):
        """测试执行器初始化"""
        # 模拟数据库返回
        mock_db = Mock()
        mock_db.get_training_data_by_task.return_value = [
            {'features': [1, 2, 3], 'label': 0},
            {'features': [4, 5, 6], 'label': 1}
        ]
        mock_db_manager.return_value = mock_db

        executor = FederatedTrainingExecutor(self.task_id, self.config)
        executor.initialize()

        self.assertIsNotNone(executor.training_data)
        self.assertIsNotNone(executor.trainer)
        self.assertEqual(executor.algorithm, 'FEDERATED_AVERAGING')

    def test_algorithm_selection(self):
        """测试算法选择"""
        algorithms = ['FEDERATED_AVERAGING', 'FEDERATED_PROXIMAL', 'FEDERATED_NOVA']

        for algorithm in algorithms:
            with self.subTest(algorithm=algorithm):
                config = self.config.copy()
                config['algorithm'] = algorithm

                executor = FederatedTrainingExecutor(self.task_id, config)
                executor._create_trainer()

                self.assertIsNotNone(executor.trainer)

    @patch('feduwacomm.ml.algorithms.FedAvgTrainer')
    def test_round_execution(self, mock_trainer_class):
        """测试轮次执行"""
        mock_trainer = Mock()
        mock_trainer.train.return_value = {'loss': [0.5], 'accuracy': [0.8]}
        mock_trainer.get_gradients.return_value = {'layer1': [0.1, 0.2]}
        mock_trainer_class.return_value = mock_trainer

        executor = FederatedTrainingExecutor(self.task_id, self.config)
        executor.trainer = mock_trainer
        executor.training_data = [{'features': [1, 2, 3], 'label': 0}]
        executor.test_data = [{'features': [4, 5, 6], 'label': 1}]

        # 模拟评估器
        executor.evaluator = Mock()
        executor.evaluator.evaluate.return_value = {'accuracy': 0.85, 'loss': 0.4}

        global_model_data = b'mock_model_data'
        round_params = {'learningRate': 0.02}

        result = executor.execute_round(global_model_data, round_params)

        self.assertIn('gradients', result)
        self.assertIn('evaluation', result)
        self.assertIn('training_stats', result)
        mock_trainer.train.assert_called_once()

    def test_gradient_serialization(self):
        """测试梯度序列化"""
        executor = FederatedTrainingExecutor(self.task_id, self.config)

        # 模拟梯度数据
        gradients = {
            'layer1': [0.1, 0.2, 0.3],
            'layer2': [0.4, 0.5, 0.6]
        }

        serialized = executor._serialize_gradients(gradients)
        self.assertIsInstance(serialized, bytes)

        # 验证可以反序列化
        import pickle
        deserialized = pickle.loads(serialized)
        self.assertEqual(deserialized, gradients)
```

## 3. 集成测试实现

### 3.1 WebSocket通信集成测试

#### 端到端通信测试
```python
import asyncio
import websockets
import json
import threading
import time
from unittest import TestCase

class TestWebSocketIntegration(TestCase):

    @classmethod
    def setUpClass(cls):
        """启动模拟WebSocket服务器"""
        cls.server_port = 8765
        cls.server_thread = threading.Thread(target=cls._run_mock_server)
        cls.server_thread.daemon = True
        cls.server_thread.start()
        time.sleep(1)  # 等待服务器启动

    @classmethod
    def _run_mock_server(cls):
        """运行模拟WebSocket服务器"""
        async def handle_client(websocket, path):
            try:
                async for message in websocket:
                    data = json.loads(message)

                    # 模拟服务器响应
                    if data['type'] == 'CONNECT':
                        response = {
                            'type': 'CONNECT_ACK',
                            'data': {'status': 'success', 'serverVersion': '1.4.0'}
                        }
                        await websocket.send(json.dumps(response))

                    elif data['type'] == 'HEARTBEAT':
                        response = {
                            'type': 'HEARTBEAT_ACK',
                            'data': {'timestamp': time.time()}
                        }
                        await websocket.send(json.dumps(response))

            except websockets.exceptions.ConnectionClosed:
                pass

        start_server = websockets.serve(handle_client, "localhost", cls.server_port)
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        loop.run_until_complete(start_server)
        loop.run_forever()

    def test_client_server_connection(self):
        """测试客户端与服务器连接"""
        client = FederatedLearningClient(
            server_url=f"ws://localhost:{self.server_port}",
            access_token="test_token",
            vm_id="test_vm_001",
            config={}
        )

        # 连接到模拟服务器
        client.connect()

        # 等待连接建立
        time.sleep(1)

        self.assertTrue(client.connected)

        # 断开连接
        client.disconnect()
        self.assertFalse(client.connected)

    def test_heartbeat_communication(self):
        """测试心跳通信"""
        client = FederatedLearningClient(
            server_url=f"ws://localhost:{self.server_port}",
            access_token="test_token",
            vm_id="test_vm_001",
            config={}
        )
        client.heartbeat_interval = 1  # 1秒心跳间隔

        client.connect()
        time.sleep(3)  # 等待几次心跳

        # 验证心跳正常
        self.assertTrue(client.connected)

        client.disconnect()
```

### 3.2 数据库集成测试

#### 数据访问集成测试
```python
import tempfile
import os
from feduwacomm.database import DatabaseManager

class TestDatabaseIntegration(TestCase):

    def setUp(self):
        """设置测试数据库"""
        self.test_db_file = tempfile.mktemp(suffix='.db')
        self.db_manager = DatabaseManager(db_path=self.test_db_file)
        self._create_test_data()

    def tearDown(self):
        """清理测试数据库"""
        if os.path.exists(self.test_db_file):
            os.remove(self.test_db_file)

    def _create_test_data(self):
        """创建测试数据"""
        # 插入测试训练数据
        test_data = [
            {
                'task_id': 'test_task_001',
                'features': [1.0, 2.0, 3.0],
                'label': 0,
                'data_type': 'ACOUSTIC'
            },
            {
                'task_id': 'test_task_001',
                'features': [4.0, 5.0, 6.0],
                'label': 1,
                'data_type': 'ACOUSTIC'
            }
        ]

        for data in test_data:
            self.db_manager.insert_training_data(data)

    def test_training_data_retrieval(self):
        """测试训练数据检索"""
        data = self.db_manager.get_training_data_by_task('test_task_001')

        self.assertEqual(len(data), 2)
        self.assertEqual(data[0]['label'], 0)
        self.assertEqual(data[1]['label'], 1)

    def test_task_data_isolation(self):
        """测试任务数据隔离"""
        # 为另一个任务插入数据
        other_task_data = {
            'task_id': 'test_task_002',
            'features': [7.0, 8.0, 9.0],
            'label': 1,
            'data_type': 'ACOUSTIC'
        }
        self.db_manager.insert_training_data(other_task_data)

        # 验证数据隔离
        task1_data = self.db_manager.get_training_data_by_task('test_task_001')
        task2_data = self.db_manager.get_training_data_by_task('test_task_002')

        self.assertEqual(len(task1_data), 2)
        self.assertEqual(len(task2_data), 1)

    def test_concurrent_data_access(self):
        """测试并发数据访问"""
        import threading

        results = []
        errors = []

        def access_data(task_id):
            try:
                data = self.db_manager.get_training_data_by_task(task_id)
                results.append(len(data))
            except Exception as e:
                errors.append(e)

        # 创建多个线程同时访问数据
        threads = []
        for i in range(10):
            thread = threading.Thread(target=access_data, args=('test_task_001',))
            threads.append(thread)
            thread.start()

        # 等待所有线程完成
        for thread in threads:
            thread.join()

        # 验证结果
        self.assertEqual(len(errors), 0)
        self.assertEqual(len(results), 10)
        self.assertTrue(all(r == 2 for r in results))
```

### 3.3 算法集成测试

#### 联邦学习算法集成测试
```python
class TestAlgorithmIntegration(TestCase):

    def setUp(self):
        """设置测试环境"""
        self.test_data = [
            {'features': [1.0, 2.0], 'label': 0},
            {'features': [3.0, 4.0], 'label': 1},
            {'features': [5.0, 6.0], 'label': 0},
            {'features': [7.0, 8.0], 'label': 1},
        ]

    def test_fedavg_algorithm_integration(self):
        """测试FedAvg算法集成"""
        config = {
            'algorithm': 'FEDERATED_AVERAGING',
            'localEpochs': 2,
            'batchSize': 2,
            'learningRate': 0.01
        }

        executor = FederatedTrainingExecutor('test_task', config)
        executor.training_data = self.test_data
        executor.test_data = self.test_data
        executor._create_trainer()
        executor._create_evaluator()

        # 模拟全局模型
        global_model = self._create_mock_model()
        global_model_data = executor._serialize_model(global_model)

        # 执行一轮训练
        result = executor.execute_round(global_model_data, {})

        # 验证结果
        self.assertIn('gradients', result)
        self.assertIn('evaluation', result)
        self.assertIn('training_stats', result)
        self.assertGreater(result['samples_count'], 0)

    def test_multiple_algorithms_comparison(self):
        """测试多种算法对比"""
        algorithms = ['FEDERATED_AVERAGING', 'FEDERATED_PROXIMAL']
        results = {}

        for algorithm in algorithms:
            config = {
                'algorithm': algorithm,
                'localEpochs': 2,
                'batchSize': 2,
                'learningRate': 0.01
            }

            if algorithm == 'FEDERATED_PROXIMAL':
                config['algorithmParams'] = {'proximalTerm': 0.01}

            executor = FederatedTrainingExecutor(f'test_task_{algorithm}', config)
            executor.training_data = self.test_data
            executor.test_data = self.test_data
            executor._create_trainer()
            executor._create_evaluator()

            # 执行训练
            global_model = self._create_mock_model()
            global_model_data = executor._serialize_model(global_model)
            result = executor.execute_round(global_model_data, {})

            results[algorithm] = result

        # 验证所有算法都产生了结果
        for algorithm in algorithms:
            self.assertIn(algorithm, results)
            self.assertIn('evaluation', results[algorithm])

    def _create_mock_model(self):
        """创建模拟模型"""
        # 创建简单的线性模型用于测试
        class MockModel:
            def __init__(self):
                self.weights = {'w1': [0.1, 0.2], 'b1': [0.0]}

        return MockModel()
```

## 4. 端到端测试

### 4.1 完整流程测试

#### 联邦学习完整流程测试
```python
class TestFederatedLearningE2E(TestCase):

    @classmethod
    def setUpClass(cls):
        """设置端到端测试环境"""
        cls._setup_test_database()
        cls._start_mock_server()

    def test_complete_federated_learning_flow(self):
        """测试完整联邦学习流程"""
        # 1. 创建客户端
        client = FederatedLearningClient(
            server_url="ws://localhost:8765",
            access_token="test_token",
            vm_id="test_vm_001",
            config=self._get_test_config()
        )

        # 2. 连接到服务器
        client.connect()
        self.assertTrue(client.connected)

        # 3. 模拟接收任务启动消息
        task_config = {
            'taskId': 'e2e_test_task',
            'algorithm': 'FEDERATED_AVERAGING',
            'localEpochs': 2,
            'totalRounds': 3,
            'dataConfig': {
                'type': 'ACOUSTIC',
                'datasetId': 'test_dataset'
            }
        }

        task_start_msg = {
            'type': 'FEDERATED_TASK_START',
            'data': task_config
        }

        client.message_router.route_message(task_start_msg)

        # 验证任务启动
        self.assertIn('e2e_test_task', client.active_tasks)

        # 4. 执行多轮训练
        for round_num in range(1, 4):
            # 发送轮次开始消息
            round_start_msg = {
                'type': 'ROUND_START',
                'data': {
                    'taskId': 'e2e_test_task',
                    'roundNumber': round_num,
                    'globalModel': self._create_mock_model_data(),
                    'parameters': {'learningRate': 0.01}
                }
            }

            client.message_router.route_message(round_start_msg)

            # 等待训练完成
            time.sleep(2)

            # 验证轮次完成
            task_context = client.active_tasks['e2e_test_task']
            self.assertEqual(task_context.current_round, round_num)

        # 5. 任务完成
        task_complete_msg = {
            'type': 'FEDERATED_TASK_COMPLETE',
            'data': {'taskId': 'e2e_test_task'}
        }

        client.message_router.route_message(task_complete_msg)

        # 验证任务清理
        time.sleep(1)
        self.assertNotIn('e2e_test_task', client.active_tasks)

        # 6. 断开连接
        client.disconnect()
        self.assertFalse(client.connected)

    def test_multi_task_concurrent_execution(self):
        """测试多任务并发执行"""
        client = FederatedLearningClient(
            server_url="ws://localhost:8765",
            access_token="test_token",
            vm_id="test_vm_001",
            config=self._get_test_config()
        )

        client.connect()

        # 启动3个并发任务
        task_ids = ['concurrent_task_1', 'concurrent_task_2', 'concurrent_task_3']

        for task_id in task_ids:
            task_config = {
                'taskId': task_id,
                'algorithm': 'FEDERATED_AVERAGING',
                'localEpochs': 1,
                'dataConfig': {
                    'type': 'ACOUSTIC',
                    'datasetId': 'test_dataset'
                }
            }

            task_start_msg = {
                'type': 'FEDERATED_TASK_START',
                'data': task_config
            }

            client.message_router.route_message(task_start_msg)

        # 验证所有任务都启动
        for task_id in task_ids:
            self.assertIn(task_id, client.active_tasks)

        # 同时执行所有任务的训练轮次
        for task_id in task_ids:
            round_start_msg = {
                'type': 'ROUND_START',
                'data': {
                    'taskId': task_id,
                    'roundNumber': 1,
                    'globalModel': self._create_mock_model_data()
                }
            }

            client.message_router.route_message(round_start_msg)

        # 等待所有任务完成
        time.sleep(3)

        # 验证所有任务都正在运行
        for task_id in task_ids:
            task_context = client.active_tasks[task_id]
            self.assertEqual(task_context.current_round, 1)

        client.disconnect()

    def _get_test_config(self):
        """获取测试配置"""
        return {
            'federated_learning': {
                'performance': {
                    'max_concurrent_tasks': 5,
                    'max_memory_mb': 1024
                }
            }
        }

    def _create_mock_model_data(self):
        """创建模拟模型数据"""
        import pickle
        mock_model = {'weights': [0.1, 0.2, 0.3], 'bias': 0.0}
        return pickle.dumps(mock_model)
```

### 4.2 压力测试

#### 并发和性能测试
```python
class TestPerformanceAndStress(TestCase):

    def test_high_frequency_messages(self):
        """测试高频消息处理"""
        client = FederatedLearningClient(
            server_url="ws://localhost:8765",
            access_token="test_token",
            vm_id="test_vm_001",
            config={}
        )

        message_count = 0
        processed_count = 0

        def count_processed_messages(msg):
            nonlocal processed_count
            processed_count += 1

        # 监听消息处理
        original_route = client.message_router.route_message
        def counting_route(msg):
            count_processed_messages(msg)
            return original_route(msg)

        client.message_router.route_message = counting_route

        client.connect()

        # 发送大量心跳消息
        for i in range(1000):
            heartbeat_msg = {
                'type': 'HEARTBEAT_ACK',
                'data': {'timestamp': time.time()}
            }
            client.message_router.route_message(heartbeat_msg)
            message_count += 1

        # 等待处理完成
        time.sleep(2)

        # 验证所有消息都被处理
        self.assertEqual(processed_count, message_count)

        client.disconnect()

    def test_memory_usage_under_load(self):
        """测试负载下的内存使用"""
        import psutil
        import gc

        process = psutil.Process()
        initial_memory = process.memory_info().rss

        client = FederatedLearningClient(
            server_url="ws://localhost:8765",
            access_token="test_token",
            vm_id="test_vm_001",
            config={}
        )

        client.connect()

        # 创建多个大型任务
        for i in range(50):
            large_task_config = {
                'taskId': f'memory_test_task_{i}',
                'algorithm': 'FEDERATED_AVERAGING',
                'dataConfig': {
                    'type': 'ACOUSTIC',
                    'datasetId': 'large_dataset'
                }
            }

            task_start_msg = {
                'type': 'FEDERATED_TASK_START',
                'data': large_task_config
            }

            client.message_router.route_message(task_start_msg)

        # 检查内存使用
        peak_memory = process.memory_info().rss
        memory_increase = (peak_memory - initial_memory) / (1024 * 1024)  # MB

        # 清理任务
        for i in range(50):
            task_stop_msg = {
                'type': 'FEDERATED_TASK_STOP',
                'data': {'taskId': f'memory_test_task_{i}'}
            }
            client.message_router.route_message(task_stop_msg)

        # 强制垃圾回收
        gc.collect()
        time.sleep(1)

        final_memory = process.memory_info().rss
        memory_after_cleanup = (final_memory - initial_memory) / (1024 * 1024)  # MB

        # 验证内存泄漏控制在合理范围内
        self.assertLess(memory_after_cleanup, memory_increase * 0.1)  # 清理后内存增长应小于峰值的10%

        client.disconnect()
```

## 5. 调试工具和技术

### 5.1 日志和监控

#### 日志配置
```python
import logging
import json
import time
from datetime import datetime

class FederatedLearningLogger:
    """联邦学习专用日志器"""

    def __init__(self, name: str, log_level: str = 'INFO'):
        self.logger = logging.getLogger(name)
        self.logger.setLevel(getattr(logging, log_level.upper()))

        # 创建格式化器
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )

        # 文件处理器
        file_handler = logging.FileHandler('federated_learning.log')
        file_handler.setFormatter(formatter)
        self.logger.addHandler(file_handler)

        # 控制台处理器
        console_handler = logging.StreamHandler()
        console_handler.setFormatter(formatter)
        self.logger.addHandler(console_handler)

    def log_message(self, direction: str, message: dict, task_id: str = None):
        """记录消息日志"""
        log_data = {
            'timestamp': datetime.now().isoformat(),
            'direction': direction,  # 'sent' or 'received'
            'message_type': message.get('type', 'UNKNOWN'),
            'task_id': task_id or message.get('data', {}).get('taskId'),
            'message_size': len(json.dumps(message)),
            'message': message
        }

        self.logger.info(f"MESSAGE {direction}: {json.dumps(log_data, indent=2)}")

    def log_training_metrics(self, task_id: str, round_num: int, metrics: dict):
        """记录训练指标"""
        log_data = {
            'timestamp': datetime.now().isoformat(),
            'task_id': task_id,
            'round_number': round_num,
            'metrics': metrics
        }

        self.logger.info(f"TRAINING_METRICS: {json.dumps(log_data, indent=2)}")

    def log_error(self, error: Exception, context: str, task_id: str = None):
        """记录错误日志"""
        log_data = {
            'timestamp': datetime.now().isoformat(),
            'error_type': type(error).__name__,
            'error_message': str(error),
            'context': context,
            'task_id': task_id
        }

        self.logger.error(f"ERROR: {json.dumps(log_data, indent=2)}")
```

#### 性能监控
```python
import time
import threading
from collections import defaultdict, deque

class PerformanceMonitor:
    """性能监控器"""

    def __init__(self):
        self.metrics = defaultdict(lambda: deque(maxlen=1000))
        self.counters = defaultdict(int)
        self.timers = {}
        self.lock = threading.Lock()

    def start_timer(self, name: str):
        """开始计时"""
        with self.lock:
            self.timers[name] = time.time()

    def end_timer(self, name: str):
        """结束计时并记录"""
        with self.lock:
            if name in self.timers:
                duration = time.time() - self.timers[name]
                self.metrics[f"{name}_duration"].append(duration)
                del self.timers[name]
                return duration
        return None

    def record_metric(self, name: str, value: float):
        """记录指标"""
        with self.lock:
            self.metrics[name].append(value)

    def increment_counter(self, name: str):
        """增加计数器"""
        with self.lock:
            self.counters[name] += 1

    def get_metrics_summary(self) -> dict:
        """获取指标摘要"""
        with self.lock:
            summary = {}

            # 处理时间序列指标
            for name, values in self.metrics.items():
                if values:
                    summary[name] = {
                        'count': len(values),
                        'average': sum(values) / len(values),
                        'min': min(values),
                        'max': max(values),
                        'latest': values[-1]
                    }

            # 处理计数器
            summary['counters'] = dict(self.counters)

            return summary

    def reset_metrics(self):
        """重置所有指标"""
        with self.lock:
            self.metrics.clear()
            self.counters.clear()
            self.timers.clear()
```

### 5.2 调试脚本

#### WebSocket连接调试脚本
```python
#!/usr/bin/env python3
"""
WebSocket连接调试脚本
"""

import asyncio
import websockets
import json
import argparse
import sys

async def debug_websocket_connection(uri: str, access_token: str, vm_id: str):
    """调试WebSocket连接"""
    print(f"Connecting to {uri}")

    try:
        async with websockets.connect(uri) as websocket:
            print("Connected successfully!")

            # 发送连接消息
            connect_msg = {
                'type': 'CONNECT',
                'data': {
                    'vmId': vm_id,
                    'accessToken': access_token,
                    'clientVersion': '1.0.0'
                }
            }

            await websocket.send(json.dumps(connect_msg))
            print(f"Sent CONNECT message: {connect_msg}")

            # 等待响应
            try:
                response = await asyncio.wait_for(websocket.recv(), timeout=10.0)
                response_data = json.loads(response)
                print(f"Received response: {response_data}")

                if response_data.get('type') == 'CONNECT_ACK':
                    print("✅ Connection acknowledged by server")
                else:
                    print("❌ Unexpected response type")

            except asyncio.TimeoutError:
                print("❌ Timeout waiting for server response")

            # 发送心跳测试
            heartbeat_msg = {
                'type': 'HEARTBEAT',
                'data': {'timestamp': time.time()}
            }

            await websocket.send(json.dumps(heartbeat_msg))
            print(f"Sent HEARTBEAT: {heartbeat_msg}")

            # 等待心跳响应
            try:
                heartbeat_response = await asyncio.wait_for(websocket.recv(), timeout=5.0)
                heartbeat_data = json.loads(heartbeat_response)
                print(f"Heartbeat response: {heartbeat_data}")

                if heartbeat_data.get('type') == 'HEARTBEAT_ACK':
                    print("✅ Heartbeat acknowledged")
                else:
                    print("❌ Unexpected heartbeat response")

            except asyncio.TimeoutError:
                print("❌ Timeout waiting for heartbeat response")

    except websockets.exceptions.ConnectionClosed as e:
        print(f"❌ Connection closed: {e}")
    except Exception as e:
        print(f"❌ Connection error: {e}")

def main():
    parser = argparse.ArgumentParser(description='Debug WebSocket connection')
    parser.add_argument('--uri', '-u', required=True, help='WebSocket URI')
    parser.add_argument('--token', '-t', required=True, help='Access token')
    parser.add_argument('--vm-id', '-v', default='debug_vm', help='VM ID')

    args = parser.parse_args()

    asyncio.run(debug_websocket_connection(args.uri, args.token, args.vm_id))

if __name__ == "__main__":
    main()
```

#### 健康检查脚本
```python
#!/usr/bin/env python3
"""
系统健康检查脚本
"""

import psutil
import json
import time
import argparse
from pathlib import Path

def check_system_resources():
    """检查系统资源"""
    print("🔍 Checking system resources...")

    # CPU 使用率
    cpu_percent = psutil.cpu_percent(interval=1)
    print(f"CPU Usage: {cpu_percent}%")

    # 内存使用率
    memory = psutil.virtual_memory()
    print(f"Memory Usage: {memory.percent}% ({memory.used // (1024**2)}MB / {memory.total // (1024**2)}MB)")

    # 磁盘使用率
    disk = psutil.disk_usage('/')
    print(f"Disk Usage: {(disk.used / disk.total) * 100:.1f}% ({disk.used // (1024**3)}GB / {disk.total // (1024**3)}GB)")

    # 网络连接
    net_connections = len(psutil.net_connections())
    print(f"Network Connections: {net_connections}")

    return {
        'cpu_percent': cpu_percent,
        'memory_percent': memory.percent,
        'memory_used_mb': memory.used // (1024**2),
        'disk_percent': (disk.used / disk.total) * 100,
        'network_connections': net_connections
    }

def check_python_environment():
    """检查Python环境"""
    print("\n🐍 Checking Python environment...")

    import sys
    print(f"Python Version: {sys.version}")

    # 检查关键依赖
    required_packages = [
        'websocket-client',
        'numpy',
        'scikit-learn',
        'PyMySQL',
        'psutil'
    ]

    missing_packages = []
    for package in required_packages:
        try:
            __import__(package.replace('-', '_'))
            print(f"✅ {package}: Installed")
        except ImportError:
            print(f"❌ {package}: Missing")
            missing_packages.append(package)

    return {
        'python_version': sys.version,
        'missing_packages': missing_packages
    }

def check_database_connectivity():
    """检查数据库连接"""
    print("\n🗃️ Checking database connectivity...")

    try:
        from feduwacomm.database import DatabaseManager
        db = DatabaseManager()

        # 尝试简单查询
        test_query_result = db.execute_query("SELECT 1 as test")
        if test_query_result:
            print("✅ Database connection: OK")
            return {'database_status': 'OK'}
        else:
            print("❌ Database connection: Failed")
            return {'database_status': 'FAILED'}

    except Exception as e:
        print(f"❌ Database error: {e}")
        return {'database_status': 'ERROR', 'error': str(e)}

def check_websocket_connectivity(server_url: str, access_token: str):
    """检查WebSocket连接"""
    print(f"\n🌐 Checking WebSocket connectivity to {server_url}...")

    try:
        from feduwacomm.websocket.client import FederatedLearningClient

        client = FederatedLearningClient(
            server_url=server_url,
            access_token=access_token,
            vm_id="health_check_vm",
            config={}
        )

        # 尝试连接
        client.connect()
        time.sleep(2)

        if client.connected:
            print("✅ WebSocket connection: OK")
            client.disconnect()
            return {'websocket_status': 'OK'}
        else:
            print("❌ WebSocket connection: Failed")
            return {'websocket_status': 'FAILED'}

    except Exception as e:
        print(f"❌ WebSocket error: {e}")
        return {'websocket_status': 'ERROR', 'error': str(e)}

def main():
    parser = argparse.ArgumentParser(description='System health check')
    parser.add_argument('--server-url', '-s', help='WebSocket server URL for connectivity test')
    parser.add_argument('--token', '-t', help='Access token for WebSocket test')
    parser.add_argument('--output', '-o', help='Output file for results (JSON format)')

    args = parser.parse_args()

    print("🏥 Starting system health check...\n")

    # 执行各项检查
    results = {}
    results['timestamp'] = time.time()
    results['system_resources'] = check_system_resources()
    results['python_environment'] = check_python_environment()
    results['database'] = check_database_connectivity()

    if args.server_url and args.token:
        results['websocket'] = check_websocket_connectivity(args.server_url, args.token)

    # 输出结果
    print(f"\n📊 Health check completed at {time.ctime()}")

    if args.output:
        with open(args.output, 'w') as f:
            json.dump(results, f, indent=2)
        print(f"Results saved to {args.output}")

    # 计算总体健康评分
    score = 0
    max_score = 0

    if results['system_resources']['cpu_percent'] < 80:
        score += 1
    max_score += 1

    if results['system_resources']['memory_percent'] < 85:
        score += 1
    max_score += 1

    if not results['python_environment']['missing_packages']:
        score += 1
    max_score += 1

    if results['database']['database_status'] == 'OK':
        score += 1
    max_score += 1

    if 'websocket' in results and results['websocket']['websocket_status'] == 'OK':
        score += 1
    max_score += 1

    health_percentage = (score / max_score) * 100
    print(f"\n🎯 Overall Health Score: {score}/{max_score} ({health_percentage:.1f}%)")

    if health_percentage >= 80:
        print("✅ System is healthy")
        return 0
    elif health_percentage >= 60:
        print("⚠️ System has minor issues")
        return 1
    else:
        print("❌ System has major issues")
        return 2

if __name__ == "__main__":
    sys.exit(main())
```

## 6. 故障排除指南

### 6.1 常见问题和解决方案

#### 连接问题
```yaml
问题: WebSocket连接失败
症状:
  - 连接超时
  - 认证失败
  - 协议版本不匹配

解决步骤:
  1. 检查网络连接
     - ping 服务器地址
     - 检查防火墙设置
     - 验证端口开放状态

  2. 验证认证信息
     - 检查access_token是否有效
     - 确认vm_id格式正确
     - 验证服务器端用户权限

  3. 协议兼容性
     - 确认客户端和服务器都使用v1.4协议
     - 检查消息格式是否正确
     - 验证支持的消息类型

调试命令:
  python debug_websocket.py --uri ws://server:8080/websocket --token your_token --vm-id your_vm_id
```

#### 训练执行问题
```yaml
问题: 训练任务执行失败
症状:
  - 任务启动后立即失败
  - 训练过程中断
  - 梯度计算错误

解决步骤:
  1. 检查数据完整性
     - 验证训练数据是否存在
     - 检查数据格式是否正确
     - 确认数据量是否足够

  2. 算法配置验证
     - 检查算法类型是否支持
     - 验证超参数范围
     - 确认模型兼容性

  3. 资源限制
     - 检查内存使用情况
     - 验证CPU资源是否充足
     - 确认磁盘空间充足

调试命令:
  python health_check.py --server-url ws://server:8080/websocket --token your_token
```

#### 内存和性能问题
```yaml
问题: 内存泄漏或性能下降
症状:
  - 内存使用持续增长
  - 训练速度越来越慢
  - 系统响应变慢

解决步骤:
  1. 监控资源使用
     - 使用性能监控器跟踪资源
     - 检查是否有内存泄漏
     - 分析CPU使用模式

  2. 优化配置
     - 减少并发任务数量
     - 调整批次大小
     - 启用梯度压缩

  3. 代码优化
     - 及时释放不用的对象
     - 使用内存映射文件
     - 优化数据加载流程

监控命令:
  python -c "
  from feduwacomm.utils.resource_monitor import ResourceMonitor
  monitor = ResourceMonitor()
  monitor.start_monitoring()
  # 运行你的代码
  print(monitor.get_current_stats())
  "
```

### 6.2 日志分析

#### 日志级别和格式
```python
# 日志级别配置
CRITICAL: 系统无法继续运行的严重错误
ERROR: 功能失败，但系统可以继续运行
WARNING: 可能的问题，但不影响功能
INFO: 一般信息，如任务状态变化
DEBUG: 详细的调试信息

# 日志格式示例
2024-01-15 10:30:45,123 - feduwacomm.websocket.client - INFO - MESSAGE received: {
  "timestamp": "2024-01-15T10:30:45.123Z",
  "direction": "received",
  "message_type": "ROUND_START",
  "task_id": "task_001",
  "message_size": 1024,
  "message": {...}
}
```

#### 日志分析脚本
```python
#!/usr/bin/env python3
"""
日志分析脚本
"""

import re
import json
import argparse
from datetime import datetime
from collections import defaultdict, Counter

def analyze_logs(log_file: str):
    """分析日志文件"""
    print(f"📊 Analyzing log file: {log_file}")

    message_counts = Counter()
    error_counts = Counter()
    task_performance = defaultdict(list)
    connection_events = []

    with open(log_file, 'r') as f:
        for line in f:
            # 解析消息日志
            if 'MESSAGE' in line:
                try:
                    # 提取JSON部分
                    json_match = re.search(r'\{.*\}', line)
                    if json_match:
                        data = json.loads(json_match.group())
                        msg_type = data.get('message_type', 'UNKNOWN')
                        message_counts[msg_type] += 1
                except:
                    pass

            # 解析错误日志
            elif 'ERROR' in line:
                try:
                    json_match = re.search(r'\{.*\}', line)
                    if json_match:
                        data = json.loads(json_match.group())
                        error_type = data.get('error_type', 'UNKNOWN')
                        error_counts[error_type] += 1
                except:
                    pass

            # 解析训练指标
            elif 'TRAINING_METRICS' in line:
                try:
                    json_match = re.search(r'\{.*\}', line)
                    if json_match:
                        data = json.loads(json_match.group())
                        task_id = data.get('task_id')
                        metrics = data.get('metrics', {})
                        if task_id and metrics:
                            task_performance[task_id].append(metrics)
                except:
                    pass

            # 解析连接事件
            elif any(event in line for event in ['Connected', 'Disconnected', 'Connection failed']):
                timestamp_match = re.match(r'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})', line)
                if timestamp_match:
                    connection_events.append({
                        'timestamp': timestamp_match.group(1),
                        'event': line.strip()
                    })

    # 输出分析结果
    print("\n📈 Message Statistics:")
    for msg_type, count in message_counts.most_common():
        print(f"  {msg_type}: {count}")

    if error_counts:
        print("\n🚨 Error Statistics:")
        for error_type, count in error_counts.most_common():
            print(f"  {error_type}: {count}")

    print(f"\n🔄 Connection Events: {len(connection_events)}")
    for event in connection_events[-5:]:  # 显示最近5个事件
        print(f"  {event['timestamp']}: {event['event']}")

    print(f"\n🎯 Task Performance Summary:")
    for task_id, metrics_list in task_performance.items():
        if metrics_list:
            avg_accuracy = sum(m.get('evaluation', {}).get('accuracy', 0) for m in metrics_list) / len(metrics_list)
            avg_loss = sum(m.get('evaluation', {}).get('loss', 0) for m in metrics_list) / len(metrics_list)
            print(f"  {task_id}: {len(metrics_list)} rounds, avg accuracy: {avg_accuracy:.3f}, avg loss: {avg_loss:.3f}")

def main():
    parser = argparse.ArgumentParser(description='Analyze federated learning logs')
    parser.add_argument('log_file', help='Path to log file')

    args = parser.parse_args()
    analyze_logs(args.log_file)

if __name__ == "__main__":
    main()
```

---

**下一步**: 继续阅读 [部署和维护指导](./07-deployment-maintenance.md) 了解生产环境部署和长期维护策略。