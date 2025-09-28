"""
消息路由器单元测试
"""

import pytest
from unittest.mock import Mock, patch
import time

from feduwacomm.ml.websocket.message_router import MessageRouter, MessageType


@pytest.mark.unit
class TestMessageRouter:
    """消息路由器测试类"""

    def test_router_initialization(self, mock_client):
        """测试路由器初始化"""
        router = MessageRouter(mock_client)
        
        assert router.client == mock_client
        assert len(router.message_handlers) > 0
        assert router.routing_stats['total_messages'] == 0

    def test_message_type_enum(self):
        """测试消息类型枚举"""
        # 验证所有必需的消息类型存在
        required_types = [
            'CONNECT', 'CONNECT_ACK', 'DISCONNECT',
            'HEARTBEAT', 'HEARTBEAT_ACK',
            'FEDERATED_TASK_START', 'FEDERATED_TASK_STOP',
            'ROUND_START', 'ROUND_COMPLETE',
            'GLOBAL_MODEL_BROADCAST', 'GRADIENT_UPLOAD',
            'STATUS_QUERY', 'STATUS_RESPONSE',
            'ERROR'
        ]
        
        for msg_type in required_types:
            assert hasattr(MessageType, msg_type)

    def test_route_message_success(self, message_router, sample_message):
        """测试消息路由成功"""
        # 模拟处理器
        with patch.object(message_router, '_handle_task_start', return_value=True) as mock_handler:
            result = message_router.route_message(sample_message)
            
            assert result == True
            assert message_router.routing_stats['successful_routes'] == 1
            mock_handler.assert_called_once_with(sample_message)

    def test_route_message_missing_type(self, message_router):
        """测试缺少type字段的消息"""
        invalid_message = {
            'id': 'msg-001',
            'timestamp': 1640995200000
        }
        
        result = message_router.route_message(invalid_message)
        
        assert result == False
        assert message_router.routing_stats['failed_routes'] == 1

    def test_route_message_unknown_type(self, message_router):
        """测试未知消息类型"""
        unknown_message = {
            'type': 'UNKNOWN_MESSAGE_TYPE',
            'id': 'msg-001',
            'timestamp': 1640995200000
        }
        
        result = message_router.route_message(unknown_message)
        
        assert result == False
        assert message_router.routing_stats['unknown_types'] == 1

    def test_extract_task_id(self, message_router):
        """测试提取任务ID"""
        # 有任务ID的消息
        message_with_task = {
            'type': 'FEDERATED_TASK_START',
            'data': {'taskId': 'test-task-001'}
        }
        
        task_id = message_router._extract_task_id(message_with_task)
        assert task_id == 'test-task-001'
        
        # 没有任务ID的消息
        message_without_task = {
            'type': 'HEARTBEAT',
            'data': {}
        }
        
        task_id = message_router._extract_task_id(message_without_task)
        assert task_id is None

    def test_handle_connect_ack_success(self, message_router):
        """测试处理连接确认成功"""
        connect_ack = {
            'type': 'CONNECT_ACK',
            'data': {
                'status': 'SUCCESS',
                'serverConfig': {
                    'heartbeatInterval': 60,
                    'maxConcurrentTasks': 5
                }
            }
        }
        
        result = message_router._handle_connect_ack(connect_ack)
        
        assert result == True
        assert message_router.client.heartbeat_interval == 60
        assert message_router.client.max_concurrent_tasks == 5

    def test_handle_connect_ack_failure(self, message_router):
        """测试处理连接确认失败"""
        connect_ack = {
            'type': 'CONNECT_ACK',
            'data': {
                'status': 'FAILED',
                'message': 'Authentication failed'
            }
        }
        
        result = message_router._handle_connect_ack(connect_ack)
        
        assert result == False

    def test_handle_heartbeat_ack(self, message_router):
        """测试处理心跳确认"""
        heartbeat_ack = {
            'type': 'HEARTBEAT_ACK',
            'data': {
                'serverTime': int(time.time() * 1000)
            }
        }
        
        result = message_router._handle_heartbeat_ack(heartbeat_ack)
        
        assert result == True

    def test_handle_task_start_success(self, message_router, sample_task_data):
        """测试处理任务启动成功"""
        task_start_msg = {
            'type': 'FEDERATED_TASK_START',
            'data': sample_task_data
        }
        
        with patch('feduwacomm.ml.websocket.task_context.TaskContext') as mock_task_context:
            mock_context = Mock()
            mock_context.initialize.return_value = True
            mock_task_context.return_value = mock_context
            
            result = message_router._handle_task_start(task_start_msg)
            
            assert result == True
            assert sample_task_data['taskId'] in message_router.client.active_tasks

    def test_handle_task_start_concurrent_limit(self, message_router, sample_task_data):
        """测试任务启动并发限制"""
        # 设置并发限制
        message_router.client.max_concurrent_tasks = 1
        
        # 添加一个活跃任务
        message_router.client.active_tasks['existing-task'] = Mock()
        
        task_start_msg = {
            'type': 'FEDERATED_TASK_START',
            'data': sample_task_data
        }
        
        result = message_router._handle_task_start(task_start_msg)
        
        assert result == False

    def test_validate_task_config_valid(self, message_router, sample_task_data):
        """测试有效任务配置验证"""
        result = message_router._validate_task_config(sample_task_data)
        
        assert result['valid'] == True
        assert result['error'] is None

    def test_validate_task_config_invalid(self, message_router):
        """测试无效任务配置验证"""
        invalid_config = {
            'taskId': 'test-task-001'
            # 缺少必需字段
        }
        
        result = message_router._validate_task_config(invalid_config)
        
        assert result['valid'] == False
        assert result['error'] is not None

    def test_handle_task_stop(self, message_router):
        """测试处理任务停止"""
        # 添加活跃任务
        mock_task = Mock()
        task_id = 'test-task-001'
        message_router.client.active_tasks[task_id] = mock_task
        
        task_stop_msg = {
            'type': 'FEDERATED_TASK_STOP',
            'data': {
                'taskId': task_id,
                'reason': 'MANUAL_STOP',
                'graceful': True
            }
        }
        
        result = message_router._handle_task_stop(task_stop_msg)
        
        assert result == True
        mock_task.stop.assert_called_once()

    def test_handle_task_resume(self, message_router):
        """测试处理任务恢复"""
        # 添加暂停的任务
        mock_task = Mock()
        mock_task.status.value = 'PAUSED'
        task_id = 'test-task-001'
        message_router.client.active_tasks[task_id] = mock_task
        
        task_resume_msg = {
            'type': 'FEDERATED_TASK_RESUME',
            'data': {
                'taskId': task_id,
                'resumeFrom': {
                    'roundNumber': 3
                }
            }
        }
        
        with patch('feduwacomm.ml.websocket.task_context.TaskStatus') as mock_status:
            mock_status.PAUSED = 'PAUSED'
            mock_task.status = mock_status.PAUSED
            
            result = message_router._handle_task_resume(task_resume_msg)
            
            assert result == True
            mock_task.resume.assert_called_once()

    def test_handle_task_delete(self, message_router):
        """测试处理任务删除"""
        # 添加活跃任务
        mock_task = Mock()
        task_id = 'test-task-001'
        message_router.client.active_tasks[task_id] = mock_task
        
        task_delete_msg = {
            'type': 'FEDERATED_TASK_DELETE',
            'data': {
                'taskId': task_id
            }
        }
        
        result = message_router._handle_task_delete(task_delete_msg)
        
        assert result == True
        mock_task.cleanup.assert_called_once()
        assert task_id not in message_router.client.active_tasks

    def test_handle_round_start(self, message_router):
        """测试处理轮次开始"""
        # 添加活跃任务
        mock_task = Mock()
        mock_task.start_round_training.return_value = True
        task_id = 'test-task-001'
        message_router.client.active_tasks[task_id] = mock_task
        
        round_start_msg = {
            'type': 'ROUND_START',
            'data': {
                'taskId': task_id,
                'roundNumber': 1
            }
        }
        
        result = message_router._handle_round_start(round_start_msg)
        
        assert result == True
        mock_task.start_round_training.assert_called_once_with(1, round_start_msg['data'])

    def test_handle_global_model_broadcast(self, message_router, sample_global_model):
        """测试处理全局模型广播"""
        # 添加活跃任务
        mock_task = Mock()
        mock_task.update_global_model.return_value = True
        task_id = 'test-task-001'
        message_router.client.active_tasks[task_id] = mock_task
        
        model_broadcast_msg = {
            'type': 'GLOBAL_MODEL_BROADCAST',
            'data': {
                'taskId': task_id,
                'roundNumber': 1,
                'globalModelData': sample_global_model
            }
        }
        
        result = message_router._handle_global_model_broadcast(model_broadcast_msg)
        
        assert result == True
        mock_task.update_global_model.assert_called_once()

    def test_decode_global_model_json(self, message_router, sample_global_model):
        """测试解码JSON格式全局模型"""
        model_data = {
            'globalModelData': sample_global_model
        }
        
        decoded = message_router._decode_global_model(model_data)
        
        assert decoded == sample_global_model

    def test_decode_global_model_base64(self, message_router, sample_global_model):
        """测试解码Base64格式全局模型"""
        import base64
        import json
        
        model_json = json.dumps(sample_global_model)
        model_b64 = base64.b64encode(model_json.encode('utf-8')).decode('utf-8')
        
        model_data = {
            'globalModel': model_b64
        }
        
        decoded = message_router._decode_global_model(model_data)
        
        assert decoded == sample_global_model

    def test_validate_model_data_valid(self, message_router, sample_global_model):
        """测试有效模型数据验证"""
        result = message_router._validate_model_data(sample_global_model)
        
        assert result == True

    def test_validate_model_data_invalid(self, message_router):
        """测试无效模型数据验证"""
        invalid_model = {
            'parameters': {}
            # 缺少model_type
        }
        
        result = message_router._validate_model_data(invalid_model)
        
        assert result == False

    def test_handle_status_query_vm_status(self, message_router):
        """测试处理VM状态查询"""
        status_query = {
            'type': 'STATUS_QUERY',
            'id': 'query-001',
            'data': {
                'queryType': 'VM_STATUS'
            }
        }
        
        result = message_router._handle_status_query(status_query)
        
        assert result == True
        # 验证发送了状态响应
        message_router.client._send_message.assert_called_once()

    def test_handle_status_query_task_status(self, message_router):
        """测试处理任务状态查询"""
        # 添加活跃任务
        mock_task = Mock()
        mock_task.get_statistics.return_value = {'status': 'TRAINING'}
        task_id = 'test-task-001'
        message_router.client.active_tasks[task_id] = mock_task
        
        status_query = {
            'type': 'STATUS_QUERY',
            'id': 'query-001',
            'data': {
                'queryType': 'TASK_STATUS',
                'taskId': task_id
            }
        }
        
        result = message_router._handle_status_query(status_query)
        
        assert result == True

    def test_handle_error_message(self, message_router):
        """测试处理错误消息"""
        error_msg = {
            'type': 'ERROR',
            'data': {
                'errorType': 'TASK_ERROR',
                'errorMessage': 'Task execution failed',
                'taskId': 'test-task-001'
            }
        }
        
        result = message_router._handle_error_message(error_msg)
        
        assert result == True

    def test_routing_stats(self, message_router, sample_message):
        """测试路由统计"""
        # 路由成功消息
        with patch.object(message_router, '_handle_task_start', return_value=True):
            message_router.route_message(sample_message)
        
        # 路由失败消息
        invalid_message = {'invalid': 'message'}
        message_router.route_message(invalid_message)
        
        stats = message_router.get_routing_stats()
        
        assert stats['total_messages'] == 2
        assert stats['successful_routes'] == 1
        assert stats['failed_routes'] == 1

    def test_send_acknowledgment_messages(self, message_router):
        """测试发送确认消息"""
        task_id = 'test-task-001'
        
        # 测试任务启动确认
        message_router._send_task_start_ack(task_id, 'SUCCESS', '任务启动成功')
        
        # 验证消息被发送
        message_router.client._send_message.assert_called()
        
        # 获取发送的消息
        sent_message = message_router.client._send_message.call_args[0][0]
        assert sent_message['type'] == 'FEDERATED_TASK_START_ACK'
        assert sent_message['data']['taskId'] == task_id
        assert sent_message['data']['status'] == 'SUCCESS'

    def test_concurrent_message_routing(self, message_router):
        """测试并发消息路由"""
        import threading
        
        messages = []
        for i in range(10):
            messages.append({
                'type': 'HEARTBEAT_ACK',
                'id': f'msg-{i}',
                'timestamp': 1640995200000 + i,
                'data': {}
            })
        
        results = []
        
        def route_message(msg):
            result = message_router.route_message(msg)
            results.append(result)
        
        threads = []
        for msg in messages:
            thread = threading.Thread(target=route_message, args=(msg,))
            threads.append(thread)
            thread.start()
        
        for thread in threads:
            thread.join()
        
        # 所有消息都应该被成功路由
        assert len(results) == 10
        assert all(results)
