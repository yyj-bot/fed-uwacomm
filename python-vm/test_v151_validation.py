"""
v1.5.1 更新验证脚本
"""
import sys
import os

# 添加路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

def test_imports():
    """测试模块导入"""
    print("=" * 60)
    print("测试 1: 模块导入验证")
    print("=" * 60)
    
    try:
        from feduwacomm.ml.storage import DatasetManager, DatasetInfo, DatasetStatus
        print("✓ DatasetManager 导入成功")
        print("✓ DatasetInfo 导入成功")
        print("✓ DatasetStatus 导入成功")
    except Exception as e:
        print(f"✗ 导入失败: {e}")
        return False
    
    try:
        from feduwacomm.ml.storage import VMStorage
        print("✓ VMStorage 导入成功")
    except Exception as e:
        print(f"✗ VMStorage 导入失败: {e}")
        return False
    
    try:
        from feduwacomm.ml.websocket.message_router import MessageType, MessageRouter
        print("✓ MessageRouter 导入成功")
        print("✓ MessageType 导入成功")
    except Exception as e:
        print(f"✗ MessageRouter 导入失败: {e}")
        return False
    
    return True

def test_protocol_enum():
    """测试协议枚举"""
    print("\n" + "=" * 60)
    print("测试 2: 协议枚举验证")
    print("=" * 60)
    
    try:
        from feduwacomm.ml.websocket.message_router import MessageType
        
        # v1.5新增协议
        v15_protocols = [
            "DATASET_LIST_QUERY",
            "DATASET_LIST_RESPONSE",
            "DATASET_STATUS_QUERY",
            "DATASET_STATUS_RESPONSE",
            "MESSAGE_ERROR",
            "CONNECTION_ERROR"
        ]
        
        missing = []
        for protocol in v15_protocols:
            if not hasattr(MessageType, protocol):
                missing.append(protocol)
        
        if missing:
            print(f"✗ 缺少协议: {missing}")
            return False
        
        print(f"✓ v1.5新增的6个协议全部存在")
        
        # 统计协议总数
        protocol_count = len([attr for attr in dir(MessageType) if not attr.startswith('_')])
        print(f"✓ 协议总数: {protocol_count}")
        
        return True
    except Exception as e:
        print(f"✗ 协议枚举验证失败: {e}")
        return False

def test_dataset_manager():
    """测试数据集管理器"""
    print("\n" + "=" * 60)
    print("测试 3: DatasetManager功能验证")
    print("=" * 60)
    
    try:
        from feduwacomm.ml.storage import DatasetManager
        
        # 创建管理器
        manager = DatasetManager()
        print("✓ DatasetManager 实例化成功")
        
        # 检查核心方法
        required_methods = [
            'create_dataset',
            'append_rows',
            'complete_dataset',
            'get_dataset_status',
            'get_available_datasets',
            'delete_dataset'
        ]
        
        missing_methods = []
        for method in required_methods:
            if not hasattr(manager, method):
                missing_methods.append(method)
        
        if missing_methods:
            print(f"✗ 缺少方法: {missing_methods}")
            return False
        
        print(f"✓ 所有核心方法存在: {', '.join(required_methods)}")
        
        # 测试基本功能
        success = manager.create_dataset(
            task_id="test-task-001",
            assigned_dataset_id="test-dataset-001",
            dataset_name="测试数据集",
            expected_rows=100,
            slice_info={
                "startIndex": 0,
                "endIndex": 99,
                "sliceSamples": 100,
                "totalSamples": 1000,
                "sliceIndex": 1,
                "totalSlices": 10,
                "allocationStrategy": "IID"
            }
        )
        
        if success:
            print("✓ 数据集创建功能正常")
        else:
            print("✗ 数据集创建失败")
            return False
        
        # 获取统计
        stats = manager.get_statistics()
        print(f"✓ 统计功能正常: {stats}")
        
        return True
    except Exception as e:
        print(f"✗ DatasetManager测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_sqlite_storage():
    """测试SQLite存储"""
    print("\n" + "=" * 60)
    print("测试 4: SQLite存储验证")
    print("=" * 60)
    
    try:
        from feduwacomm.ml.storage import VMStorage
        import tempfile
        import os
        
        # 创建临时数据库
        temp_db = os.path.join(tempfile.gettempdir(), "test_vm_storage.db")
        storage = VMStorage(temp_db)
        print("✓ VMStorage 实例化成功")
        
        # 检查v1.5.1新增方法
        v151_methods = [
            'save_dataset',
            'save_dataset_rows',
            'update_dataset_status',
            'get_dataset_info',
            'get_dataset_rows',
            'verify_dataset_completeness',
            'delete_dataset'
        ]
        
        missing_methods = []
        for method in v151_methods:
            if not hasattr(storage, method):
                missing_methods.append(method)
        
        if missing_methods:
            print(f"✗ 缺少v1.5.1方法: {missing_methods}")
            return False
        
        print(f"✓ 所有v1.5.1新增方法存在: {len(v151_methods)}个")
        
        # 测试数据集保存
        success = storage.save_dataset(
            assigned_dataset_id="test-dataset-002",
            task_id="test-task-002",
            dataset_name="测试数据集2",
            expected_rows=50,
            slice_info={
                "startIndex": 0,
                "endIndex": 49,
                "sliceSamples": 50
            }
        )
        
        if success:
            print("✓ 数据集保存功能正常")
        else:
            print("✗ 数据集保存失败")
            return False
        
        # 清理
        try:
            os.remove(temp_db)
        except:
            pass
        
        return True
    except Exception as e:
        print(f"✗ SQLite存储测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_message_router():
    """测试消息路由器"""
    print("\n" + "=" * 60)
    print("测试 5: MessageRouter验证")
    print("=" * 60)
    
    try:
        from feduwacomm.ml.websocket.message_router import MessageRouter
        
        # 创建模拟客户端
        class MockClient:
            def __init__(self):
                self.vm_id = "test-vm-001"
                self.active_tasks = {}
                self.dataset_manager = None
            
            def _generate_message_id(self):
                return "test-msg-001"
            
            def _get_current_timestamp(self):
                return "2025-01-30T00:00:00.000Z"
            
            def _send_message(self, msg):
                return True
        
        client = MockClient()
        router = MessageRouter(client)
        print("✓ MessageRouter 实例化成功")
        
        # 检查处理器
        handler_count = len(router.message_handlers)
        print(f"✓ 消息处理器数量: {handler_count}")
        
        # 检查v1.5.1新增处理器
        v151_handlers = [
            'DATASET_LIST_QUERY',
            'DATASET_STATUS_QUERY',
            'MESSAGE_ERROR',
            'CONNECTION_ERROR'
        ]
        
        from feduwacomm.ml.websocket.message_router import MessageType
        missing_handlers = []
        for handler in v151_handlers:
            enum_value = getattr(MessageType, handler).value
            if enum_value not in router.message_handlers:
                missing_handlers.append(handler)
        
        if missing_handlers:
            print(f"✗ 缺少处理器: {missing_handlers}")
            return False
        
        print(f"✓ 所有v1.5.1处理器已注册")
        
        return True
    except Exception as e:
        print(f"✗ MessageRouter测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    """主函数"""
    print("\n")
    print("╔" + "=" * 58 + "╗")
    print("║" + " " * 15 + "v1.5.1 更新验证测试" + " " * 15 + "║")
    print("╚" + "=" * 58 + "╝")
    
    results = []
    
    # 运行所有测试
    results.append(("模块导入", test_imports()))
    results.append(("协议枚举", test_protocol_enum()))
    results.append(("DatasetManager", test_dataset_manager()))
    results.append(("SQLite存储", test_sqlite_storage()))
    results.append(("MessageRouter", test_message_router()))
    
    # 汇总结果
    print("\n" + "=" * 60)
    print("测试结果汇总")
    print("=" * 60)
    
    passed = 0
    failed = 0
    for name, result in results:
        status = "✓ 通过" if result else "✗ 失败"
        print(f"{name:20s} : {status}")
        if result:
            passed += 1
        else:
            failed += 1
    
    print("-" * 60)
    print(f"总计: {len(results)} 个测试, {passed} 个通过, {failed} 个失败")
    print("=" * 60)
    
    if failed == 0:
        print("\n✓ 所有测试通过！v1.5.1更新验证成功！")
        return 0
    else:
        print(f"\n✗ 有 {failed} 个测试失败，请检查错误信息")
        return 1

if __name__ == "__main__":
    sys.exit(main())

