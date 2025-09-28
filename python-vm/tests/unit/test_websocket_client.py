"""
WebSocket客户端单元测试
"""

import pytest
import asyncio
import json
from unittest.mock import Mock, patch, AsyncMock
import websockets

from feduwacomm.ml.websocket.client import WebSocketClient, ConnectionStatus


@pytest.mark.unit
class TestWebSocketClient:
    """WebSocket客户端测试类"""

    def test_client_initialization(self, test_config):
        """测试客户端初始化"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        assert client.server_url == "ws://localhost:8080/websocket"
        assert client.vm_id == "test-vm-001"
        assert client.status == ConnectionStatus.DISCONNECTED
        assert client.is_connected == False
        assert len(client.active_tasks) == 0

    def test_message_id_generation(self, test_config):
        """测试消息ID生成"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        msg_id1 = client._generate_message_id()
        msg_id2 = client._generate_message_id()
        
        assert msg_id1 != msg_id2
        assert isinstance(msg_id1, str)
        assert len(msg_id1) > 0

    def test_timestamp_generation(self, test_config):
        """测试时间戳生成"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        timestamp1 = client._get_current_timestamp()
        timestamp2 = client._get_current_timestamp()
        
        assert isinstance(timestamp1, int)
        assert timestamp2 >= timestamp1

    def test_connect_message_creation(self, test_config):
        """测试连接消息创建"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        connect_msg = client._create_connect_message()
        
        assert connect_msg['type'] == 'CONNECT'
        assert connect_msg['vmId'] == 'test-vm-001'
        assert 'id' in connect_msg
        assert 'timestamp' in connect_msg
        assert 'data' in connect_msg
        assert connect_msg['data']['protocolVersion'] == '1.4'

    def test_heartbeat_message_creation(self, test_config):
        """测试心跳消息创建"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        heartbeat_msg = client._create_heartbeat_message()
        
        assert heartbeat_msg['type'] == 'HEARTBEAT'
        assert heartbeat_msg['vmId'] == 'test-vm-001'
        assert 'id' in heartbeat_msg
        assert 'timestamp' in heartbeat_msg

    @patch('websockets.connect')
    async def test_connect_success(self, mock_connect, test_config):
        """测试连接成功"""
        # 模拟WebSocket连接
        mock_websocket = AsyncMock()
        mock_connect.return_value.__aenter__.return_value = mock_websocket
        
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 模拟连接确认消息
        connect_ack = {
            'type': 'CONNECT_ACK',
            'data': {'status': 'SUCCESS'}
        }
        mock_websocket.recv.return_value = json.dumps(connect_ack)
        
        result = await client.connect()
        
        assert result == True
        assert client.status == ConnectionStatus.CONNECTING
        mock_connect.assert_called_once()

    @patch('websockets.connect')
    async def test_connect_failure(self, mock_connect, test_config):
        """测试连接失败"""
        # 模拟连接异常
        mock_connect.side_effect = Exception("Connection failed")
        
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        result = await client.connect()
        
        assert result == False
        assert client.status == ConnectionStatus.ERROR

    def test_message_validation(self, test_config):
        """测试消息验证"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 有效消息
        valid_message = {
            'type': 'HEARTBEAT',
            'id': 'msg-001',
            'timestamp': 1640995200000,
            'vmId': 'test-vm-001'
        }
        assert client._validate_message(valid_message) == True
        
        # 无效消息 - 缺少type
        invalid_message1 = {
            'id': 'msg-001',
            'timestamp': 1640995200000
        }
        assert client._validate_message(invalid_message1) == False
        
        # 无效消息 - 缺少id
        invalid_message2 = {
            'type': 'HEARTBEAT',
            'timestamp': 1640995200000
        }
        assert client._validate_message(invalid_message2) == False

    def test_resource_usage_calculation(self, test_config):
        """测试资源使用情况计算"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        with patch('psutil.cpu_percent', return_value=50.0), \
             patch('psutil.virtual_memory') as mock_memory, \
             patch('psutil.disk_usage') as mock_disk:
            
            mock_memory.return_value.percent = 60.0
            mock_disk.return_value.percent = 70.0
            
            usage = client._get_resource_usage()
            
            assert usage['cpu_percent'] == 50.0
            assert usage['memory_percent'] == 60.0
            assert usage['disk_percent'] == 70.0

    def test_vm_capabilities(self, test_config):
        """测试VM能力信息"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        capabilities = client._get_vm_capabilities()
        
        assert 'supported_algorithms' in capabilities
        assert 'max_concurrent_tasks' in capabilities
        assert 'protocol_version' in capabilities
        assert capabilities['protocol_version'] == '1.4'

    def test_active_tasks_status(self, test_config):
        """测试活跃任务状态"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 添加模拟任务
        mock_task = Mock()
        mock_task.get_statistics.return_value = {
            'status': 'TRAINING',
            'progress': 0.5
        }
        client.active_tasks['task-001'] = mock_task
        
        status = client._get_active_tasks_status()
        
        assert 'task-001' in status
        assert status['task-001']['status'] == 'TRAINING'

    def test_performance_metrics(self, test_config):
        """测试性能指标"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        metrics = client._get_performance_metrics()
        
        assert isinstance(metrics, dict)
        # 基本指标应该存在
        expected_keys = ['uptime', 'message_count', 'error_count']
        for key in expected_keys:
            assert key in metrics

    def test_disconnect(self, test_config):
        """测试断开连接"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 模拟已连接状态
        client.status = ConnectionStatus.CONNECTED
        client.websocket = Mock()
        
        client.disconnect()
        
        assert client.status == ConnectionStatus.DISCONNECTED
        assert client.websocket is None

    def test_reconnect_logic(self, test_config):
        """测试重连逻辑"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 设置重连参数
        client.max_reconnect_attempts = 3
        client.reconnect_delay = 0.1  # 快速测试
        
        with patch.object(client, 'connect', return_value=True) as mock_connect:
            result = client.reconnect()
            
            assert result == True
            mock_connect.assert_called_once()

    def test_message_queue_management(self, test_config):
        """测试消息队列管理"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 测试消息入队
        message = {'type': 'TEST', 'data': 'test'}
        client._queue_message(message)
        
        assert len(client.message_queue) == 1
        
        # 测试消息出队
        queued_message = client._dequeue_message()
        assert queued_message == message
        assert len(client.message_queue) == 0

    def test_error_handling(self, test_config):
        """测试错误处理"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 测试连接错误处理
        with patch.object(client, '_handle_connection_error') as mock_handler:
            client._handle_error(Exception("Test error"), "CONNECTION")
            mock_handler.assert_called_once()

    def test_heartbeat_mechanism(self, test_config):
        """测试心跳机制"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        client.heartbeat_interval = 0.1  # 快速测试
        
        with patch.object(client, '_send_heartbeat') as mock_heartbeat:
            client._start_heartbeat()
            
            # 等待心跳发送
            import time
            time.sleep(0.2)
            
            client._stop_heartbeat()
            
            # 验证心跳被发送
            assert mock_heartbeat.called

    @pytest.mark.slow
    def test_connection_timeout(self, test_config):
        """测试连接超时"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        client.connection_timeout = 0.1  # 快速超时
        
        with patch('websockets.connect') as mock_connect:
            # 模拟连接挂起
            mock_connect.return_value.__aenter__ = AsyncMock(side_effect=asyncio.TimeoutError())
            
            result = asyncio.run(client.connect())
            
            assert result == False
            assert client.status == ConnectionStatus.ERROR

    def test_message_compression(self, test_config):
        """测试消息压缩"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 大消息应该被压缩
        large_message = {
            'type': 'TEST',
            'data': 'x' * 10000  # 大数据
        }
        
        compressed = client._compress_message(large_message)
        decompressed = client._decompress_message(compressed)
        
        assert decompressed == large_message

    def test_concurrent_task_limit(self, test_config):
        """测试并发任务限制"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        client.max_concurrent_tasks = 2
        
        # 添加任务直到达到限制
        for i in range(3):
            task_id = f"task-{i}"
            if i < 2:
                assert client._can_accept_task(task_id) == True
                client.active_tasks[task_id] = Mock()
            else:
                assert client._can_accept_task(task_id) == False

    def test_statistics_collection(self, test_config):
        """测试统计信息收集"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 模拟一些活动
        client._record_message_sent("HEARTBEAT")
        client._record_message_received("HEARTBEAT_ACK")
        client._record_error("CONNECTION_ERROR")
        
        stats = client.get_statistics()
        
        assert stats['messages_sent'] >= 1
        assert stats['messages_received'] >= 1
        assert stats['errors'] >= 1
