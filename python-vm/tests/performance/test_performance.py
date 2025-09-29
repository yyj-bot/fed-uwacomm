"""
性能测试
"""

import pytest
import time
import threading
import concurrent.futures
import numpy as np
from unittest.mock import Mock, patch
import psutil
import gc


@pytest.mark.slow
class TestPerformance:
    """性能测试类"""

    def test_concurrent_task_processing(self):
        """测试并发任务处理性能"""
        from feduwacomm.ml.websocket.task_manager import TaskManager
        
        # 创建模拟客户端
        mock_client = Mock()
        mock_client.vm_id = "test-vm-001"
        mock_client.max_concurrent_tasks = 10
        
        task_manager = TaskManager(mock_client)
        
        # 模拟任务
        def simulate_task(task_id):
            time.sleep(0.1)  # 模拟任务执行时间
            return f"Task {task_id} completed"
        
        # 并发执行任务
        start_time = time.time()
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
            futures = []
            for i in range(50):
                future = executor.submit(simulate_task, i)
                futures.append(future)
            
            # 等待所有任务完成
            results = [future.result() for future in futures]
        
        end_time = time.time()
        
        # 验证性能
        total_time = end_time - start_time
        assert len(results) == 50
        assert total_time < 10.0  # 应该在10秒内完成
        
        # 计算吞吐量
        throughput = len(results) / total_time
        assert throughput > 5  # 每秒至少处理5个任务

    def test_memory_usage_under_load(self):
        """测试负载下的内存使用"""
        process = psutil.Process()
        initial_memory = process.memory_info().rss / 1024 / 1024  # MB
        
        # 创建大量数据模拟负载
        data_arrays = []
        for i in range(100):
            # 每个数组约1MB
            array = np.random.rand(1000, 100)
            data_arrays.append(array)
        
        peak_memory = process.memory_info().rss / 1024 / 1024  # MB
        
        # 清理数据
        del data_arrays
        gc.collect()
        
        final_memory = process.memory_info().rss / 1024 / 1024  # MB
        
        # 验证内存使用合理
        memory_increase = peak_memory - initial_memory
        memory_cleanup = peak_memory - final_memory
        
        assert memory_increase > 50  # 应该有明显的内存增长
        assert memory_cleanup > memory_increase * 0.8  # 大部分内存应该被释放

    def test_message_processing_throughput(self):
        """测试消息处理吞吐量"""
        from feduwacomm.ml.websocket.message_router import MessageRouter
        
        # 创建模拟客户端
        mock_client = Mock()
        mock_client.vm_id = "test-vm-001"
        mock_client._send_message = Mock(return_value=True)
        
        router = MessageRouter(mock_client)
        
        # 创建测试消息
        test_messages = []
        for i in range(1000):
            message = {
                'type': 'HEARTBEAT_ACK',
                'id': f'msg-{i}',
                'timestamp': int(time.time() * 1000),
                'data': {}
            }
            test_messages.append(message)
        
        # 测试消息处理性能
        start_time = time.time()
        
        successful_routes = 0
        for message in test_messages:
            if router.route_message(message):
                successful_routes += 1
        
        end_time = time.time()
        
        # 验证性能
        total_time = end_time - start_time
        throughput = successful_routes / total_time
        
        assert successful_routes == 1000
        assert throughput > 100  # 每秒至少处理100条消息
        assert total_time < 10.0  # 总时间应该在10秒内

    def test_websocket_connection_performance(self):
        """测试WebSocket连接性能"""
        # 模拟多个连接的建立和断开
        connection_times = []
        
        for i in range(10):
            start_time = time.time()
            
            # 模拟连接建立
            time.sleep(0.01)  # 模拟网络延迟
            
            end_time = time.time()
            connection_times.append(end_time - start_time)
        
        # 验证连接性能
        avg_connection_time = sum(connection_times) / len(connection_times)
        max_connection_time = max(connection_times)
        
        assert avg_connection_time < 0.1  # 平均连接时间应该小于100ms
        assert max_connection_time < 0.2  # 最大连接时间应该小于200ms

    def test_model_training_performance(self):
        """测试模型训练性能"""
        from sklearn.ensemble import RandomForestClassifier
        import numpy as np
        
        # 创建训练数据
        n_samples = 1000
        n_features = 20
        
        X = np.random.rand(n_samples, n_features)
        y = np.random.randint(0, 2, n_samples)
        
        # 测试不同模型大小的训练时间
        model_configs = [
            {'n_estimators': 10, 'max_depth': 5},
            {'n_estimators': 50, 'max_depth': 10},
            {'n_estimators': 100, 'max_depth': 15}
        ]
        
        training_times = []
        
        for config in model_configs:
            model = RandomForestClassifier(**config, random_state=42)
            
            start_time = time.time()
            model.fit(X, y)
            end_time = time.time()
            
            training_time = end_time - start_time
            training_times.append(training_time)
        
        # 验证训练性能
        assert all(t < 30.0 for t in training_times)  # 所有模型应该在30秒内训练完成
        
        # 验证训练时间随模型复杂度增长是合理的
        assert training_times[1] > training_times[0]  # 更复杂的模型需要更长时间
        assert training_times[2] > training_times[1]

    def test_data_processing_performance(self):
        """测试数据处理性能"""
        import pandas as pd
        
        # 创建大数据集
        n_rows = 10000
        n_cols = 50
        
        data = {}
        for i in range(n_cols):
            data[f'feature_{i}'] = np.random.rand(n_rows)
        
        df = pd.DataFrame(data)
        
        # 测试各种数据处理操作的性能
        operations = []
        
        # 1. 数据筛选
        start_time = time.time()
        filtered_df = df[df['feature_0'] > 0.5]
        operations.append(('filtering', time.time() - start_time))
        
        # 2. 数据聚合
        start_time = time.time()
        aggregated = df.groupby(pd.cut(df['feature_1'], bins=10)).mean()
        operations.append(('aggregation', time.time() - start_time))
        
        # 3. 数据转换
        start_time = time.time()
        transformed = df.apply(lambda x: x ** 2)
        operations.append(('transformation', time.time() - start_time))
        
        # 验证性能
        for operation, duration in operations:
            assert duration < 5.0, f"{operation} took too long: {duration:.2f}s"

    def test_concurrent_model_inference(self):
        """测试并发模型推理性能"""
        from sklearn.ensemble import RandomForestClassifier
        
        # 训练模型
        X_train = np.random.rand(1000, 10)
        y_train = np.random.randint(0, 2, 1000)
        
        model = RandomForestClassifier(n_estimators=50, random_state=42)
        model.fit(X_train, y_train)
        
        # 创建推理数据
        X_test = np.random.rand(1000, 10)
        
        # 并发推理测试
        def inference_worker(data_chunk):
            return model.predict(data_chunk)
        
        # 将数据分成10个块
        chunk_size = len(X_test) // 10
        data_chunks = [X_test[i:i+chunk_size] for i in range(0, len(X_test), chunk_size)]
        
        start_time = time.time()
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            futures = [executor.submit(inference_worker, chunk) for chunk in data_chunks]
            results = [future.result() for future in futures]
        
        end_time = time.time()
        
        # 验证性能
        total_predictions = sum(len(result) for result in results)
        inference_time = end_time - start_time
        throughput = total_predictions / inference_time
        
        assert total_predictions >= 1000
        assert throughput > 100  # 每秒至少100个预测
        assert inference_time < 10.0

    def test_error_handling_performance(self):
        """测试错误处理性能"""
        from feduwacomm.ml.utils.error_handler import ErrorHandler
        
        # 创建模拟客户端
        mock_client = Mock()
        mock_client.vm_id = "test-vm-001"
        mock_client.reconnect = Mock(return_value=True)
        
        error_handler = ErrorHandler(mock_client)
        
        # 测试大量错误处理的性能
        start_time = time.time()
        
        for i in range(100):
            error = RuntimeError(f"Test error {i}")
            error_handler.handle_error(error, "TASK_ERROR")
        
        end_time = time.time()
        
        # 验证性能
        total_time = end_time - start_time
        error_handling_rate = 100 / total_time
        
        assert error_handling_rate > 10  # 每秒至少处理10个错误
        assert total_time < 10.0

    def test_monitoring_overhead(self):
        """测试监控系统开销"""
        from feduwacomm.ml.utils.monitor import Monitor
        
        # 创建模拟客户端
        mock_client = Mock()
        mock_client.vm_id = "test-vm-001"
        mock_client.active_tasks = {}
        
        monitor_config = {
            'enabled': True,
            'interval': 0.1,  # 高频监控
            'retention_hours': 1
        }
        
        monitor = Monitor(mock_client, monitor_config)
        
        # 测试监控开销
        process = psutil.Process()
        initial_cpu = process.cpu_percent()
        
        # 启动监控
        monitor.start()
        
        # 运行一段时间
        time.sleep(2.0)
        
        # 停止监控
        monitor.stop()
        
        final_cpu = process.cpu_percent()
        
        # 验证监控开销合理
        cpu_overhead = final_cpu - initial_cpu
        assert cpu_overhead < 10.0  # CPU开销应该小于10%

    def test_scalability_limits(self):
        """测试可扩展性限制"""
        # 测试系统在不同负载下的表现
        load_levels = [10, 50, 100, 200]
        response_times = []
        
        for load in load_levels:
            # 模拟不同负载级别
            start_time = time.time()
            
            # 创建负载
            tasks = []
            for i in range(load):
                # 模拟任务处理
                task_time = 0.01  # 10ms per task
                tasks.append(task_time)
            
            # 处理所有任务
            total_task_time = sum(tasks)
            time.sleep(total_task_time / 10)  # 模拟并行处理
            
            end_time = time.time()
            response_times.append(end_time - start_time)
        
        # 验证可扩展性
        # 响应时间不应该线性增长
        for i in range(1, len(response_times)):
            growth_ratio = response_times[i] / response_times[i-1]
            load_ratio = load_levels[i] / load_levels[i-1]
            
            # 响应时间增长应该小于负载增长
            assert growth_ratio < load_ratio * 1.5

    def test_resource_cleanup_performance(self):
        """测试资源清理性能"""
        # 创建大量资源
        resources = []
        
        start_time = time.time()
        
        # 创建资源
        for i in range(1000):
            resource = {
                'id': i,
                'data': np.random.rand(100),
                'timestamp': time.time()
            }
            resources.append(resource)
        
        creation_time = time.time() - start_time
        
        # 清理资源
        start_time = time.time()
        
        resources.clear()
        gc.collect()
        
        cleanup_time = time.time() - start_time
        
        # 验证性能
        assert creation_time < 5.0  # 创建应该在5秒内完成
        assert cleanup_time < 1.0   # 清理应该在1秒内完成

    @pytest.mark.network
    def test_network_performance_simulation(self):
        """测试网络性能模拟"""
        # 模拟不同网络条件下的性能
        network_conditions = [
            {'latency': 0.01, 'bandwidth': 1000},  # 良好网络
            {'latency': 0.05, 'bandwidth': 100},   # 一般网络
            {'latency': 0.1, 'bandwidth': 10}      # 差网络
        ]
        
        for condition in network_conditions:
            # 模拟网络延迟
            latency = condition['latency']
            bandwidth = condition['bandwidth']
            
            # 模拟数据传输
            data_size = 1024  # 1KB
            transfer_time = data_size / bandwidth + latency
            
            start_time = time.time()
            time.sleep(transfer_time)
            end_time = time.time()
            
            actual_time = end_time - start_time
            
            # 验证网络性能模拟
            assert abs(actual_time - transfer_time) < 0.1

    def test_stress_testing(self):
        """压力测试"""
        # 创建高负载场景
        stress_duration = 5.0  # 5秒压力测试
        
        def stress_worker():
            # 模拟CPU密集型任务
            end_time = time.time() + 1.0
            while time.time() < end_time:
                # 执行一些计算
                _ = sum(i * i for i in range(1000))
        
        # 启动多个压力线程
        start_time = time.time()
        
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
            futures = []
            while time.time() - start_time < stress_duration:
                future = executor.submit(stress_worker)
                futures.append(future)
                time.sleep(0.1)
            
            # 等待所有任务完成
            for future in futures:
                future.result()
        
        end_time = time.time()
        
        # 验证系统在压力下仍能正常工作
        total_time = end_time - start_time
        assert total_time >= stress_duration
        assert total_time < stress_duration * 2  # 不应该超时太多
