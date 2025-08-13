#!/usr/bin/env python3
"""
BELLHOP Batch Processing Tool
Automatically call BELLHOP model to process all environment files
"""

import os
import subprocess
import glob
import time
from pathlib import Path
import logging

class BellhopProcessor:
    def __init__(self, bellhop_dir=None):
        # 使用相对于python-vm的路径
        if bellhop_dir is None:
            # 获取当前脚本的路径
            script_dir = Path(__file__).resolve().parent
            # 计算python-vm的路径
            python_vm_dir = script_dir.parent.parent.parent
            # 设置BELLHOP目录为python-vm/data/bellhop
            bellhop_dir = python_vm_dir / "data" / "bellhop"
        
        self.bellhop_dir = Path(bellhop_dir).resolve()
        self.setup_logging()
        self.logger.info(f"使用BELLHOP目录: {self.bellhop_dir}")
        self.bellhop_exe = self.find_bellhop_executable()
        
    def setup_logging(self):
        logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
        self.logger = logging.getLogger(__name__)
        
    def find_bellhop_executable(self):
        possible_exes = [
            self.bellhop_dir / "bellhop.exe",
            self.bellhop_dir / "bellhopf.exe",
        ]
        
        for exe_path in possible_exes:
            if exe_path.exists():
                self.logger.info(f"Found BELLHOP: {exe_path}")
                return str(exe_path)
        
        self.logger.error("BELLHOP executable not found")
        return None
    
    def run_bellhop(self, env_file):
        env_name = Path(env_file).stem
        self.logger.info(f"Processing: {env_name}")
        
        try:
            original_dir = os.getcwd()
            os.chdir(self.bellhop_dir)
            
            if os.path.isabs(self.bellhop_exe):
                cmd = [self.bellhop_exe, env_name]
            else:
                cmd = [Path(self.bellhop_exe).name, env_name]
            
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
            
            if result.returncode == 0:
                self.logger.info(f"+ {env_name} success")
                return True
            else:
                self.logger.error(f"- {env_name} failed")
                return False
                
        except Exception as e:
            self.logger.error(f"- {env_name} exception: {e}")
            return False
        finally:
            os.chdir(original_dir)
    
    def process_all(self):
        env_files = sorted(glob.glob(str(self.bellhop_dir / "*.env")))
        
        if not env_files:
            self.logger.error("No .env files found")
            return
        
        self.logger.info(f"Starting to process {len(env_files)} environment files")
        
        success_count = 0
        for env_file in env_files:
            if self.run_bellhop(env_file):
                success_count += 1
            time.sleep(0.5)
        
        self.logger.info(f"Processing completed: success {success_count}/{len(env_files)}")

def main():
    processor = BellhopProcessor()
    processor.process_all()

if __name__ == "__main__":
    main()