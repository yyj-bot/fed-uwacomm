#!/usr/bin/env python3
"""
Database Connection and Operations Module
Handle MySQL database operations for BELLHOP feature data
"""

import os
import pymysql
import pandas as pd
import numpy as np
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime
import logging
from pathlib import Path
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

class DatabaseManager:
    """Manage database connections and operations"""
    
    def __init__(self):
        self.host = os.getenv('DB_HOST', 'localhost')
        self.port = int(os.getenv('DB_PORT', 3306))
        self.user = os.getenv('DB_USER', 'root')
        self.password = os.getenv('DB_PASSWORD', '')
        self.database = os.getenv('DB_NAME', 'bellhop_data')
        self.table = os.getenv('DB_TABLE', 'features')
        self.connection = None
        
        # Set up logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        
    def connect(self) -> bool:
        """Establish database connection"""
        try:
            self.connection = pymysql.connect(
                host=self.host,
                port=self.port,
                user=self.user,
                password=self.password,
                database=self.database,
                charset='utf8mb4',
                cursorclass=pymysql.cursors.DictCursor,
                autocommit=True
            )
            self.logger.info(f"Connected to database {self.database}")
            return True
        except Exception as e:
            self.logger.error(f"Database connection failed: {e}")
            return False
    
    def disconnect(self):
        """Close database connection"""
        if self.connection:
            self.connection.close()
            self.connection = None
            self.logger.info("Database connection closed")
    
    def create_features_table(self):
        """Create features table if it doesn't exist"""
        if not self.connection:
            self.logger.error("No database connection")
            return False
            
        try:
            with self.connection.cursor() as cursor:
                # Create comprehensive features table
                create_table_sql = f"""
                CREATE TABLE IF NOT EXISTS {self.table} (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    env_id VARCHAR(20) NOT NULL,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    
                    -- Environment file features
                    env_frequency FLOAT,
                    env_source_count INT,
                    env_receiver_count INT,
                    env_range_count INT,
                    env_max_depth FLOAT,
                    env_ssp_type VARCHAR(50),
                    
                    -- PRT file features (arrival mode)
                    prt_arr_success TINYINT DEFAULT 0,
                    prt_arr_freq FLOAT,
                    prt_arr_ssp_points INT,
                    prt_arr_ssp_min_speed FLOAT,
                    prt_arr_ssp_max_speed FLOAT,
                    prt_arr_ssp_mean_speed FLOAT,
                    prt_arr_ssp_std_speed FLOAT,
                    prt_arr_depth_min FLOAT,
                    prt_arr_depth_max FLOAT,
                    prt_arr_depth_range FLOAT,
                    prt_arr_source_depths TEXT,
                    prt_arr_receiver_depths TEXT,
                    prt_arr_ranges TEXT,
                    prt_arr_run_time FLOAT,
                    
                    -- PRT file features (ray mode)
                    prt_ray_success TINYINT DEFAULT 0,
                    prt_ray_freq FLOAT,
                    prt_ray_ssp_points INT,
                    prt_ray_ssp_min_speed FLOAT,
                    prt_ray_ssp_max_speed FLOAT,
                    prt_ray_ssp_mean_speed FLOAT,
                    prt_ray_ssp_std_speed FLOAT,
                    prt_ray_depth_min FLOAT,
                    prt_ray_depth_max FLOAT,
                    prt_ray_depth_range FLOAT,
                    prt_ray_source_depths TEXT,
                    prt_ray_receiver_depths TEXT,
                    prt_ray_ranges TEXT,
                    prt_ray_beam_angles TEXT,
                    prt_ray_run_time FLOAT,
                    
                    -- PRT file features (shd mode)
                    prt_shd_success TINYINT DEFAULT 0,
                    prt_shd_freq FLOAT,
                    prt_shd_ssp_points INT,
                    prt_shd_ssp_min_speed FLOAT,
                    prt_shd_ssp_max_speed FLOAT,
                    prt_shd_ssp_mean_speed FLOAT,
                    prt_shd_ssp_std_speed FLOAT,
                    prt_shd_depth_min FLOAT,
                    prt_shd_depth_max FLOAT,
                    prt_shd_depth_range FLOAT,
                    prt_shd_source_depths TEXT,
                    prt_shd_receiver_depths TEXT,
                    prt_shd_ranges TEXT,
                    prt_shd_run_time FLOAT,
                    
                    -- ARR file features
                    arr_arrival_count INT,
                    arr_min_time FLOAT,
                    arr_max_time FLOAT,
                    arr_mean_time FLOAT,
                    arr_std_time FLOAT,
                    arr_min_amplitude FLOAT,
                    arr_max_amplitude FLOAT,
                    arr_mean_amplitude FLOAT,
                    arr_std_amplitude FLOAT,
                    arr_angle_spread FLOAT,
                    arr_phase_variance FLOAT,
                    
                    -- RAY file features
                    ray_ray_count INT,
                    ray_total_points INT,
                    ray_max_range FLOAT,
                    ray_max_depth FLOAT,
                    ray_avg_ray_length FLOAT,
                    ray_path_complexity FLOAT,
                    ray_turning_points INT,
                    ray_bounce_count INT,
                    
                    -- SHD file features
                    shd_grid_size INT,
                    shd_range_extent FLOAT,
                    shd_depth_extent FLOAT,
                    shd_min_tl FLOAT,
                    shd_max_tl FLOAT,
                    shd_mean_tl FLOAT,
                    shd_std_tl FLOAT,
                    shd_tl_gradient FLOAT,
                    shd_energy_distribution FLOAT,
                    
                    -- Derived features for ML
                    ml_channel_complexity FLOAT,
                    ml_propagation_efficiency FLOAT,
                    ml_environment_variability FLOAT,
                    ml_signal_quality FLOAT,
                    
                    INDEX idx_env_id (env_id),
                    INDEX idx_timestamp (timestamp)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
                
                cursor.execute(create_table_sql)
                self.logger.info(f"Features table '{self.table}' created/verified")
                return True
                
        except Exception as e:
            self.logger.error(f"Error creating features table: {e}")
            return False
    
    def insert_features(self, features_df: pd.DataFrame) -> bool:
        """Insert features dataframe into database"""
        if not self.connection:
            self.logger.error("No database connection")
            return False
            
        try:
            # Prepare data for insertion
            processed_data = []
            
            for _, row in features_df.iterrows():
                # Convert complex data types to JSON strings
                processed_row = {}
                for key, value in row.items():
                    if isinstance(value, (list, np.ndarray)):
                        processed_row[key] = str(value)
                    elif pd.isna(value):
                        processed_row[key] = None
                    elif isinstance(value, (np.integer, np.floating)):
                        processed_row[key] = float(value) if not np.isnan(value) else None
                    else:
                        processed_row[key] = value
                
                processed_data.append(processed_row)
            
            # Create insert query dynamically
            if processed_data:
                columns = list(processed_data[0].keys())
                placeholders = ', '.join(['%s'] * len(columns))
                columns_str = ', '.join(columns)
                
                insert_sql = f"INSERT INTO {self.table} ({columns_str}) VALUES ({placeholders})"
                
                with self.connection.cursor() as cursor:
                    # Insert data
                    for row_data in processed_data:
                        values = [row_data.get(col) for col in columns]
                        cursor.execute(insert_sql, values)
                
                self.logger.info(f"Inserted {len(processed_data)} feature records")
                return True
                
        except Exception as e:
            self.logger.error(f"Error inserting features: {e}")
            return False
    
    def get_features_for_training(self, env_ids: Optional[List[str]] = None) -> pd.DataFrame:
        """Get features data for machine learning training"""
        if not self.connection:
            self.logger.error("No database connection")
            return pd.DataFrame()
            
        try:
            with self.connection.cursor() as cursor:
                if env_ids:
                    placeholders = ', '.join(['%s'] * len(env_ids))
                    query = f"SELECT * FROM {self.table} WHERE env_id IN ({placeholders}) ORDER BY env_id"
                    cursor.execute(query, env_ids)
                else:
                    query = f"SELECT * FROM {self.table} ORDER BY env_id"
                    cursor.execute(query)
                
                results = cursor.fetchall()
                
                if results:
                    df = pd.DataFrame(results)
                    self.logger.info(f"Retrieved {len(df)} feature records")
                    return df
                else:
                    self.logger.warning("No feature data found")
                    return pd.DataFrame()
                    
        except Exception as e:
            self.logger.error(f"Error retrieving features: {e}")
            return pd.DataFrame()
    
    def get_numeric_features(self) -> pd.DataFrame:
        """Get only numeric features for ML training"""
        df = self.get_features_for_training()
        
        if df.empty:
            return df
            
        # Select only numeric columns (excluding ID, env_id, timestamp, and text fields)
        numeric_columns = []
        exclude_columns = ['id', 'env_id', 'timestamp']
        
        for col in df.columns:
            if col not in exclude_columns and not col.endswith('_depths') and not col.endswith('_ranges') and not col.endswith('_angles'):
                if pd.api.types.is_numeric_dtype(df[col]):
                    numeric_columns.append(col)
        
        numeric_df = df[['env_id'] + numeric_columns].copy()
        
        # Handle missing values
        numeric_df = numeric_df.fillna(0)
        
        self.logger.info(f"Selected {len(numeric_columns)} numeric features")
        return numeric_df
    
    def calculate_derived_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Calculate derived features for machine learning"""
        if df.empty:
            return df
            
        try:
            # Channel complexity: combination of ray complexity and TL variance
            df['ml_channel_complexity'] = (
                df.get('ray_path_complexity', 0) * 0.5 + 
                df.get('shd_std_tl', 0) * 0.3 + 
                df.get('ray_turning_points', 0) * 0.2
            )
            
            # Propagation efficiency: inverse of transmission loss
            df['ml_propagation_efficiency'] = np.where(
                df.get('shd_mean_tl', 0) > 0,
                1.0 / (df.get('shd_mean_tl', 1) + 1),
                0
            )
            
            # Environment variability: SSP variation
            df['ml_environment_variability'] = (
                df.get('prt_arr_ssp_std_speed', 0) + 
                df.get('prt_ray_ssp_std_speed', 0) + 
                df.get('prt_shd_ssp_std_speed', 0)
            ) / 3.0
            
            # Signal quality: amplitude statistics
            df['ml_signal_quality'] = np.where(
                df.get('arr_std_amplitude', 0) > 0,
                df.get('arr_mean_amplitude', 0) / (df.get('arr_std_amplitude', 1) + 1e-6),
                0
            )
            
            self.logger.info("Calculated derived ML features")
            
        except Exception as e:
            self.logger.error(f"Error calculating derived features: {e}")
            
        return df
    
    def update_derived_features(self):
        """Update derived features in database"""
        if not self.connection:
            self.logger.error("No database connection")
            return False
            
        try:
            df = self.get_features_for_training()
            if df.empty:
                return False
                
            df = self.calculate_derived_features(df)
            
            with self.connection.cursor() as cursor:
                for _, row in df.iterrows():
                    update_sql = f"""
                    UPDATE {self.table} SET
                        ml_channel_complexity = %s,
                        ml_propagation_efficiency = %s,
                        ml_environment_variability = %s,
                        ml_signal_quality = %s
                    WHERE id = %s
                    """
                    
                    cursor.execute(update_sql, [
                        row.get('ml_channel_complexity'),
                        row.get('ml_propagation_efficiency'),
                        row.get('ml_environment_variability'),
                        row.get('ml_signal_quality'),
                        row.get('id')
                    ])
            
            self.logger.info("Updated derived features in database")
            return True
            
        except Exception as e:
            self.logger.error(f"Error updating derived features: {e}")
            return False
    
    def get_table_info(self) -> Dict[str, Any]:
        """Get information about the features table"""
        if not self.connection:
            return {}
            
        try:
            with self.connection.cursor() as cursor:
                # Get table structure
                cursor.execute(f"DESCRIBE {self.table}")
                columns = cursor.fetchall()
                
                # Get row count
                cursor.execute(f"SELECT COUNT(*) as count FROM {self.table}")
                count_result = cursor.fetchone()
                row_count = count_result['count'] if count_result else 0
                
                # Get sample data
                cursor.execute(f"SELECT * FROM {self.table} LIMIT 5")
                sample_data = cursor.fetchall()
                
                return {
                    'columns': columns,
                    'row_count': row_count,
                    'sample_data': sample_data
                }
                
        except Exception as e:
            self.logger.error(f"Error getting table info: {e}")
            return {}
    
    def __enter__(self):
        """Context manager entry"""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit"""
        self.disconnect()

def test_database_connection():
    """Test database connection and operations"""
    db = DatabaseManager()
    
    if db.connect():
        print("✓ Database connection successful")
        
        # Create table
        if db.create_features_table():
            print("✓ Features table created/verified")
        
        # Get table info
        info = db.get_table_info()
        if info:
            print(f"✓ Table has {info['row_count']} rows and {len(info['columns'])} columns")
        
        db.disconnect()
    else:
        print("✗ Database connection failed")

if __name__ == "__main__":
    test_database_connection()