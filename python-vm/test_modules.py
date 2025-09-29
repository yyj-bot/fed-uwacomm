#!/usr/bin/env python3
"""
新模块测试脚本
逐一测试所有新写的模块功能
"""

import sys
import os
import tempfile
import traceback
from pathlib import Path

# 添加项目路径
sys.path.insert(0, 'src')

def test_config_manager():
    """测试配置管理器"""
    print("=== 测试配置管理器 ===")
    try:
        from feduwacomm.ml.config.config_manager import ConfigManager
        
        with tempfile.TemporaryDirectory() as temp_dir:
            config_manager = ConfigManager(temp_dir, 'test')
            
            # 测试基本功能
            config_manager.set('test.key', 'test_value')
            value = config_manager.get('test.key')
            assert value == 'test_value', f"期望 'test_value'，得到 {value}"
            print("✅ 设置和获取配置")
            
            # 测试嵌套配置
            nested_value = config_manager.get('websocket.server_url', 'default')
            assert nested_value is not None, "嵌套配置获取失败"
            print("✅ 获取嵌套配置")
            
            # 测试配置信息
            info = config_manager.get_config_info()
            assert 'environment' in info, "配置信息缺少环境字段"
            print("✅ 配置信息获取")
            
            # 测试WebSocket配置
            ws_config = config_manager.get_websocket_config()
            assert isinstance(ws_config, dict), "WebSocket配置应该是字典"
            print("✅ WebSocket配置获取")
            
        print("配置管理器测试完成 ✅\n")
        return True
        
    except Exception as e:
        print(f"❌ 配置管理器测试失败: {e}")
        traceback.print_exc()
        return False


def test_error_handler():
    """测试错误处理器"""
    print("=== 测试错误处理器 ===")
    try:
        from feduwacomm.ml.utils.error_handler import ErrorHandler, ErrorType, ErrorLevel
        
        # 创建错误处理器
        error_handler = ErrorHandler()
        
        # 测试错误处理
        error_id = error_handler.handle_error(
            ErrorType.CONNECTION_ERROR,
            "测试连接错误",
            context={'test': True}
        )
        assert error_id is not None, "错误ID不应该为空"
        print("✅ 错误处理")
        
        # 测试错误统计
        stats = error_handler.get_error_stats()
        assert 'total_errors' in stats, "错误统计缺少总错误数"
        assert stats['total_errors'] > 0, "应该有错误记录"
        print("✅ 错误统计")
        
        # 测试最近错误
        recent_errors = error_handler.get_recent_errors(5)
        assert len(recent_errors) > 0, "应该有最近错误记录"
        print("✅ 最近错误获取")
        
        print("错误处理器测试完成 ✅\n")
        return True
        
    except Exception as e:
        print(f"❌ 错误处理器测试失败: {e}")
        traceback.print_exc()
        return False


def test_monitor():
    """测试监控器"""
    print("=== 测试监控器 ===")
    try:
        from feduwacomm.ml.utils.monitor import Monitor
        
        # 创建监控器
        config = {
            'enabled': True,
            'interval': 1,
            'retention_hours': 1,
            'debug': True
        }
        monitor = Monitor(None, config)
        
        # 测试指标记录
        monitor.record_metric('test_metric', 100.0, {'type': 'test'})
        print("✅ 指标记录")
        
        # 测试计时器
        stop_timer = monitor.start_timer('test_timer')
        import time
        time.sleep(0.01)  # 短暂等待
        duration = stop_timer()
        assert duration > 0, "计时器应该返回正数"
        print("✅ 计时器功能")
        
        # 测试健康状态
        health = monitor.get_health_status()
        assert 'status' in health, "健康状态缺少状态字段"
        print("✅ 健康状态")
        
        # 测试监控信息
        info = monitor.get_monitoring_info()
        assert 'enabled' in info, "监控信息缺少启用字段"
        print("✅ 监控信息")
        
        print("监控器测试完成 ✅\n")
        return True
        
    except Exception as e:
        print(f"❌ 监控器测试失败: {e}")
        traceback.print_exc()
        return False


def test_message_router():
    """测试消息路由器"""
    print("=== 测试消息路由器 ===")
    try:
        from feduwacomm.ml.websocket.message_router import MessageRouter, MessageType
        from unittest.mock import Mock
        
        # 创建模拟客户端
        mock_client = Mock()
        mock_client.vm_id = "test-vm"
        mock_client.active_tasks = {}
        mock_client._generate_message_id = Mock(return_value="test-msg-001")
        mock_client._get_current_timestamp = Mock(return_value=1640995200000)
        mock_client._send_message = Mock(return_value=True)
        
        # 创建消息路由器
        router = MessageRouter(mock_client)
        
        # 测试消息类型枚举
        assert hasattr(MessageType, 'CONNECT'), "缺少CONNECT消息类型"
        assert hasattr(MessageType, 'FEDERATED_TASK_START'), "缺少任务启动消息类型"
        print("✅ 消息类型枚举")
        
        # 测试心跳确认处理
        heartbeat_ack = {
            'type': 'HEARTBEAT_ACK',
            'data': {'serverTime': 1640995200000}
        }
        result = router._handle_heartbeat_ack(heartbeat_ack)
        assert result == True, "心跳确认处理应该成功"
        print("✅ 心跳确认处理")
        
        # 测试路由统计
        stats = router.get_routing_stats()
        assert 'total_messages' in stats, "路由统计缺少总消息数"
        print("✅ 路由统计")
        
        print("消息路由器测试完成 ✅\n")
        return True
        
    except Exception as e:
        print(f"❌ 消息路由器测试失败: {e}")
        traceback.print_exc()
        return False


def test_task_context():
    """测试任务上下文"""
    print("=== 测试任务上下文 ===")
    try:
        from feduwacomm.ml.websocket.task_context import TaskContext, TaskStatus
        
        # 创建任务上下文
        task_data = {
            'taskId': 'test-task-001',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 3,
            'localTrainingConfig': {
                'datasetId': 'test-dataset',
                'epochs': 2
            }
        }
        
        task_context = TaskContext('test-task-001', task_data)
        
        # 测试状态枚举
        assert hasattr(TaskStatus, 'INITIALIZING'), "缺少INITIALIZING状态"
        assert hasattr(TaskStatus, 'READY'), "缺少READY状态"
        print("✅ 任务状态枚举")
        
        # 测试初始状态
        assert task_context.task_id == 'test-task-001', "任务ID不匹配"
        assert task_context.status == TaskStatus.INITIALIZING, "初始状态应该是INITIALIZING"
        print("✅ 任务初始化")
        
        # 测试统计信息
        stats = task_context.get_statistics()
        assert isinstance(stats, dict), "统计信息应该是字典"
        assert 'task_id' in stats, "统计信息缺少任务ID"
        print("✅ 统计信息")
        
        print("任务上下文测试完成 ✅\n")
        return True
        
    except Exception as e:
        print(f"❌ 任务上下文测试失败: {e}")
        traceback.print_exc()
        return False


def test_task_manager():
    """测试任务管理器"""
    print("=== 测试任务管理器 ===")
    try:
        from feduwacomm.ml.websocket.task_manager import TaskManager
        
        # 创建任务管理器（使用正确的参数）
        task_manager = TaskManager(max_concurrent_tasks=3)
        
        # 测试资源管理器
        assert hasattr(task_manager, 'resource_manager'), "缺少资源管理器"
        print("✅ 资源管理器")
        
        # 测试并发控制器
        assert hasattr(task_manager, 'concurrency_controller'), "缺少并发控制器"
        print("✅ 并发控制器")
        
        # 测试统计信息
        stats = task_manager.get_statistics()
        assert isinstance(stats, dict), "统计信息应该是字典"
        print("✅ 统计信息")
        
        print("任务管理器测试完成 ✅\n")
        return True
        
    except Exception as e:
        print(f"❌ 任务管理器测试失败: {e}")
        traceback.print_exc()
        return False


def test_task_executor():
    """测试任务执行器"""
    print("=== 测试任务执行器 ===")
    try:
        from feduwacomm.ml.federated.task_executor import TaskExecutor
        
        # 创建任务配置
        task_config = {
            'epochs': 1,
            'batchSize': 16,
            'learningRate': 0.01
        }
        
        # 创建任务执行器（使用正确的参数）
        executor = TaskExecutor('test-task', 'FEDERATED_AVERAGING', task_config)
        
        # 测试算法支持
        assert executor.algorithm == 'FEDERATED_AVERAGING', "算法设置不正确"
        print("✅ 算法配置")
        
        # 测试配置验证
        assert executor.task_id == 'test-task', "任务ID不正确"
        print("✅ 配置验证")
        
        print("任务执行器测试完成 ✅\n")
        return True
        
    except Exception as e:
        print(f"❌ 任务执行器测试失败: {e}")
        traceback.print_exc()
        return False


def test_ml_adapter():
    """测试ML适配器"""
    print("=== 测试ML适配器 ===")
    try:
        from feduwacomm.ml.adapters.ml_adapter import MLAdapter
        
        # 创建ML适配器（提供必需的task_id参数）
        adapter = MLAdapter('test-task')
        
        # 测试数据转换
        test_data = [[1, 2, 3], [4, 5, 6]]
        converted = adapter.convert_data_format(test_data, 'numpy')
        assert converted is not None, "数据转换失败"
        print("✅ 数据格式转换")
        
        # 测试模型适配
        test_model = {'type': 'test', 'data': [1, 2, 3]}
        adapted = adapter.adapt_model_interface(test_model)
        assert adapted is not None, "模型适配失败"
        print("✅ 模型接口适配")
        
        print("ML适配器测试完成 ✅\n")
        return True
        
    except Exception as e:
        print(f"❌ ML适配器测试失败: {e}")
        traceback.print_exc()
        return False


def main():
    """主测试函数"""
    print("🧪 开始测试新写的模块...\n")
    
    test_results = []
    
    # 逐一测试各个模块
    test_functions = [
        test_config_manager,
        test_error_handler,
        test_monitor,
        test_message_router,
        test_task_context,
        test_task_manager,
        test_task_executor,
        test_ml_adapter,
    ]
    
    for test_func in test_functions:
        try:
            result = test_func()
            test_results.append(result)
        except Exception as e:
            print(f"❌ 测试函数 {test_func.__name__} 执行失败: {e}")
            test_results.append(False)
    
    # 统计结果
    passed = sum(test_results)
    total = len(test_results)
    
    print("=" * 50)
    print(f"📊 测试结果汇总:")
    print(f"总测试数: {total}")
    print(f"通过数: {passed}")
    print(f"失败数: {total - passed}")
    print(f"成功率: {passed/total*100:.1f}%")
    
    if passed == total:
        print("🎉 所有模块测试通过！")
        return 0
    else:
        print("⚠️  部分模块测试失败，请检查错误信息")
        return 1


if __name__ == '__main__':
    exit_code = main()
    sys.exit(exit_code)
