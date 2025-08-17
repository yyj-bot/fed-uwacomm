#!/usr/bin/env python3
"""
BELLHOP Environment File Generator
Generate standard .env files for different ocean environments: ARR, SHD, RAY modes
"""

import os
import numpy as np
import random
from pathlib import Path

class BellhopEnvGenerator:
    def __init__(self, output_dir=None):
        # 使用相对于python-vm的路径
        if output_dir is None:
            # 获取当前脚本的路径
            script_dir = Path(__file__).resolve().parent
            # 计算python-vm的路径
            python_vm_dir = script_dir.parent.parent.parent
            # 设置输出目录为python-vm/data/bellhop
            output_dir = python_vm_dir / "data" / "bellhop"
        
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        print(f"环境文件将生成在: {self.output_dir}")
    
    def generate_sound_speed_profile(self, profile_type="standard", variation_percent=5):
        """Generate randomized sound speed profile data with variations"""
        
        # Base profiles with randomization
        profile_configs = {
            "deep_ocean": {
                "max_depth": np.random.uniform(4500, 5500),
                "num_points": np.random.randint(45, 55),  # 增加深度点数量
                "surface_speed": np.random.uniform(1495, 1505),
                "min_speed": np.random.uniform(1475, 1485),
                "deep_speed": np.random.uniform(1500, 1510)
            },
            "shallow_water": {
                "max_depth": np.random.uniform(400, 600),
                "num_points": np.random.randint(25, 35),  # 增加深度点数量
                "surface_speed": np.random.uniform(1500, 1510),
                "min_speed": np.random.uniform(1485, 1495),
                "deep_speed": np.random.uniform(1495, 1505)
            },
            "tropical": {
                "max_depth": np.random.uniform(2800, 3200),
                "num_points": np.random.randint(35, 45),  # 增加深度点数量
                "surface_speed": np.random.uniform(1510, 1520),
                "min_speed": np.random.uniform(1475, 1485),
                "deep_speed": np.random.uniform(1500, 1510)
            },
            "polar": {
                "max_depth": np.random.uniform(2300, 2700),
                "num_points": np.random.randint(35, 45),  # 增加深度点数量
                "surface_speed": np.random.uniform(1460, 1470),
                "min_speed": np.random.uniform(1450, 1460),
                "deep_speed": np.random.uniform(1470, 1480)
            },
            "continental_shelf": {
                "max_depth": np.random.uniform(900, 1100),
                "num_points": np.random.randint(25, 35),  # 增加深度点数量
                "surface_speed": np.random.uniform(1495, 1505),
                "min_speed": np.random.uniform(1485, 1495),
                "deep_speed": np.random.uniform(1490, 1500)
            },
            "mixed_layer": {
                "max_depth": np.random.uniform(1400, 1600),
                "num_points": np.random.randint(30, 40),  # 增加深度点数量
                "surface_speed": np.random.uniform(1505, 1515),
                "min_speed": np.random.uniform(1485, 1495),
                "deep_speed": np.random.uniform(1500, 1510)
            }
        }
        
        config = profile_configs.get(profile_type, profile_configs["deep_ocean"])
        
        # 使用更密集且均匀的深度采样，确保深度间隔合理
        max_depth = config["max_depth"]
        num_points = int(config["num_points"])
        
        # 确保深度间隔不超过100米，参考官方示例
        min_interval = 50  # 最小间隔50米
        max_interval = 100  # 最大间隔100米
        
        # 调整点数以确保合理的深度间隔
        optimal_points = max(int(max_depth / max_interval), num_points)
        actual_interval = max_depth / (optimal_points - 1)
        
        if actual_interval > max_interval:
            optimal_points = int(max_depth / max_interval) + 1
        
        # 生成均匀分布的深度点
        depths = np.linspace(0, max_depth, optimal_points)
        
        # Generate realistic sound speed profile with smooth thermocline
        speeds = []
        for i, depth in enumerate(depths):
            if depth < 100:  # Surface layer - nearly constant
                speed = config["surface_speed"] - (depth / 100) * 5  # Small decrease
            elif depth < 1000:  # Thermocline - gradual decrease then increase
                progress = (depth - 100) / 900
                speed = config["surface_speed"] - 20 * np.sin(progress * np.pi / 2) + \
                       (config["deep_speed"] - config["surface_speed"]) * progress * 0.3
            else:  # Deep water - gradual increase
                progress = (depth - 1000) / (max_depth - 1000)
                speed = config["min_speed"] + (config["deep_speed"] - config["min_speed"]) * progress
            
            # Add small random variation (reduced)
            variation = np.random.normal(0, 1.5)  # 减小变化幅度
            speed = np.clip(speed + variation, 1460, 1540)  # Tighter, realistic bounds
            speeds.append(speed)
        
        return depths, np.array(speeds)
    
    def get_environment_params(self, env_type, max_depth=None):
        """Get randomized environment parameters"""
        
        # Base frequency ranges for different environments (Hz)
        freq_ranges = {
            "deep_ocean": (800, 1200),
            "shallow_water": (1500, 2500),
            "tropical": (1200, 1800),
            "polar": (600, 1000),
            "continental_shelf": (1000, 1600),
            "mixed_layer": (800, 1400)
        }
        
        # Generate random frequency within range
        freq_range = freq_ranges.get(env_type, (800, 1200))
        freq = np.random.uniform(freq_range[0], freq_range[1])
        
        # Use provided max_depth or default
        if max_depth is None:
            depth_ranges = {
                "deep_ocean": (4500, 5500),
                "shallow_water": (400, 600),
                "tropical": (2800, 3200),
                "polar": (2300, 2700),
                "continental_shelf": (900, 1100),
                "mixed_layer": (1400, 1600)
            }
            depth_range = depth_ranges.get(env_type, (4500, 5500))
            max_depth = np.random.uniform(depth_range[0], depth_range[1])
        
        environments = {
            "deep_ocean": {"title": "Deep Ocean Environment", "freq": freq, "max_depth": max_depth},
            "shallow_water": {"title": "Shallow Water Environment", "freq": freq, "max_depth": max_depth},
            "tropical": {"title": "Tropical Ocean Environment", "freq": freq, "max_depth": max_depth},
            "polar": {"title": "Polar Ocean Environment", "freq": freq, "max_depth": max_depth},
            "continental_shelf": {"title": "Continental Shelf Environment", "freq": freq, "max_depth": max_depth},
            "mixed_layer": {"title": "Mixed Layer Environment", "freq": freq, "max_depth": max_depth}
        }
        return environments.get(env_type, environments["deep_ocean"])
    
    def generate_env_file(self, env_id, env_type, mode, shared_params=None):
        """Generate single environment file with consistent parameters across modes"""
        
        # Use shared parameters if provided, otherwise generate new ones
        if shared_params:
            params = shared_params['params']
            depths = shared_params['depths']
            speeds = shared_params['speeds']
        else:
            params = self.get_environment_params(env_type)
            depths, speeds = self.generate_sound_speed_profile(env_type)
        
        filename = f"{env_id}_{mode}.env"
        filepath = self.output_dir / filename
        
        # Randomized mode configurations with variations
        base_configs = {
            "arr": {
                "run_type": "'A'", 
                "nsd": np.random.randint(1, 4), 
                "nrd": np.random.randint(30, 50), 
                "nbeams": np.random.randint(20, 40),
                "range_extent": np.random.uniform(40.0, 60.0)
            },
            "shd": {
                "run_type": "'C'", 
                "nsd": 1, 
                "nrd": np.random.randint(60, 100), 
                "nbeams": 0,
                "range_extent": np.random.uniform(40.0, 60.0)
            },
            "ray": {
                "run_type": "'R'", 
                "nsd": np.random.randint(1, 4), 
                "nrd": np.random.randint(30, 50), 
                "nbeams": np.random.randint(20, 40),
                "range_extent": np.random.uniform(40.0, 60.0)
            }
        }
        
        config = base_configs[mode]
        
        # Generate source depths based on nsd
        if config["nsd"] == 1:
            source_depths = f"{np.random.uniform(50.0, 200.0):.1f}"
        else:
            depths_list = np.random.uniform(30.0, 300.0, config["nsd"])
            source_depths = " ".join([f"{d:.1f}" for d in sorted(depths_list)])
        
        # Generate angle ranges
        if mode == "shd":
            alpha_range = f"{np.random.uniform(-90.0, -70.0):.1f} {np.random.uniform(70.0, 90.0):.1f}"
        else:
            max_angle = np.random.uniform(10.0, 25.0)
            alpha_range = f"{-max_angle:.1f} {max_angle:.1f}"
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(f"'{params['title']} {mode.upper()} Mode'\t\t! TITLE\n")
            f.write(f"{params['freq']:.1f}\t\t\t! FREQ (Hz)\n")
            f.write("1\t\t\t! NMEDIA\n")
            f.write("'SVF'\t\t\t! SSPOPT\n")
            # 确保DEPTH行的参数与实际深度数据一致
            min_depth = depths[0]  # 第一个深度点（通常是0.0）
            max_depth = depths[-1]  # 最后一个深度点（最大深度）
            f.write(f"{len(depths)}  {min_depth:.1f}  {max_depth:.1f}\t\t! DEPTH\n")
            
            for depth, speed in zip(depths, speeds):
                f.write(f"{depth:8.1f}  {speed:.2f}  /\n")
            
            f.write("'A' 0.0\n")
            f.write(f" {max_depth+200:.1f}  1600.00 0.0 1.8 /\n")
            f.write(f"{config['nsd']}\t\t\t\t! NSD\n")
            f.write(f"{source_depths} /\t\t\t! SD\n")
            f.write(f"{config['nrd']}\t\t\t\t! NRD\n")
            f.write(f"0.0 {max_depth+850:.1f} /\t\t\t! RD\n")
            f.write("201\t\t\t\t! NR\n")
            f.write(f"0.0 {config['range_extent']:.1f} /\t\t\t! R\n")
            f.write(f"{config['run_type']}\t\t\t\t! Run type\n")
            f.write(f"{config['nbeams']}\t\t\t\t! NBeams\n")
            f.write(f"{alpha_range} /\t\t! ALPHA\n")
            f.write(f"0.0 {max_depth+850:.1f} 50.0\t\t! STEP, ZBOX, RBOX\n")
        
        return filepath, {"params": params, "depths": depths, "speeds": speeds}
    
    def generate_all_environments(self, num_environments=100):
        """Generate all environment files in three modes with consistent parameters"""
        env_types = ["deep_ocean", "shallow_water", "tropical", "polar", "continental_shelf", "mixed_layer"]
        
        # Set random seed for reproducible results within this run
        np.random.seed(42)
        random.seed(42)
        
        env_configs = {}
        for i in range(1, num_environments + 1):
            env_id = f"B{i:03d}"  # B001, B002, ...
            env_type = env_types[(i-1) % len(env_types)]  # 循环使用环境类型
            env_configs[env_id] = env_type
        
        files_generated = []
        
        print(f"Generating {num_environments * 3} BELLHOP environment files...")
        print("="*50)
        
        for env_id, env_type in env_configs.items():
            print(f"\nEnvironment {env_id} ({env_type}):")
            
            # Generate shared parameters for this environment
            shared_params = None
            
            for mode in ["arr", "ray", "shd"]:  # Generate in this order for consistency
                if shared_params is None:
                    # First mode generates the shared parameters
                    filepath, shared_params = self.generate_env_file(env_id, env_type, mode)
                else:
                    # Subsequent modes use the same shared parameters
                    filepath, _ = self.generate_env_file(env_id, env_type, mode, shared_params)
                
                files_generated.append(filepath)
                print(f"  + {filepath.name}")
        
        print(f"\nGeneration completed!")
        print(f"Total: {len(files_generated)} environment files")
        print(f"Environments: {num_environments}")
        print(f"Modes per environment: 3 (arr, ray, shd)")
        
        return files_generated

def main():
    generator = BellhopEnvGenerator()
    files = generator.generate_all_environments()
    print("Environment file generation completed!")

if __name__ == "__main__":
    main()