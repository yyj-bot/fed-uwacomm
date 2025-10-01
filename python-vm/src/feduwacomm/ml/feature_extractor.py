#!/usr/bin/env python3
"""
BELLHOP特征提取模块
从BELLHOP仿真结果中提取机器学习特征
"""

import os
import re
import struct
import numpy as np
import pandas as pd
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
import logging
import warnings
from functools import lru_cache

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)
warnings.filterwarnings('ignore', category=RuntimeWarning)

class BellhopFeatureExtractor:
    """从BELLHOP仿真结果中提取特征"""
    
    def __init__(self, data_dir: str = "data/bellhop", max_file_size_mb: int = 100):
        self.data_dir = Path(data_dir)
        self.max_file_size_mb = max_file_size_mb
        self.max_file_size_bytes = max_file_size_mb * 1024 * 1024
        
        # 验证数据目录
        if not self.data_dir.exists():
            logger.warning(f"数据目录不存在: {self.data_dir}")
            self.data_dir.mkdir(parents=True, exist_ok=True)
        
        # 统计信息
        self.stats = {
            'files_processed': 0,
            'files_failed': 0,
            'total_features_extracted': 0
        }
        
    def _validate_file(self, file_path: Path) -> bool:
        """验证文件是否可以安全读取"""
        if not file_path.exists():
            logger.warning(f"文件不存在: {file_path}")
            return False
        
        if file_path.stat().st_size > self.max_file_size_bytes:
            logger.warning(f"文件过大 ({file_path.stat().st_size / 1024 / 1024:.1f}MB): {file_path}")
            return False
        
        return True
    
    @lru_cache(maxsize=128)
    def _compile_regex_patterns(self):
        """编译并缓存正则表达式模式"""
        return {
            'frequency': re.compile(r'frequency\s*=\s*([\d.]+)', re.IGNORECASE),
            'ssp_data': re.compile(r'^\s*([\d.]+)\s+([\d.]+)\s*$'),
            'source_depths': re.compile(r'Source\s+depths,\s+Sz\s*\(m\)'),
            'depth_config': re.compile(r'Depth\s*=\s*([\d.]+)'),
            'range_config': re.compile(r'([\d.]+)\s*km'),
            'beam_angles': re.compile(r'Beam\s+take-off\s+angles'),
            'cpu_time': re.compile(r'CPU\s+Time\s*=\s*([\d.E+-]+)s?', re.IGNORECASE),
            'env_id': re.compile(r'(B\d+_[^.]+)'),
            'numbers': re.compile(r'[\d.]+')
        }
    
    def extract_prt_features(self, prt_file: Path) -> Dict[str, Any]:
        """从.prt（打印）文件中提取特征"""
        features = {
            'filename': prt_file.name,
            'env_id': self._extract_env_id(prt_file.name),
            'mode': self._extract_mode(prt_file.name),
            'success': 0,
            'error_msg': '',
            'freq': None,
            'ssp_points': 0,
            'ssp_min_speed': None,
            'ssp_max_speed': None,
            'ssp_mean_speed': None,
            'ssp_std_speed': None,
            'depth_min': None,
            'depth_max': None,
            'depth_range': None,
            'source_depths': [],
            'receiver_depths': [],
            'ranges': [],
            'beam_angles': [],
            'run_time': None
        }
        
        # 验证文件
        if not self._validate_file(prt_file):
            return features
        
        try:
            # 获取编译的正则表达式
            patterns = self._compile_regex_patterns()
            
            with open(prt_file, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                lines = content.split('\n')
            
            # Check for fatal errors
            if 'FATAL ERROR' in content:
                features['error_msg'] = self._extract_error_message(content)
                return features
            
            features['success'] = 1
            
            # Extract frequency
            freq_match = patterns['frequency'].search(content)
            if freq_match:
                try:
                    features['freq'] = float(freq_match.group(1))
                except ValueError:
                    logger.warning(f"无效的频率值: {freq_match.group(1)}")
            
            # Extract sound speed profile
            ssp_speeds = []
            ssp_depths = []
            
            for line in lines:
                line = line.strip()
                if not line or line.startswith('#') or line.startswith("'"):
                    continue
                
                # Match depth and speed pairs
                ssp_match = patterns['ssp_data'].match(line)
                if ssp_match:
                    try:
                        depth = float(ssp_match.group(1))
                        speed = float(ssp_match.group(2))
                        # 更严格的声速范围检查
                        if 1400 <= speed <= 1600 and 0 <= depth <= 11000:
                            ssp_depths.append(depth)
                            ssp_speeds.append(speed)
                    except (ValueError, IndexError):
                        continue
            
            if ssp_speeds:
                features['ssp_points'] = len(ssp_speeds)
                features['ssp_min_speed'] = min(ssp_speeds)
                features['ssp_max_speed'] = max(ssp_speeds)
                features['ssp_mean_speed'] = np.mean(ssp_speeds)
                features['ssp_std_speed'] = np.std(ssp_speeds)
                
            if ssp_depths:
                features['depth_min'] = min(ssp_depths)
                features['depth_max'] = max(ssp_depths)
                features['depth_range'] = max(ssp_depths) - min(ssp_depths)
            
            # Extract source depths (BELLHOP格式解析)
            source_depths = []
            in_source_section = False
            for i, line in enumerate(lines):
                if patterns['source_depths'].search(line):
                    in_source_section = True
                    continue
                if in_source_section:
                    if line.strip() == '' or '_' in line or line.startswith('Number'):
                        break
                    # 提取数值
                    numbers = patterns['numbers'].findall(line.strip())
                    for num in numbers:
                        try:
                            depth = float(num)
                            if 0 <= depth <= 10000:  # 合理的深度范围
                                source_depths.append(depth)
                        except ValueError:
                            continue
            
            if source_depths:
                features['source_depths'] = source_depths
            
            # Extract receiver depths (从几何配置中提取)
            for line in lines:
                depth_match = patterns['depth_config'].search(line)
                if depth_match:
                    try:
                        max_depth = float(depth_match.group(1))
                        if 0 <= max_depth <= 11000:
                            features['receiver_depths'] = [0.0, max_depth]
                        break
                    except ValueError:
                        continue
            
            # Extract ranges (从几何配置中提取)
            for line in lines:
                if 'Maximum ray range' in line or 'Box%r' in line:
                    range_match = patterns['range_config'].search(line)
                    if range_match:
                        try:
                            max_range = float(range_match.group(1))
                            if 0 <= max_range <= 1000:  # 合理的距离范围
                                features['ranges'] = [0.0, max_range]
                            break
                        except ValueError:
                            continue
            
            # Extract beam angles (从光束角度中提取)
            beam_angles = []
            in_beam_section = False
            for line in lines:
                if patterns['beam_angles'].search(line):
                    in_beam_section = True
                    continue
                if in_beam_section:
                    if line.strip() == '' or 'Number of beams' in line:
                        break
                    # 提取角度值
                    angles = re.findall(r'-?[\d.]+', line)
                    for angle in angles:
                        try:
                            angle_val = float(angle)
                            if -90 <= angle_val <= 90:  # 合理的角度范围
                                beam_angles.append(angle_val)
                        except ValueError:
                            continue
            
            if beam_angles:
                features['beam_angles'] = beam_angles
            
            # Extract run time (更准确的模式)
            time_match = patterns['cpu_time'].search(content)
            if time_match:
                try:
                    run_time = float(time_match.group(1))
                    if 0 <= run_time <= 86400:  # 合理的运行时间范围（0-24小时）
                        features['run_time'] = run_time
                except ValueError:
                    logger.warning(f"无效的运行时间值: {time_match.group(1)}")
                
        except Exception as e:
            features['error_msg'] = str(e)
            logger.error(f"提取PRT特征时出错 {prt_file}: {e}")
            self.stats['files_failed'] += 1
        else:
            self.stats['files_processed'] += 1
            
        return features
    
    def extract_arr_features(self, arr_file: Path) -> Dict[str, Any]:
        """从.arr（到达）文件中提取特征"""
        features = {
            'filename': arr_file.name,
            'env_id': self._extract_env_id(arr_file.name),
            'arrival_count': 0,
            'min_time': None,
            'max_time': None,
            'mean_time': None,
            'std_time': None,
            'min_amplitude': None,
            'max_amplitude': None,
            'mean_amplitude': None,
            'std_amplitude': None,
            'angle_spread': None,
            'phase_variance': None
        }
        
        try:
            times = []
            amplitudes = []
            angles = []
            phases = []
            
            with open(arr_file, 'r') as f:
                lines = f.readlines()
            
            for line in lines:
                line = line.strip()
                if not line or line.startswith("'") or line.isspace():
                    continue
                
                parts = line.split()
                if len(parts) >= 8:
                    try:
                        amplitude = float(parts[0])
                        phase = float(parts[1])
                        time = float(parts[2])
                        angle = float(parts[3])
                        
                        times.append(time)
                        amplitudes.append(abs(amplitude))
                        angles.append(angle)
                        phases.append(phase)
                    except (ValueError, IndexError):
                        continue
            
            if times:
                features['arrival_count'] = len(times)
                features['min_time'] = min(times)
                features['max_time'] = max(times)
                features['mean_time'] = np.mean(times)
                features['std_time'] = np.std(times)
                
            if amplitudes:
                features['min_amplitude'] = min(amplitudes)
                features['max_amplitude'] = max(amplitudes)
                features['mean_amplitude'] = np.mean(amplitudes)
                features['std_amplitude'] = np.std(amplitudes)
                
            if angles:
                features['angle_spread'] = max(angles) - min(angles)
                
            if phases:
                features['phase_variance'] = np.var(phases)
                
        except Exception as e:
            logger.error(f"提取ARR特征时出错 {arr_file}: {e}")
            self.stats['files_failed'] += 1
        else:
            self.stats['files_processed'] += 1
            
        return features
    
    def extract_shd_features(self, shd_file: Path) -> Dict[str, Any]:
        """从.shd（阴影）文件中提取特征"""
        features = {
            'filename': shd_file.name,
            'env_id': self._extract_env_id(shd_file.name),
            'grid_size': None,
            'range_extent': None,
            'depth_extent': None,
            'min_tl': None,
            'max_tl': None,
            'mean_tl': None,
            'std_tl': None,
            'tl_gradient': None,
            'energy_distribution': None
        }
        
        # 验证文件
        if not self._validate_file(shd_file):
            return features
        
        try:
            with open(shd_file, 'rb') as fid:
                # Read record length
                recl = struct.unpack('<i', fid.read(4))[0]
                
                # Skip to dimensions
                fid.seek(2 * 4 * recl, 0)
                fid.read(4)  # Skip record start
                Nfreq = struct.unpack('<i', fid.read(4))[0]
                Ntheta = struct.unpack('<i', fid.read(4))[0]
                Nsx = struct.unpack('<i', fid.read(4))[0]
                Nsy = struct.unpack('<i', fid.read(4))[0]
                Nsz = struct.unpack('<i', fid.read(4))[0]
                Nrz = struct.unpack('<i', fid.read(4))[0]
                Nrr = struct.unpack('<i', fid.read(4))[0]
                
                features['grid_size'] = Nrz * Nrr
                
                # Read ranges and depths for extent calculation with safety checks
                if Nrz > 0 and Nrz < 1000:  # Safety check
                    try:
                        fid.seek(8 * 4 * recl, 0)
                        rd_bytes = fid.read(4 * Nrz)
                        if len(rd_bytes) == 4 * Nrz:
                            rd = np.array(struct.unpack(f'<{Nrz}f', rd_bytes))
                        else:
                            rd = np.array([0, 100])  # Default depth range
                    except (struct.error, OSError, IOError):
                        rd = np.array([0, 100])  # Default depth range
                else:
                    rd = np.array([0, 100])  # Default depth range
                
                if Nrr > 0 and Nrr < 1000:  # Safety check
                    try:
                        fid.seek(9 * 4 * recl, 0)
                        rr_bytes = fid.read(4 * Nrr)
                        if len(rr_bytes) == 4 * Nrr:
                            rr = np.array(struct.unpack(f'<{Nrr}f', rr_bytes))
                        else:
                            rr = np.array([0, 1000])  # Default range
                    except (struct.error, OSError, IOError):
                        rr = np.array([0, 1000])  # Default range
                else:
                    rr = np.array([0, 1000])  # Default range
                
                features['range_extent'] = rr[-1] - rr[0] if len(rr) > 1 else 0
                features['depth_extent'] = rd[-1] - rd[0] if len(rd) > 1 else 0
                
                # 读取传输损失数据（优化采样）
                tl_values = self._extract_tl_data(fid, Nsz, Nrz, Nrr, recl)
                
                if tl_values:
                    features['min_tl'] = min(tl_values)
                    features['max_tl'] = max(tl_values)
                    features['mean_tl'] = np.mean(tl_values)
                    features['std_tl'] = np.std(tl_values)
                    
                    # 计算梯度（简化）
                    if len(tl_values) > 1:
                        features['tl_gradient'] = np.mean(np.abs(np.diff(tl_values)))
                    
                    # 能量分布（类似熵的度量）
                    tl_normalized = np.array(tl_values) - min(tl_values)
                    if max(tl_normalized) > 0:
                        tl_normalized = tl_normalized / max(tl_normalized)
                        features['energy_distribution'] = -np.sum(tl_normalized * np.log(tl_normalized + 1e-10))
                
        except Exception as e:
            logger.error(f"提取SHD特征时出错 {shd_file}: {e}")
            self.stats['files_failed'] += 1
            # 为失败的SHD提取设置默认值
            features.update({
                'grid_size': 0,
                'range_extent': 0,
                'depth_extent': 0,
                'min_tl': None,
                'max_tl': None,
                'mean_tl': None,
                'std_tl': None,
                'tl_gradient': None,
                'energy_distribution': None
            })
        else:
            self.stats['files_processed'] += 1
            
        return features
    
    def _extract_tl_data(self, fid, Nsz: int, Nrz: int, Nrr: int, recl: int) -> List[float]:
        """从SHD文件中提取传输损失数据"""
        tl_values = []
        
        # 严格的维度验证
        if not (0 < Nsz < 100 and 0 < Nrz < 1000 and 0 < Nrr < 1000):
            logger.warning(f"SHD文件维度超出安全范围: Nsz={Nsz}, Nrz={Nrz}, Nrr={Nrr}")
            return tl_values
        
        try:
            # 保守的采样策略
            max_samples = 50
            sample_step_z = max(1, Nrz // 10)  # 深度方向采样步长
            sample_step_r = max(1, Nrr // 10)  # 距离方向采样步长
            
            for i in range(min(Nsz, 2)):  # 最多处理2个声源
                for j in range(0, Nrz, sample_step_z):
                    if len(tl_values) >= max_samples:
                        break
                    
                    recnum = 10 + i * Nrz + j
                    file_pos = recnum * 4 * recl
                    
                    # 文件位置安全检查
                    if file_pos > self.max_file_size_bytes:
                        continue
                    
                    try:
                        fid.seek(file_pos, 0)
                        
                        # 读取较小的数据块
                        chunk_size = min(Nrr, 20, sample_step_r * 5)
                        data_bytes = fid.read(8 * chunk_size)
                        
                        if len(data_bytes) != 8 * chunk_size:
                            continue
                        
                        # 解析复数数据
                        temp = struct.unpack(f'<{2*chunk_size}f', data_bytes)
                        
                        for k in range(0, len(temp), 2 * sample_step_r):
                            if k + 1 < len(temp) and len(tl_values) < max_samples:
                                real_part = temp[k]
                                imag_part = temp[k + 1]
                                
                                # 数据有效性检查
                                if (abs(real_part) < 1e6 and abs(imag_part) < 1e6 and
                                    not np.isnan(real_part) and not np.isnan(imag_part)):
                                    
                                    magnitude = abs(complex(real_part, imag_part))
                                    if magnitude > 1e-15:
                                        tl = -20 * np.log10(magnitude)
                                        if 0 <= tl <= 200 and not np.isnan(tl):
                                            tl_values.append(tl)
                    
                    except (struct.error, OSError, IOError) as e:
                        logger.debug(f"读取SHD数据块失败: {e}")
                        continue
                
                if len(tl_values) >= max_samples:
                    break
        
        except Exception as e:
            logger.warning(f"SHD数据提取失败: {e}")
        
        return tl_values
    
    def extract_ray_features(self, ray_file: Path) -> Dict[str, Any]:
        """从.ray文件中提取特征"""
        features = {
            'filename': ray_file.name,
            'env_id': self._extract_env_id(ray_file.name),
            'ray_count': 0,
            'total_points': 0,
            'max_range': None,
            'max_depth': None,
            'avg_ray_length': None,
            'path_complexity': None,
            'turning_points': 0,
            'bounce_count': 0
        }
        
        try:
            rays = []
            current_ray = []
            
            with open(ray_file, 'r') as f:
                lines = f.readlines()
            
            for line in lines:
                line = line.strip()
                if not line or line.startswith('Number') or line.startswith('Freq'):
                    continue
                
                parts = line.split()
                if len(parts) >= 2:
                    try:
                        r = float(parts[0])  # 距离
                        z = float(parts[1])  # 深度
                        current_ray.append((r, z))
                    except ValueError:
                        if current_ray:
                            rays.append(current_ray)
                            current_ray = []
            
            if current_ray:
                rays.append(current_ray)
            
            if rays:
                features['ray_count'] = len(rays)
                features['total_points'] = sum(len(ray) for ray in rays)
                
                all_ranges = [r for ray in rays for r, z in ray]
                all_depths = [z for ray in rays for r, z in ray]
                
                if all_ranges:
                    features['max_range'] = max(all_ranges)
                if all_depths:
                    features['max_depth'] = max(all_depths)
                
                # 计算平均射线长度
                ray_lengths = []
                for ray in rays:
                    if len(ray) > 1:
                        length = 0
                        for i in range(1, len(ray)):
                            dr = ray[i][0] - ray[i-1][0]
                            dz = ray[i][1] - ray[i-1][1]
                            length += np.sqrt(dr**2 + dz**2)
                        ray_lengths.append(length)
                
                if ray_lengths:
                    features['avg_ray_length'] = np.mean(ray_lengths)
                
                # 计算路径复杂度（总曲率）
                total_curvature = 0
                total_turns = 0
                
                for ray in rays:
                    if len(ray) > 2:
                        for i in range(1, len(ray) - 1):
                            # 计算角度变化
                            v1 = np.array(ray[i]) - np.array(ray[i-1])
                            v2 = np.array(ray[i+1]) - np.array(ray[i])
                            
                            if np.linalg.norm(v1) > 0 and np.linalg.norm(v2) > 0:
                                cos_angle = np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2))
                                cos_angle = np.clip(cos_angle, -1, 1)
                                angle_change = np.arccos(cos_angle)
                                total_curvature += angle_change
                                
                                if angle_change > 0.1:  # 显著转弯
                                    total_turns += 1
                
                features['path_complexity'] = total_curvature
                features['turning_points'] = total_turns
                
        except Exception as e:
            print(f"Error extracting RAY features from {ray_file}: {e}")
            
        return features
    
    def extract_env_features(self, env_file: Path) -> Dict[str, Any]:
        """从.env文件中提取特征"""
        features = {
            'filename': env_file.name,
            'env_id': self._extract_env_id(env_file.name),
            'mode': self._extract_mode(env_file.name),
            'frequency': None,
            'source_count': 0,
            'receiver_count': 0,
            'range_count': 0,
            'max_depth': None,
            'ssp_type': None
        }
        
        try:
            with open(env_file, 'r') as f:
                lines = f.readlines()
            
            # Extract frequency (usually line 2)
            if len(lines) > 1:
                try:
                    features['frequency'] = float(lines[1].strip())
                except ValueError:
                    pass
            
            # Parse environment file structure
            for i, line in enumerate(lines):
                line = line.strip()
                
                # Look for depth information
                if 'depth' in line.lower() or (i > 3 and len(line.split()) >= 3):
                    parts = line.split()
                    if len(parts) >= 3:
                        try:
                            depth = float(parts[2])
                            if features['max_depth'] is None or depth > features['max_depth']:
                                features['max_depth'] = depth
                        except (ValueError, IndexError):
                            pass
                
                # Look for SSP type
                if "'" in line and ('SVF' in line or 'C' in line or 'A' in line):
                    features['ssp_type'] = line.strip("'").strip()
                
                # Count sources, receivers, ranges (simplified)
                if line.isdigit():
                    num = int(line)
                    if 1 <= num <= 10:
                        features['source_count'] = max(features['source_count'], num)
                    elif 10 < num <= 1000:
                        features['receiver_count'] = max(features['receiver_count'], num)
                        
        except Exception as e:
            print(f"Error extracting ENV features from {env_file}: {e}")
            
        return features
    
    def extract_all_features(self, env_id: str) -> Dict[str, Any]:
        """从给定环境的所有文件中提取特征"""
        combined_features = {
            'env_id': env_id,
            'timestamp': pd.Timestamp.now()
        }
        
        # 新的正确格式：B001_arr, B001_ray, B001_shd
        # 提取ENV文件
        env_file = self.data_dir / f"{env_id}.env"
        if env_file.exists():
            try:
                features = self.extract_env_features(env_file)
                if features:
                    for key, value in features.items():
                        if key not in ['env_id', 'filename']:
                            combined_key = f"env_{key}"
                            combined_features[combined_key] = value
            except Exception as e:
                print(f"Warning: Failed to extract from {env_file}: {e}")
        
        # 提取PRT文件（所有模式都应该有prt文件）
        prt_file = self.data_dir / f"{env_id}.prt"
        if prt_file.exists():
            try:
                features = self.extract_prt_features(prt_file)
                if features:
                    for key, value in features.items():
                        if key not in ['env_id', 'filename']:
                            combined_key = f"prt_{key}"
                            combined_features[combined_key] = value
            except Exception as e:
                print(f"Warning: Failed to extract from {prt_file}: {e}")
        
        # 提取模式特定的输出文件
        if '_arr' in env_id:
            # 提取ARR文件
            arr_file = self.data_dir / f"{env_id}.arr"
            if arr_file.exists():
                try:
                    features = self.extract_arr_features(arr_file)
                    if features:
                        for key, value in features.items():
                            if key not in ['env_id', 'filename']:
                                combined_key = f"arr_{key}"
                                combined_features[combined_key] = value
                except Exception as e:
                    print(f"Warning: Failed to extract from {arr_file}: {e}")
        
        elif '_ray' in env_id:
            # 提取RAY文件
            ray_file = self.data_dir / f"{env_id}.ray"
            if ray_file.exists():
                try:
                    features = self.extract_ray_features(ray_file)
                    if features:
                        for key, value in features.items():
                            if key not in ['env_id', 'filename']:
                                combined_key = f"ray_{key}"
                                combined_features[combined_key] = value
                except Exception as e:
                    print(f"Warning: Failed to extract from {ray_file}: {e}")
        
        elif '_shd' in env_id:
            # 提取SHD文件
            shd_file = self.data_dir / f"{env_id}.shd"
            if shd_file.exists():
                try:
                    features = self.extract_shd_features(shd_file)
                    if features:
                        for key, value in features.items():
                            if key not in ['env_id', 'filename']:
                                combined_key = f"shd_{key}"
                                combined_features[combined_key] = value
                except Exception as e:
                    print(f"Warning: Failed to extract from {shd_file}: {e}")
        
        return combined_features
    
    def get_extraction_stats(self) -> Dict[str, Any]:
        """获取特征提取统计信息"""
        return {
            'files_processed': self.stats['files_processed'],
            'files_failed': self.stats['files_failed'],
            'success_rate': self.stats['files_processed'] / (self.stats['files_processed'] + self.stats['files_failed']) if (self.stats['files_processed'] + self.stats['files_failed']) > 0 else 0,
            'total_features_extracted': self.stats['total_features_extracted']
        }
    
    def reset_stats(self):
        """重置统计信息"""
        self.stats = {
            'files_processed': 0,
            'files_failed': 0,
            'total_features_extracted': 0
        }
    
    def batch_extract_features(self) -> pd.DataFrame:
        """从所有环境中提取特征"""
        all_features = []
        
        # 获取所有环境ID
        env_ids = set()
        for file_path in self.data_dir.glob("*.env"):
            env_id = self._extract_env_id(file_path.name)
            if env_id:
                env_ids.add(env_id)
        
        print(f"Found {len(env_ids)} environments: {sorted(env_ids)}")
        
        for env_id in sorted(env_ids):
            print(f"Extracting features for {env_id}...")
            features = self.extract_all_features(env_id)
            all_features.append(features)
        
        df = pd.DataFrame(all_features)
        print(f"Extracted features for {len(df)} environments")
        print(f"Feature columns: {len(df.columns)}")
        
        return df
    
    def _extract_env_id(self, filename: str) -> Optional[str]:
        """从文件名中提取环境ID"""
        # 处理正确模式：B001_arr.env -> B001_arr
        if filename.endswith('.env'):
            return filename[:-4]  # 移除.env扩展名
        
        # 其他文件的备用方案：B001_arr.prt -> B001_arr
        match = re.match(r'(B\d+_[^.]+)', filename)
        return match.group(1) if match else None
    
    def _extract_mode(self, filename: str) -> Optional[str]:
        """从文件名中提取模式"""
        if '_arr.' in filename:
            return 'arr'
        elif '_ray.' in filename:
            return 'ray'
        elif '_shd.' in filename:
            return 'shd'
        return None
    
    def _extract_error_message(self, content: str) -> str:
        """从PRT文件内容中提取错误消息"""
        lines = content.split('\n')
        for line in lines:
            if 'FATAL ERROR' in line:
                return line.strip()
        return "Unknown error"

if __name__ == "__main__":
    extractor = BellhopFeatureExtractor()
    df = extractor.batch_extract_features()
    
    # 保存为CSV以供检查
    output_file = "extracted_features.csv"
    df.to_csv(output_file, index=False)
    print(f"Features saved to {output_file}")
    
    # 显示摘要
    print(f"\nFeature Summary:")
    print(f"Environments: {df['env_id'].nunique()}")
    print(f"Total features: {len(df.columns)}")
    print(f"Numeric features: {df.select_dtypes(include=[np.number]).shape[1]}")