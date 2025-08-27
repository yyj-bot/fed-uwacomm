#!/usr/bin/env python3
"""
BELLHOP Feature Extraction Module
Extract features from BELLHOP simulation results for machine learning
"""

import os
import re
import struct
import numpy as np
import pandas as pd
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any

class BellhopFeatureExtractor:
    """Extract features from BELLHOP simulation results"""
    
    def __init__(self, data_dir: str = "data/bellhop"):
        self.data_dir = Path(data_dir)
        
    def extract_prt_features(self, prt_file: Path) -> Dict[str, Any]:
        """Extract features from .prt (print) files"""
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
        
        try:
            with open(prt_file, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                lines = content.split('\n')
            
            # Check for fatal errors
            if 'FATAL ERROR' in content:
                features['error_msg'] = self._extract_error_message(content)
                return features
            
            features['success'] = 1
            
            # Extract frequency
            freq_match = re.search(r'frequency\s*=\s*([\d.]+)', content, re.IGNORECASE)
            if freq_match:
                features['freq'] = float(freq_match.group(1))
            
            # Extract sound speed profile
            ssp_speeds = []
            ssp_depths = []
            
            for line in lines:
                # Match depth and speed pairs
                ssp_match = re.match(r'\s*([\d.]+)\s+([\d.]+)', line.strip())
                if ssp_match and len(line.strip().split()) >= 2:
                    try:
                        depth = float(ssp_match.group(1))
                        speed = float(ssp_match.group(2))
                        if 1400 <= speed <= 1600:  # Reasonable sound speed range
                            ssp_depths.append(depth)
                            ssp_speeds.append(speed)
                    except ValueError:
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
                if 'Source   depths, Sz (m)' in line:
                    in_source_section = True
                    continue
                if in_source_section:
                    if line.strip() == '' or '_' in line:
                        break
                    # 提取数值
                    numbers = re.findall(r'[\d.]+', line.strip())
                    for num in numbers:
                        try:
                            depth = float(num)
                            if 0 <= depth <= 10000:  # 合理的深度范围
                                source_depths.append(depth)
                        except ValueError:
                            pass
            
            if source_depths:
                features['source_depths'] = source_depths
            
            # Extract receiver depths (从几何配置中提取)
            depth_section = False
            for line in lines:
                if 'Depth =' in line:
                    depth_match = re.search(r'Depth\s*=\s*([\d.]+)', line)
                    if depth_match:
                        max_depth = float(depth_match.group(1))
                        features['receiver_depths'] = [0.0, max_depth]
                        break
            
            # Extract ranges (从几何配置中提取)
            for line in lines:
                if 'Maximum ray range' in line or 'Box%r' in line:
                    range_match = re.search(r'([\d.]+)\s*km', line)
                    if range_match:
                        max_range = float(range_match.group(1))
                        features['ranges'] = [0.0, max_range]
                        break
            
            # Extract beam angles (从光束角度中提取)
            beam_angles = []
            in_beam_section = False
            for line in lines:
                if 'Beam take-off angles' in line:
                    in_beam_section = True
                    continue
                if in_beam_section:
                    if line.strip() == '' or 'Number of beams' in line:
                        break
                    # 提取角度值
                    angles = re.findall(r'-?[\d.]+', line)
                    for angle in angles:
                        try:
                            beam_angles.append(float(angle))
                        except ValueError:
                            pass
            
            if beam_angles:
                features['beam_angles'] = beam_angles
            
            # Extract run time (更准确的模式)
            time_match = re.search(r'CPU Time\s*=\s*([\d.E+-]+)s?', content, re.IGNORECASE)
            if time_match:
                try:
                    features['run_time'] = float(time_match.group(1))
                except ValueError:
                    pass
                
        except Exception as e:
            features['error_msg'] = str(e)
            
        return features
    
    def extract_arr_features(self, arr_file: Path) -> Dict[str, Any]:
        """Extract features from .arr (arrival) files"""
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
            print(f"Error extracting ARR features from {arr_file}: {e}")
            
        return features
    
    def extract_shd_features(self, shd_file: Path) -> Dict[str, Any]:
        """Extract features from .shd (shade) files"""
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
                
                # Read transmission loss data (simplified sampling with bounds checking)
                # Add more strict validation for file dimensions
                if (Nsz > 0 and Nrz > 0 and Nrr > 0 and 
                    Nsz < 100 and Nrz < 1000 and Nrr < 1000):  # Much stricter bounds
                    tl_values = []
                    
                    # Very conservative sampling
                    max_samples = min(50, Nrz * Nrr)
                    
                    try:
                        for i in range(0, min(Nsz, 1)):  # Only first source
                            for j in range(0, min(Nrz, 5), max(1, max(Nrz//5, 1))):  # Even fewer samples
                                recnum = 10 + i * Nrz + j
                                file_pos = recnum * 4 * recl
                                
                                # Check if position is within reasonable bounds
                                if file_pos > 10 * 1024 * 1024:  # Skip if > 10MB (much stricter)
                                    continue
                                
                                # Ensure we don't read beyond file size
                                try:
                                    fid.seek(file_pos, 0)
                                except (OSError, IOError):
                                    continue
                                
                                # Read very small chunks with extra safety
                                chunk_size = min(Nrr, 10)  # Much smaller chunks
                                if chunk_size <= 0 or chunk_size > 100:  # Additional safety
                                    continue
                                
                                try:
                                    data_bytes = fid.read(8 * chunk_size)
                                    if len(data_bytes) != 8 * chunk_size:
                                        continue  # Skip if couldn't read full chunk
                                    
                                    temp = struct.unpack(f'<{2*chunk_size}f', data_bytes)
                                    
                                    for k in range(0, len(temp), 2):  # Process in pairs (real, imag)
                                        if k + 1 < len(temp):
                                            real_part = temp[k]
                                            imag_part = temp[k + 1]
                                            if (abs(real_part) < 1e6 and abs(imag_part) < 1e6 and  # Stricter bounds
                                                not np.isnan(real_part) and not np.isnan(imag_part)):
                                                magnitude = abs(complex(real_part, imag_part))
                                                if magnitude > 1e-15:  # Avoid log(0)
                                                    tl = -20 * np.log10(magnitude)
                                                    if not np.isnan(tl) and not np.isinf(tl) and 0 <= tl <= 200:  # Reasonable TL range
                                                        tl_values.append(tl)
                                                        if len(tl_values) >= 50:  # Limit total samples
                                                            break
                                except (struct.error, OSError, IOError):
                                    continue
                                
                                if len(tl_values) >= 50:  # Stop if we have enough samples
                                    break
                            
                            if len(tl_values) >= 50:
                                break
                                
                    except Exception as read_error:
                        # If detailed reading fails, skip this file gracefully
                        print(f"SHD reading error: {read_error}")
                        pass
                    
                    if tl_values:
                        features['min_tl'] = min(tl_values)
                        features['max_tl'] = max(tl_values)
                        features['mean_tl'] = np.mean(tl_values)
                        features['std_tl'] = np.std(tl_values)
                        
                        # Calculate gradient (simplified)
                        if len(tl_values) > 1:
                            features['tl_gradient'] = np.mean(np.abs(np.diff(tl_values)))
                        
                        # Energy distribution (entropy-like measure)
                        tl_normalized = np.array(tl_values) - min(tl_values)
                        if max(tl_normalized) > 0:
                            tl_normalized = tl_normalized / max(tl_normalized)
                            features['energy_distribution'] = -np.sum(tl_normalized * np.log(tl_normalized + 1e-10))
                
        except Exception as e:
            print(f"Error extracting SHD features from {shd_file}: {e}")
            # Set default values for failed SHD extraction
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
            
        return features
    
    def extract_ray_features(self, ray_file: Path) -> Dict[str, Any]:
        """Extract features from .ray files"""
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
                        r = float(parts[0])  # Range
                        z = float(parts[1])  # Depth
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
                
                # Calculate average ray length
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
                
                # Calculate path complexity (total curvature)
                total_curvature = 0
                total_turns = 0
                
                for ray in rays:
                    if len(ray) > 2:
                        for i in range(1, len(ray) - 1):
                            # Calculate angle change
                            v1 = np.array(ray[i]) - np.array(ray[i-1])
                            v2 = np.array(ray[i+1]) - np.array(ray[i])
                            
                            if np.linalg.norm(v1) > 0 and np.linalg.norm(v2) > 0:
                                cos_angle = np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2))
                                cos_angle = np.clip(cos_angle, -1, 1)
                                angle_change = np.arccos(cos_angle)
                                total_curvature += angle_change
                                
                                if angle_change > 0.1:  # Significant turn
                                    total_turns += 1
                
                features['path_complexity'] = total_curvature
                features['turning_points'] = total_turns
                
        except Exception as e:
            print(f"Error extracting RAY features from {ray_file}: {e}")
            
        return features
    
    def extract_env_features(self, env_file: Path) -> Dict[str, Any]:
        """Extract features from .env files"""
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
        """Extract features from all files for a given environment"""
        combined_features = {
            'env_id': env_id,
            'timestamp': pd.Timestamp.now()
        }
        
        # New correct format: B001_arr, B001_ray, B001_shd
        # Extract ENV file
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
        
        # Extract PRT file (all modes should have prt files)
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
        
        # Extract mode-specific output files
        if '_arr' in env_id:
            # Extract ARR file
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
            # Extract RAY file
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
            # Extract SHD file
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
    
    def batch_extract_features(self) -> pd.DataFrame:
        """Extract features from all environments"""
        all_features = []
        
        # Get all environment IDs
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
        """Extract environment ID from filename"""
        # Handle correct pattern: B001_arr.env -> B001_arr
        if filename.endswith('.env'):
            return filename[:-4]  # Remove .env extension
        
        # Fallback for other files: B001_arr.prt -> B001_arr
        match = re.match(r'(B\d+_[^.]+)', filename)
        return match.group(1) if match else None
    
    def _extract_mode(self, filename: str) -> Optional[str]:
        """Extract mode from filename"""
        if '_arr.' in filename:
            return 'arr'
        elif '_ray.' in filename:
            return 'ray'
        elif '_shd.' in filename:
            return 'shd'
        return None
    
    def _extract_error_message(self, content: str) -> str:
        """Extract error message from PRT file content"""
        lines = content.split('\n')
        for line in lines:
            if 'FATAL ERROR' in line:
                return line.strip()
        return "Unknown error"

if __name__ == "__main__":
    extractor = BellhopFeatureExtractor()
    df = extractor.batch_extract_features()
    
    # Save to CSV for inspection
    output_file = "extracted_features.csv"
    df.to_csv(output_file, index=False)
    print(f"Features saved to {output_file}")
    
    # Display summary
    print(f"\nFeature Summary:")
    print(f"Environments: {df['env_id'].nunique()}")
    print(f"Total features: {len(df.columns)}")
    print(f"Numeric features: {df.select_dtypes(include=[np.number]).shape[1]}")