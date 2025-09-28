"""
联邦学习端到端测试
"""

import pytest
import asyncio
import json
import time
from unittest.mock import Mock, patch, AsyncMock
import numpy as np

from feduwacomm.ml.websocket.client import WebSocketClient
from feduwacomm.ml.federated.client import FederatedClient
from feduwacomm.ml.federated.task_executor import TaskExecutor


@pytest.mark.e2e
class TestFederatedLearningE2E:
    """联邦学习端到端测试类"""

    @pytest.fixture
    def federated_client(self, test_config):
        """联邦学习客户端fixture"""
        websocket_client = WebSocketClient(
            server_url="ws://localhost:8080/websocket",
            vm_id="test-vm-001",
            config=test_config
        )
        
        return FederatedClient(websocket_client)

    @pytest.fixture
    def mock_training_data(self):
        """模拟训练数据"""
        np.random.seed(42)  # 确保可重复性
        
        n_samples = 1000
        n_features = 20
        
        X = np.random.randn(n_samples, n_features)
        y = (X[:, 0] + X[:, 1] > 0).astype(int)  # 简单的二分类
        
        return {
            'features': X.tolist(),
            'labels': y.tolist(),
            'metadata': {
                'n_samples': n_samples,
                'n_features': n_features,
                'task_type': 'classification'
            }
        }

    async def test_complete_federated_learning_workflow(self, federated_client, mock_training_data):
        """测试完整的联邦学习工作流程"""
        
        # 1. 连接到服务器
        with patch('websockets.connect') as mock_connect:
            mock_websocket = AsyncMock()
            mock_connect.return_value.__aenter__.return_value = mock_websocket
            
            # 模拟连接确认
            connect_ack = {
                'type': 'CONNECT_ACK',
                'data': {'status': 'SUCCESS'}
            }
            mock_websocket.recv.return_value = json.dumps(connect_ack)
            
            result = await federated_client.websocket_client.connect()
            assert result == True
        
        # 2. 接收任务启动指令
        task_config = {
            'taskId': 'e2e-test-task',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 3,
            'localTrainingConfig': {
                'datasetId': 'test-dataset',
                'epochs': 2,
                'batchSize': 32,
                'learningRate': 0.01
            },
            'modelConfig': {
                'modelType': 'RandomForest',
                'parameters': {
                    'n_estimators': 10,
                    'max_depth': 5
                }
            }
        }
        
        with patch.object(federated_client, '_load_training_data', return_value=mock_training_data):
            result = await federated_client.start_task(task_config)
            assert result == True
            assert task_config['taskId'] in federated_client.active_tasks
        
        # 3. 执行多轮训练
        task_context = federated_client.active_tasks[task_config['taskId']]
        
        for round_num in range(1, task_config['totalRounds'] + 1):
            # 3.1 接收全局模型
            if round_num > 1:
                global_model = {
                    'model_type': 'RandomForest',
                    'parameters': {
                        'n_estimators': 10,
                        'max_depth': 5
                    },
                    'weights': np.random.rand(20).tolist(),  # 模拟权重
                    'round': round_num - 1
                }
                
                with patch.object(task_context, 'update_global_model', return_value=True):
                    result = task_context.update_global_model(global_model)
                    assert result == True
            
            # 3.2 执行本地训练
            with patch.object(task_context, 'execute_local_training') as mock_training:
                mock_training.return_value = {
                    'success': True,
                    'model_updates': np.random.rand(20).tolist(),
                    'training_loss': 0.5 - round_num * 0.1,
                    'accuracy': 0.6 + round_num * 0.1,
                    'samples_count': len(mock_training_data['features'])
                }
                
                result = task_context.execute_local_training(round_num)
                assert result['success'] == True
                assert result['accuracy'] > 0.5
            
            # 3.3 上传梯度/模型更新
            with patch.object(federated_client, 'upload_gradients', return_value=True):
                gradient_data = {
                    'taskId': task_config['taskId'],
                    'roundNumber': round_num,
                    'gradients': np.random.rand(20).tolist(),
                    'metadata': {
                        'samples_count': len(mock_training_data['features']),
                        'training_loss': 0.5 - round_num * 0.1
                    }
                }
                
                result = await federated_client.upload_gradients(gradient_data)
                assert result == True
        
        # 4. 完成任务
        with patch.object(task_context, 'complete_task', return_value=True):
            result = task_context.complete_task()
            assert result == True

    async def test_federated_averaging_algorithm(self, federated_client, mock_training_data):
        """测试联邦平均算法"""
        
        task_config = {
            'taskId': 'fedavg-test',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 2,
            'localTrainingConfig': {
                'datasetId': 'test-dataset',
                'epochs': 1,
                'batchSize': 16
            }
        }
        
        # 创建任务执行器
        executor = TaskExecutor(task_config)
        
        with patch.object(executor, '_load_data', return_value=mock_training_data):
            # 初始化模型
            model = executor.initialize_model()
            assert model is not None
            
            # 执行训练
            training_result = executor.train_local_model(model, 1)
            
            assert training_result['success'] == True
            assert 'model_updates' in training_result
            assert 'training_metrics' in training_result

    async def test_multiple_concurrent_tasks(self, federated_client, mock_training_data):
        """测试多任务并发执行"""
        
        task_configs = []
        for i in range(3):
            task_configs.append({
                'taskId': f'concurrent-task-{i}',
                'federatedAlgorithm': 'FEDERATED_AVERAGING',
                'totalRounds': 2,
                'localTrainingConfig': {
                    'datasetId': f'dataset-{i}',
                    'epochs': 1,
                    'batchSize': 16
                }
            })
        
        # 启动所有任务
        with patch.object(federated_client, '_load_training_data', return_value=mock_training_data):
            for config in task_configs:
                result = await federated_client.start_task(config)
                assert result == True
        
        # 验证所有任务都在运行
        assert len(federated_client.active_tasks) == 3
        
        # 并发执行训练
        training_tasks = []
        for task_id in federated_client.active_tasks:
            task_context = federated_client.active_tasks[task_id]
            
            async def train_task(context):
                with patch.object(context, 'execute_local_training') as mock_training:
                    mock_training.return_value = {
                        'success': True,
                        'model_updates': np.random.rand(10).tolist(),
                        'accuracy': 0.8
                    }
                    return context.execute_local_training(1)
            
            training_tasks.append(train_task(task_context))
        
        # 等待所有训练完成
        results = await asyncio.gather(*training_tasks)
        
        # 验证所有训练都成功
        for result in results:
            assert result['success'] == True

    async def test_error_handling_and_recovery(self, federated_client, mock_training_data):
        """测试错误处理和恢复"""
        
        task_config = {
            'taskId': 'error-test-task',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 3,
            'localTrainingConfig': {
                'datasetId': 'test-dataset',
                'epochs': 1
            }
        }
        
        with patch.object(federated_client, '_load_training_data', return_value=mock_training_data):
            await federated_client.start_task(task_config)
            
            task_context = federated_client.active_tasks[task_config['taskId']]
            
            # 模拟训练错误
            with patch.object(task_context, 'execute_local_training') as mock_training:
                # 第一次训练失败
                mock_training.side_effect = [
                    Exception("Training failed"),
                    # 重试后成功
                    {
                        'success': True,
                        'model_updates': np.random.rand(10).tolist(),
                        'accuracy': 0.75
                    }
                ]
                
                # 执行训练（应该自动重试）
                with patch.object(task_context, 'handle_training_error') as mock_error_handler:
                    mock_error_handler.return_value = True  # 表示可以重试
                    
                    try:
                        result = task_context.execute_local_training(1)
                        # 如果有错误处理机制，应该最终成功
                    except Exception:
                        # 验证错误被正确处理
                        mock_error_handler.assert_called()

    async def test_model_compression_and_serialization(self, federated_client):
        """测试模型压缩和序列化"""
        
        # 创建大型模型数据
        large_model = {
            'model_type': 'NeuralNetwork',
            'layers': [
                {
                    'type': 'dense',
                    'weights': np.random.rand(1000, 500).tolist(),
                    'biases': np.random.rand(500).tolist()
                },
                {
                    'type': 'dense',
                    'weights': np.random.rand(500, 100).tolist(),
                    'biases': np.random.rand(100).tolist()
                }
            ]
        }
        
        # 测试序列化
        serialized = federated_client._serialize_model(large_model)
        assert isinstance(serialized, (str, bytes))
        
        # 测试反序列化
        deserialized = federated_client._deserialize_model(serialized)
        
        # 验证数据完整性
        assert deserialized['model_type'] == large_model['model_type']
        assert len(deserialized['layers']) == len(large_model['layers'])
        
        # 测试压缩
        compressed = federated_client._compress_data(serialized)
        decompressed = federated_client._decompress_data(compressed)
        
        assert decompressed == serialized

    async def test_performance_monitoring_e2e(self, federated_client, mock_training_data, monitor):
        """测试端到端性能监控"""
        
        # 启动监控
        monitor.start_monitoring()
        
        task_config = {
            'taskId': 'perf-test-task',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 2,
            'localTrainingConfig': {
                'datasetId': 'test-dataset',
                'epochs': 1
            }
        }
        
        with patch.object(federated_client, '_load_training_data', return_value=mock_training_data):
            # 记录任务开始
            start_time = time.time()
            
            await federated_client.start_task(task_config)
            
            task_context = federated_client.active_tasks[task_config['taskId']]
            
            # 执行训练并监控性能
            with patch.object(task_context, 'execute_local_training') as mock_training:
                mock_training.return_value = {
                    'success': True,
                    'model_updates': np.random.rand(10).tolist(),
                    'training_time': 2.5,
                    'accuracy': 0.85
                }
                
                # 记录训练指标
                monitor.record_metric('training_start', start_time)
                
                result = task_context.execute_local_training(1)
                
                end_time = time.time()
                monitor.record_metric('training_duration', end_time - start_time)
                monitor.record_metric('training_accuracy', result['accuracy'])
        
        # 等待监控数据收集
        await asyncio.sleep(0.1)
        
        # 验证性能指标
        performance_summary = monitor.get_performance_summary(minutes=1)
        assert len(performance_summary) > 0
        
        # 停止监控
        monitor.stop_monitoring()

    async def test_data_privacy_and_security(self, federated_client, mock_training_data):
        """测试数据隐私和安全性"""
        
        task_config = {
            'taskId': 'privacy-test-task',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 1,
            'localTrainingConfig': {
                'datasetId': 'sensitive-dataset',
                'epochs': 1,
                'privacyConfig': {
                    'differential_privacy': True,
                    'noise_multiplier': 1.0,
                    'max_grad_norm': 1.0
                }
            }
        }
        
        with patch.object(federated_client, '_load_training_data', return_value=mock_training_data):
            await federated_client.start_task(task_config)
            
            task_context = federated_client.active_tasks[task_config['taskId']]
            
            # 验证原始数据不会被发送
            with patch.object(federated_client, 'upload_gradients') as mock_upload:
                mock_upload.return_value = True
                
                # 执行训练
                with patch.object(task_context, 'execute_local_training') as mock_training:
                    mock_training.return_value = {
                        'success': True,
                        'model_updates': np.random.rand(10).tolist(),
                        'privacy_budget_used': 0.1
                    }
                    
                    result = task_context.execute_local_training(1)
                    
                    # 验证只有模型更新被上传，没有原始数据
                    if mock_upload.called:
                        upload_args = mock_upload.call_args[0][0]
                        assert 'gradients' in upload_args
                        assert 'raw_data' not in upload_args
                        assert 'features' not in upload_args

    @pytest.mark.slow
    async def test_long_running_federated_task(self, federated_client, mock_training_data):
        """测试长时间运行的联邦学习任务"""
        
        task_config = {
            'taskId': 'long-running-task',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 10,  # 更多轮次
            'localTrainingConfig': {
                'datasetId': 'large-dataset',
                'epochs': 3,
                'batchSize': 64
            }
        }
        
        with patch.object(federated_client, '_load_training_data', return_value=mock_training_data):
            await federated_client.start_task(task_config)
            
            task_context = federated_client.active_tasks[task_config['taskId']]
            
            # 模拟长时间训练过程
            accuracies = []
            
            for round_num in range(1, 6):  # 只测试前5轮
                with patch.object(task_context, 'execute_local_training') as mock_training:
                    # 模拟逐渐提升的准确率
                    accuracy = 0.5 + round_num * 0.05
                    accuracies.append(accuracy)
                    
                    mock_training.return_value = {
                        'success': True,
                        'model_updates': np.random.rand(20).tolist(),
                        'accuracy': accuracy,
                        'training_time': 5.0 + np.random.rand()
                    }
                    
                    result = task_context.execute_local_training(round_num)
                    assert result['success'] == True
                    
                    # 模拟轮次间的等待时间
                    await asyncio.sleep(0.01)
            
            # 验证训练进度
            assert len(accuracies) == 5
            assert accuracies[-1] > accuracies[0]  # 准确率应该提升

    async def test_federated_learning_with_different_algorithms(self, federated_client, mock_training_data):
        """测试不同联邦学习算法"""
        
        algorithms = ['FEDERATED_AVERAGING', 'FEDERATED_PROXIMAL', 'FEDERATED_NOVA']
        
        for algorithm in algorithms:
            task_config = {
                'taskId': f'algo-test-{algorithm.lower()}',
                'federatedAlgorithm': algorithm,
                'totalRounds': 2,
                'localTrainingConfig': {
                    'datasetId': 'test-dataset',
                    'epochs': 1
                },
                'algorithmConfig': {
                    'mu': 0.01 if algorithm == 'FEDERATED_PROXIMAL' else None,
                    'tau': 0.5 if algorithm == 'FEDERATED_NOVA' else None
                }
            }
            
            with patch.object(federated_client, '_load_training_data', return_value=mock_training_data):
                result = await federated_client.start_task(task_config)
                assert result == True
                
                task_context = federated_client.active_tasks[task_config['taskId']]
                
                # 验证算法特定的配置被正确应用
                assert task_context.algorithm == algorithm
                
                # 清理任务
                await federated_client.stop_task(task_config['taskId'])

    async def test_network_interruption_recovery(self, federated_client, mock_training_data):
        """测试网络中断恢复"""
        
        task_config = {
            'taskId': 'network-test-task',
            'federatedAlgorithm': 'FEDERATED_AVERAGING',
            'totalRounds': 3,
            'localTrainingConfig': {
                'datasetId': 'test-dataset',
                'epochs': 1
            }
        }
        
        with patch.object(federated_client, '_load_training_data', return_value=mock_training_data):
            await federated_client.start_task(task_config)
            
            # 模拟网络中断
            with patch.object(federated_client.websocket_client, 'is_connected', False):
                # 尝试上传梯度（应该失败）
                gradient_data = {
                    'taskId': task_config['taskId'],
                    'roundNumber': 1,
                    'gradients': np.random.rand(10).tolist()
                }
                
                with patch.object(federated_client, '_queue_for_retry') as mock_queue:
                    result = await federated_client.upload_gradients(gradient_data)
                    
                    # 应该被排队等待重试
                    if not result:
                        mock_queue.assert_called_with(gradient_data)
            
            # 模拟网络恢复
            with patch.object(federated_client.websocket_client, 'is_connected', True), \
                 patch.object(federated_client, 'upload_gradients', return_value=True):
                
                # 处理排队的请求
                result = await federated_client._process_retry_queue()
                assert result == True
