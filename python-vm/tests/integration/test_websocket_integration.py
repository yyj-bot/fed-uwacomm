"""
WebSocket集成测试
"""

import pytest
import asyncio
import json
import time
from unittest.mock import Mock, patch, AsyncMock

from feduwacomm.ml.websocket.client import WebSocketClient
from feduwacomm.ml.websocket.message_router import MessageRouter
from feduwacomm.ml.websocket.task_manager import TaskManager


@pytest.mark.integration
class TestWebSocketIntegration:
    """WebSocket集成测试类"""

    @pytest.fixture
    def integrated_client(self, test_config, mock_server):
        """集成客户端fixture"""
        client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        # 添加任务管理器
        client.task_manager = TaskManager(client)
        
        return client

    async def test_full_connection_flow(self, integrated_client, mock_server):
        """测试完整连接流程"""
        mock_server.start()
        
        with patch('websockets.connect') as mock_connect:
            # 模拟WebSocket连接
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            # 模拟连接确认
            connect_ack = {
                'type': 'CONNECT_ACK',
                'data': {
                    'status': 'SUCCESS',
                    'serverConfig': {
                        'heartbeatInterval': 30,
                        'maxConcurrentTasks': 3
                    }
                }
            }
            mock_websocket.recv.return_value = json.dumps(connect_ack)
            
            # 执行连接
            result = await integrated_client.connect()
            
            assert result == True
            mock_connect.assert_called_once()
        
        mock_server.stop()

    async def test_task_lifecycle_integration(self, integrated_client, sample_task_data):
        """测试任务生命周期集成"""
        with patch('websockets.connect') as mock_connect:
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            # 模拟连接成功
            await integrated_client.connect()
            
            # 1. 任务启动
            task_start_msg = {
                'type': 'FEDERATED_TASK_START',
                'data': sample_task_data
            }
            
            with patch('feduwacomm.ml.websocket.task_context.TaskContext') as mock_task_context:
                mock_context = Mock()
                mock_context.initialize.return_value = True
                mock_context.status.value = 'READY'
                mock_task_context.return_value = mock_context
                
                # 路由任务启动消息
                router = MessageRouter(integrated_client)
                result = router.route_message(task_start_msg)
                
                assert result == True
                assert sample_task_data['taskId'] in integrated_client.active_tasks
            
            # 2. 轮次开始
            round_start_msg = {
                'type': 'ROUND_START',
                'data': {
                    'taskId': sample_task_data['taskId'],
                    'roundNumber': 1
                }
            }
            
            mock_context.start_round_training.return_value = True
            result = router.route_message(round_start_msg)
            assert result == True
            
            # 3. 全局模型广播
            model_broadcast_msg = {
                'type': 'GLOBAL_MODEL_BROADCAST',
                'data': {
                    'taskId': sample_task_data['taskId'],
                    'roundNumber': 1,
                    'globalModelData': {
                        'model_type': 'RandomForest',
                        'parameters': {}
                    }
                }
            }
            
            mock_context.update_global_model.return_value = True
            result = router.route_message(model_broadcast_msg)
            assert result == True
            
            # 4. 任务停止
            task_stop_msg = {
                'type': 'FEDERATED_TASK_STOP',
                'data': {
                    'taskId': sample_task_data['taskId'],
                    'reason': 'COMPLETED'
                }
            }
            
            result = router.route_message(task_stop_msg)
            assert result == True

    async def test_concurrent_tasks_integration(self, integrated_client):
        """测试并发任务集成"""
        with patch('websockets.connect') as mock_connect:
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            await integrated_client.connect()
            
            router = MessageRouter(integrated_client)
            
            # 创建多个任务
            task_ids = ['task-001', 'task-002', 'task-003']
            
            with patch('feduwacomm.ml.websocket.task_context.TaskContext') as mock_task_context:
                for task_id in task_ids:
                    mock_context = Mock()
                    mock_context.initialize.return_value = True
                    mock_task_context.return_value = mock_context
                    
                    task_start_msg = {
                        'type': 'FEDERATED_TASK_START',
                        'data': {
                            'taskId': task_id,
                            'federatedAlgorithm': 'FEDERATED_AVERAGING',
                            'totalRounds': 5,
                            'localTrainingConfig': {
                                'datasetId': f'dataset-{task_id}',
                                'epochs': 3
                            }
                        }
                    }
                    
                    result = router.route_message(task_start_msg)
                    assert result == True
                
                # 验证所有任务都被创建
                assert len(integrated_client.active_tasks) == 3
                for task_id in task_ids:
                    assert task_id in integrated_client.active_tasks

    async def test_error_recovery_integration(self, integrated_client):
        """测试错误恢复集成"""
        with patch('websockets.connect') as mock_connect:
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            # 模拟连接中断
            mock_websocket.recv.side_effect = [
                json.dumps({'type': 'CONNECT_ACK', 'data': {'status': 'SUCCESS'}}),
                ConnectionError("Connection lost")
            ]
            
            await integrated_client.connect()
            
            # 模拟错误处理
            with patch.object(integrated_client, 'reconnect', return_value=True) as mock_reconnect:
                # 触发错误处理
                integrated_client._handle_connection_error(ConnectionError("Connection lost"))
                
                # 验证重连被调用
                mock_reconnect.assert_called()

    async def test_heartbeat_integration(self, integrated_client):
        """测试心跳集成"""
        with patch('websockets.connect') as mock_connect:
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            # 设置快速心跳用于测试
            integrated_client.heartbeat_interval = 0.1
            
            await integrated_client.connect()
            
            # 启动心跳
            integrated_client._start_heartbeat()
            
            # 等待心跳发送
            await asyncio.sleep(0.2)
            
            # 停止心跳
            integrated_client._stop_heartbeat()
            
            # 验证心跳消息被发送
            assert mock_websocket.send.called

    async def test_message_queue_integration(self, integrated_client):
        """测试消息队列集成"""
        # 在未连接状态下发送消息
        message = {
            'type': 'HEARTBEAT',
            'id': 'msg-001',
            'timestamp': int(time.time() * 1000)
        }
        
        # 消息应该被排队
        integrated_client._queue_message(message)
        assert len(integrated_client.message_queue) == 1
        
        with patch('websockets.connect') as mock_connect:
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            # 连接后消息队列应该被处理
            await integrated_client.connect()
            
            # 处理排队的消息
            integrated_client._process_message_queue()
            
            # 验证消息被发送
            assert mock_websocket.send.called

    async def test_status_monitoring_integration(self, integrated_client):
        """测试状态监控集成"""
        with patch('websockets.connect') as mock_connect:
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            await integrated_client.connect()
            
            # 添加模拟任务
            mock_task = Mock()
            mock_task.get_statistics.return_value = {
                'status': 'TRAINING',
                'progress': 0.5,
                'accuracy': 0.85
            }
            integrated_client.active_tasks['test-task'] = mock_task
            
            # 处理状态查询
            router = MessageRouter(integrated_client)
            status_query = {
                'type': 'STATUS_QUERY',
                'id': 'query-001',
                'data': {
                    'queryType': 'VM_STATUS'
                }
            }
            
            result = router.route_message(status_query)
            assert result == True
            
            # 验证状态响应被发送
            integrated_client._send_message.assert_called()

    async def test_resource_monitoring_integration(self, integrated_client):
        """测试资源监控集成"""
        with patch('psutil.cpu_percent', return_value=75.0), \
             patch('psutil.virtual_memory') as mock_memory, \
             patch('psutil.disk_usage') as mock_disk:
            
            mock_memory.return_value.percent = 80.0
            mock_disk.return_value.percent = 60.0
            
            # 获取资源使用情况
            usage = integrated_client._get_resource_usage()
            
            assert usage['cpu_percent'] == 75.0
            assert usage['memory_percent'] == 80.0
            assert usage['disk_percent'] == 60.0

    async def test_performance_metrics_integration(self, integrated_client):
        """测试性能指标集成"""
        # 记录一些活动
        integrated_client._record_message_sent("HEARTBEAT")
        integrated_client._record_message_received("HEARTBEAT_ACK")
        
        # 获取性能指标
        metrics = integrated_client._get_performance_metrics()
        
        assert 'message_count' in metrics
        assert 'uptime' in metrics
        assert metrics['message_count'] >= 2

    @pytest.mark.slow
    async def test_reconnection_integration(self, integrated_client):
        """测试重连集成"""
        with patch('websockets.connect') as mock_connect:
            # 第一次连接失败
            mock_connect.side_effect = [
                ConnectionError("Connection failed"),
                # 第二次连接成功
                AsyncMock()
            ]
            
            # 设置快速重连用于测试
            integrated_client.reconnect_delay = 0.1
            integrated_client.max_reconnect_attempts = 2
            
            # 尝试连接
            result = await integrated_client.connect()
            
            # 第一次应该失败
            assert result == False
            
            # 触发重连
            result = integrated_client.reconnect()
            
            # 重连应该成功
            assert result == True

    async def test_data_serialization_integration(self, integrated_client, sample_global_model):
        """测试数据序列化集成"""
        # 测试模型序列化
        serialized = integrated_client._serialize_model(sample_global_model)
        deserialized = integrated_client._deserialize_model(serialized)
        
        assert deserialized == sample_global_model
        
        # 测试消息压缩
        large_message = {
            'type': 'TEST',
            'data': 'x' * 10000
        }
        
        compressed = integrated_client._compress_message(large_message)
        decompressed = integrated_client._decompress_message(compressed)
        
        assert decompressed == large_message

    async def test_configuration_integration(self, integrated_client, config_manager):
        """测试配置集成"""
        # 更新配置
        config_manager.set('websocket.heartbeat_interval', 60)
        config_manager.set('federated.max_concurrent_tasks', 5)
        
        # 应用配置到客户端
        integrated_client._apply_config(config_manager.get_websocket_config())
        
        assert integrated_client.heartbeat_interval == 60
        assert integrated_client.max_concurrent_tasks == 5

    async def test_logging_integration(self, integrated_client, log_capture):
        """测试日志集成"""
        with patch('websockets.connect') as mock_connect:
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            # 执行一些操作
            await integrated_client.connect()
            integrated_client._record_message_sent("TEST")
            integrated_client.disconnect()
            
            # 检查日志输出
            log_output = log_capture.getvalue()
            assert len(log_output) > 0
            # 可以检查特定的日志消息
