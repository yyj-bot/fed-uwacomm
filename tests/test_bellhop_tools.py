"""
Tests for Bellhop tools module
"""
import pytest
import os
import tempfile
from src.preprocessing.bellhop_tools import BellhopFileManager, BellhopManager


class TestBellhopFileManager:
    """Test BellhopFileManager class"""
    
    def test_generate_env_file(self):
        """Test environment file generation"""
        manager = BellhopFileManager()
        with tempfile.TemporaryDirectory() as temp_dir:
            env_file = os.path.join(temp_dir, "test.env")
            manager.generate_env_file(env_file, 1, 1000.0)
            
            assert os.path.exists(env_file)
            with open(env_file, 'r') as f:
                content = f.read()
                assert "test case" in content
                assert "1000.0" in content


class TestBellhopManager:
    """Test BellhopManager class"""
    
    def test_init(self):
        """Test BellhopManager initialization"""
        manager = BellhopManager()
        assert manager is not None


if __name__ == "__main__":
    pytest.main([__file__]) 