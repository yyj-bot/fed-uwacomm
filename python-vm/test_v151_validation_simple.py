# -*- coding: utf-8 -*-
"""
v1.5.1 Update Validation Script
"""
import sys
import os

# Add path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

def test_imports():
    """Test module imports"""
    print("=" * 60)
    print("Test 1: Module Import Validation")
    print("=" * 60)
    
    try:
        # Test direct import
        sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src', 'feduwacomm', 'ml'))
        
        from storage.dataset_manager import DatasetManager, DatasetInfo, DatasetStatus
        print("[PASS] DatasetManager import successful")
        print("[PASS] DatasetInfo import successful")
        print("[PASS] DatasetStatus import successful")
    except Exception as e:
        print(f"[FAIL] Import failed: {e}")
        return False
    
    try:
        from storage.sqlite_storage import VMStorage
        print("[PASS] VMStorage import successful")
    except Exception as e:
        print(f"[FAIL] VMStorage import failed: {e}")
        return False
    
    try:
        from websocket.message_router import MessageType, MessageRouter
        print("[PASS] MessageRouter import successful")
        print("[PASS] MessageType import successful")
    except Exception as e:
        print(f"[FAIL] MessageRouter import failed: {e}")
        return False
    
    return True

def test_protocol_enum():
    """Test protocol enumeration"""
    print("\n" + "=" * 60)
    print("Test 2: Protocol Enum Validation")
    print("=" * 60)
    
    try:
        from websocket.message_router import MessageType
        
        # v1.5 new protocols
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
            print(f"[FAIL] Missing protocols: {missing}")
            return False
        
        print(f"[PASS] All 6 v1.5 protocols exist")
        
        # Count total protocols
        protocol_count = len([attr for attr in dir(MessageType) if not attr.startswith('_') and attr.isupper()])
        print(f"[INFO] Total protocols: {protocol_count}")
        
        return True
    except Exception as e:
        print(f"[FAIL] Protocol enum validation failed: {e}")
        return False

def test_dataset_manager():
    """Test DatasetManager"""
    print("\n" + "=" * 60)
    print("Test 3: DatasetManager Functionality")
    print("=" * 60)
    
    try:
        from storage.dataset_manager import DatasetManager
        
        # Create manager
        manager = DatasetManager()
        print("[PASS] DatasetManager instantiated")
        
        # Check core methods
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
            print(f"[FAIL] Missing methods: {missing_methods}")
            return False
        
        print(f"[PASS] All core methods exist")
        
        # Test basic functionality
        success = manager.create_dataset(
            task_id="test-task-001",
            assigned_dataset_id="test-dataset-001",
            dataset_name="Test Dataset",
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
            print("[PASS] Dataset creation works")
        else:
            print("[FAIL] Dataset creation failed")
            return False
        
        # Get statistics
        stats = manager.get_statistics()
        print(f"[PASS] Statistics work: {stats['totalDatasets']} datasets")
        
        return True
    except Exception as e:
        print(f"[FAIL] DatasetManager test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_message_handlers():
    """Test message handlers"""
    print("\n" + "=" * 60)
    print("Test 4: Message Handler Validation")
    print("=" * 60)
    
    try:
        from websocket.message_router import MessageRouter, MessageType
        
        # Create mock client
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
        print("[PASS] MessageRouter instantiated")
        
        # Check handlers
        handler_count = len(router.message_handlers)
        print(f"[INFO] Message handlers: {handler_count}")
        
        # Check v1.5.1 new handlers
        v151_handlers = [
            'DATASET_LIST_QUERY',
            'DATASET_STATUS_QUERY',
            'MESSAGE_ERROR',
            'CONNECTION_ERROR'
        ]
        
        missing_handlers = []
        for handler in v151_handlers:
            enum_value = getattr(MessageType, handler).value
            if enum_value not in router.message_handlers:
                missing_handlers.append(handler)
        
        if missing_handlers:
            print(f"[FAIL] Missing handlers: {missing_handlers}")
            return False
        
        print(f"[PASS] All v1.5.1 handlers registered")
        
        return True
    except Exception as e:
        print(f"[FAIL] MessageRouter test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    """Main function"""
    print("\n")
    print("=" * 60)
    print("    v1.5.1 Update Validation Test")
    print("=" * 60)
    
    results = []
    
    # Run all tests
    results.append(("Module Import", test_imports()))
    results.append(("Protocol Enum", test_protocol_enum()))
    results.append(("DatasetManager", test_dataset_manager()))
    results.append(("Message Handlers", test_message_handlers()))
    
    # Summary
    print("\n" + "=" * 60)
    print("Test Results Summary")
    print("=" * 60)
    
    passed = 0
    failed = 0
    for name, result in results:
        status = "[PASS]" if result else "[FAIL]"
        print(f"{name:20s} : {status}")
        if result:
            passed += 1
        else:
            failed += 1
    
    print("-" * 60)
    print(f"Total: {len(results)} tests, {passed} passed, {failed} failed")
    print("=" * 60)
    
    if failed == 0:
        print("\n[SUCCESS] All tests passed! v1.5.1 update validated!")
        return 0
    else:
        print(f"\n[FAIL] {failed} test(s) failed, please check error messages")
        return 1

if __name__ == "__main__":
    sys.exit(main())

