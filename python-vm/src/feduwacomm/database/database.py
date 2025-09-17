#!/usr/bin/env python3
"""
Database Connection and Operations Module
Handle SQLite database operations for BELLHOP feature data with automatic CSV structure matching
"""

import os
import sqlite3
import pandas as pd
import numpy as np
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime
import logging
from pathlib import Path
from dotenv import load_dotenv

# Load environment variables - 智能查找.env文件
def find_env_file():
    """智能查找.env文件"""
    env_paths = [
        Path('.env'),                    # 当前目录
        Path('python-vm/.env'),          # 从根目录运行时
        Path('../.env'),                 # 从子目录运行时
        Path('../../.env'),              # 从更深的子目录运行时
    ]
    
    for path in env_paths:
        if path.exists():
            return str(path)
    return '.env'  # 默认

env_file = find_env_file()
load_dotenv(env_file)

class DatabaseManager:
    """Enhanced database manager with automatic table structure matching using SQLite"""
    
    def __init__(self):
        # SQLite database file path
        self.db_path = os.getenv('DB_PATH', 'bellhop_data.db')
        self.table = os.getenv('DB_TABLE', 'features')
        self.connection = None
        
        # Ensure database directory exists
        db_dir = Path(self.db_path).parent
        if db_dir != Path('.'):
            db_dir.mkdir(parents=True, exist_ok=True)
        
        # Set up logging
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)
        
    def connect(self) -> bool:
        """Establish database connection"""
        try:
            self.connection = sqlite3.connect(self.db_path)
            # Enable row factory for dict-like access
            self.connection.row_factory = sqlite3.Row
            self.logger.info(f"Connected to SQLite database {self.db_path}")
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
    
    def analyze_csv_structure(self, csv_file_path: str) -> Dict[str, str]:
        """Analyze CSV file to determine column types for SQLite"""
        df = pd.read_csv(csv_file_path)
        
        column_types = {}
        
        for col in df.columns:
            # Get sample values (non-null)
            sample_values = df[col].dropna()
            
            if len(sample_values) == 0:
                column_types[col] = "TEXT"
                continue
            
            # Check data type - SQLite uses simplified type system
            first_non_null = sample_values.iloc[0]
            
            if col in ['env_id']:
                column_types[col] = "TEXT"  # SQLite doesn't have VARCHAR limits
            elif col in ['timestamp']:
                column_types[col] = "DATETIME"
            elif isinstance(first_non_null, str):
                column_types[col] = "TEXT"
            elif pd.api.types.is_integer_dtype(sample_values):
                column_types[col] = "INTEGER"
            elif pd.api.types.is_float_dtype(sample_values):
                column_types[col] = "REAL"
            elif pd.api.types.is_bool_dtype(sample_values):
                column_types[col] = "INTEGER"  # SQLite stores booleans as integers
            else:
                column_types[col] = "TEXT"
        
        return column_types
    
    def create_table_from_csv(self, csv_file_path: str, drop_existing: bool = True) -> bool:
        """Create table structure matching CSV file"""
        
        if not self.connection:
            self.logger.error("No database connection")
            return False
        
        try:
            # Analyze CSV structure
            column_types = self.analyze_csv_structure(csv_file_path)
            self.logger.info(f"Analyzed {len(column_types)} columns from CSV")
            
            cursor = self.connection.cursor()
            
            # Drop existing table if requested
            if drop_existing:
                cursor.execute(f"DROP TABLE IF EXISTS {self.table}")
                self.logger.info(f"Dropped existing table {self.table}")
            
            # Create table SQL - SQLite syntax
            columns_sql = ["id INTEGER PRIMARY KEY AUTOINCREMENT"]
            
            for col, col_type in column_types.items():
                # SQLite doesn't need backticks for column names, but we'll use quotes for safety
                escaped_col = f'"{col}"'
                columns_sql.append(f"{escaped_col} {col_type}")
            
            create_sql = f"""
            CREATE TABLE IF NOT EXISTS {self.table} (
                {', '.join(columns_sql)}
            )
            """
            
            cursor.execute(create_sql)
            
            # Create indexes separately in SQLite
            try:
                cursor.execute(f'CREATE INDEX IF NOT EXISTS idx_env_id ON {self.table}("env_id")')
                cursor.execute(f'CREATE INDEX IF NOT EXISTS idx_timestamp ON {self.table}("timestamp")')
            except Exception as idx_error:
                self.logger.warning(f"Index creation warning: {idx_error}")
            
            self.connection.commit()
            self.logger.info(f"Created table {self.table} with {len(column_types)} columns")
            
            return True
            
        except Exception as e:
            self.logger.error(f"Error creating table: {e}")
            return False
    
    def insert_csv_data(self, csv_file_path: str) -> bool:
        """Insert data from CSV file into database"""
        
        if not self.connection:
            self.logger.error("No database connection")
            return False
        
        try:
            # Read CSV
            df = pd.read_csv(csv_file_path)
            self.logger.info(f"Reading {len(df)} rows from CSV")
            
            cursor = self.connection.cursor()
            
            # Prepare data for batch insertion
            columns = [f'"{col}"' for col in df.columns]  # Escape column names with quotes
            columns_str = ', '.join(columns)
            placeholders = ', '.join(['?'] * len(df.columns))  # SQLite uses ? placeholders
            
            insert_sql = f"INSERT INTO {self.table} ({columns_str}) VALUES ({placeholders})"
            
            # Prepare all rows for batch insert
            rows_data = []
            for index, row in df.iterrows():
                values = []
                for col in df.columns:
                    value = row[col]
                    
                    # Handle different data types
                    if pd.isna(value):
                        values.append(None)
                    elif isinstance(value, str):
                        values.append(value)
                    elif isinstance(value, (int, float)):
                        if np.isnan(value):
                            values.append(None)
                        else:
                            values.append(value)
                    else:
                        values.append(str(value))
                
                rows_data.append(values)
            
            # Execute batch insert
            cursor.executemany(insert_sql, rows_data)
            self.connection.commit()
            
            self.logger.info(f"Successfully inserted {len(df)} rows")
            return True
            
        except Exception as e:
            self.logger.error(f"Error inserting CSV data: {e}")
            return False
    
    def create_features_table(self):
        """Create features table if it doesn't exist (legacy method for backward compatibility)"""
        if not self.connection:
            self.logger.error("No database connection")
            return False
            
        try:
            cursor = self.connection.cursor()
            # Create comprehensive features table - SQLite syntax
            create_table_sql = f"""
            CREATE TABLE IF NOT EXISTS {self.table} (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                env_id TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                
                -- Environment file features
                env_frequency REAL,
                env_source_count INTEGER,
                env_receiver_count INTEGER,
                env_range_count INTEGER,
                env_max_depth REAL,
                env_ssp_type TEXT,
                
                -- PRT file features (arrival mode)
                prt_arr_success INTEGER DEFAULT 0,
                prt_arr_freq REAL,
                prt_arr_ssp_points INTEGER,
                prt_arr_ssp_min_speed REAL,
                prt_arr_ssp_max_speed REAL,
                prt_arr_ssp_mean_speed REAL,
                prt_arr_ssp_std_speed REAL,
                prt_arr_depth_min REAL,
                prt_arr_depth_max REAL,
                prt_arr_depth_range REAL,
                prt_arr_source_depths TEXT,
                prt_arr_receiver_depths TEXT,
                prt_arr_ranges TEXT,
                prt_arr_run_time REAL,
                
                -- PRT file features (ray mode)
                prt_ray_success INTEGER DEFAULT 0,
                prt_ray_freq REAL,
                prt_ray_ssp_points INTEGER,
                prt_ray_ssp_min_speed REAL,
                prt_ray_ssp_max_speed REAL,
                prt_ray_ssp_mean_speed REAL,
                prt_ray_ssp_std_speed REAL,
                prt_ray_depth_min REAL,
                prt_ray_depth_max REAL,
                prt_ray_depth_range REAL,
                prt_ray_source_depths TEXT,
                prt_ray_receiver_depths TEXT,
                prt_ray_ranges TEXT,
                prt_ray_beam_angles TEXT,
                prt_ray_run_time REAL,
                
                -- PRT file features (shd mode)
                prt_shd_success INTEGER DEFAULT 0,
                prt_shd_freq REAL,
                prt_shd_ssp_points INTEGER,
                prt_shd_ssp_min_speed REAL,
                prt_shd_ssp_max_speed REAL,
                prt_shd_ssp_mean_speed REAL,
                prt_shd_ssp_std_speed REAL,
                prt_shd_depth_min REAL,
                prt_shd_depth_max REAL,
                prt_shd_depth_range REAL,
                prt_shd_source_depths TEXT,
                prt_shd_receiver_depths TEXT,
                prt_shd_ranges TEXT,
                prt_shd_run_time REAL,
                
                -- ARR file features
                arr_arrival_count INTEGER,
                arr_min_time REAL,
                arr_max_time REAL,
                arr_mean_time REAL,
                arr_std_time REAL,
                arr_min_amplitude REAL,
                arr_max_amplitude REAL,
                arr_mean_amplitude REAL,
                arr_std_amplitude REAL,
                arr_angle_spread REAL,
                arr_phase_variance REAL,
                
                -- RAY file features
                ray_ray_count INTEGER,
                ray_total_points INTEGER,
                ray_max_range REAL,
                ray_max_depth REAL,
                ray_avg_ray_length REAL,
                ray_path_complexity REAL,
                ray_turning_points INTEGER,
                ray_bounce_count INTEGER,
                
                -- SHD file features
                shd_grid_size INTEGER,
                shd_range_extent REAL,
                shd_depth_extent REAL,
                shd_min_tl REAL,
                shd_max_tl REAL,
                shd_mean_tl REAL,
                shd_std_tl REAL,
                shd_tl_gradient REAL,
                shd_energy_distribution REAL,
                
                -- Derived features for ML
                ml_channel_complexity REAL,
                ml_propagation_efficiency REAL,
                ml_environment_variability REAL,
                ml_signal_quality REAL
            )
            """
            
            cursor.execute(create_table_sql)
            
            # Create indexes separately
            try:
                cursor.execute(f'CREATE INDEX IF NOT EXISTS idx_env_id ON {self.table}(env_id)')
                cursor.execute(f'CREATE INDEX IF NOT EXISTS idx_timestamp ON {self.table}(timestamp)')
            except Exception as idx_error:
                self.logger.warning(f"Index creation warning: {idx_error}")
            
            self.connection.commit()
            self.logger.info(f"Features table '{self.table}' created/verified")
            return True
                
        except Exception as e:
            self.logger.error(f"Error creating features table: {e}")
            return False
    
    def insert_features(self, features_df: pd.DataFrame) -> bool:
        """Insert features dataframe into database (legacy method for backward compatibility)"""
        if not self.connection:
            self.logger.error("No database connection")
            return False
            
        try:
            cursor = self.connection.cursor()
            
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
                placeholders = ', '.join(['?'] * len(columns))  # SQLite uses ? placeholders
                columns_str = ', '.join([f'"{col}"' for col in columns])  # Escape column names
                
                insert_sql = f"INSERT INTO {self.table} ({columns_str}) VALUES ({placeholders})"
                
                # Insert data
                for row_data in processed_data:
                    values = [row_data.get(col) for col in columns]
                    cursor.execute(insert_sql, values)
                
                self.connection.commit()
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
            cursor = self.connection.cursor()
            
            if env_ids:
                placeholders = ', '.join(['?'] * len(env_ids))  # SQLite uses ? placeholders
                query = f"SELECT * FROM {self.table} WHERE env_id IN ({placeholders}) ORDER BY env_id"
                cursor.execute(query, env_ids)
            else:
                query = f"SELECT * FROM {self.table} ORDER BY env_id"
                cursor.execute(query)
            
            results = cursor.fetchall()
            
            if results:
                # Convert sqlite3.Row objects to dictionaries
                data = [dict(row) for row in results]
                df = pd.DataFrame(data)
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
            
            cursor = self.connection.cursor()
            for _, row in df.iterrows():
                update_sql = f"""
                UPDATE {self.table} SET
                    ml_channel_complexity = ?,
                    ml_propagation_efficiency = ?,
                    ml_environment_variability = ?,
                    ml_signal_quality = ?
                WHERE id = ?
                """
                
                cursor.execute(update_sql, [
                    row.get('ml_channel_complexity'),
                    row.get('ml_propagation_efficiency'),
                    row.get('ml_environment_variability'),
                    row.get('ml_signal_quality'),
                    row.get('id')
                ])
            
            self.connection.commit()
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
            cursor = self.connection.cursor()
            
            # Get table structure - SQLite uses PRAGMA instead of DESCRIBE
            cursor.execute(f"PRAGMA table_info({self.table})")
            columns = cursor.fetchall()
            
            # Get row count
            cursor.execute(f"SELECT COUNT(*) as count FROM {self.table}")
            count_result = cursor.fetchone()
            row_count = count_result['count'] if count_result else 0
            
            # Get sample data
            cursor.execute(f"SELECT * FROM {self.table} LIMIT 5")
            sample_results = cursor.fetchall()
            sample_data = [dict(row) for row in sample_results]
            
            return {
                'columns': [dict(col) for col in columns],
                'row_count': row_count,
                'sample_data': sample_data
            }
                
        except Exception as e:
            self.logger.error(f"Error getting table info: {e}")
            return {}
    
    def create_and_populate_from_csv(self, csv_file_path: str) -> bool:
        """Complete workflow: create table and populate from CSV"""
        
        if not os.path.exists(csv_file_path):
            self.logger.error(f"CSV file not found: {csv_file_path}")
            return False
        
        self.logger.info(f"Starting database creation and population from {csv_file_path}")
        
        # Step 1: Create table
        if not self.create_table_from_csv(csv_file_path):
            return False
        
        # Step 2: Insert data
        if not self.insert_csv_data(csv_file_path):
            return False
        
        # Step 3: Verify
        info = self.get_table_info()
        self.logger.info(f"Verification: {info['row_count']} rows inserted")
        
        return True
    
    def __enter__(self):
        """Context manager entry"""
        self.connect()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Context manager exit"""
        self.disconnect()

def test_database_connection():
    """Test basic database connection and operations"""
    db = DatabaseManager()
    
    if db.connect():
        print("[OK] Database connection successful")
        
        # Create table
        if db.create_features_table():
            print("[OK] Features table created/verified")
        
        # Get table info
        info = db.get_table_info()
        if info:
            print(f"[OK] Table has {info['row_count']} rows and {len(info['columns'])} columns")
        
        db.disconnect()
        return True
    else:
        print("[FAIL] Database connection failed")
        return False

def test_database_v2():
    """Test the enhanced database functionality with CSV auto-detection"""
    
    # Find latest CSV file
    features_dir = Path("results/features")
    csv_files = list(features_dir.glob("*.csv"))
    
    if not csv_files:
        print("No CSV files found in results/features/")
        return False
    
    # Use the most recent CSV file
    latest_csv = max(csv_files, key=os.path.getctime)
    print(f"Using CSV file: {latest_csv}")
    
    # Test database operations
    db = DatabaseManager()
    
    if db.connect():
        print("[OK] Database connection successful")
        
        # Create and populate
        if db.create_and_populate_from_csv(str(latest_csv)):
            print("[OK] Table created and populated successfully")
            
            # Get info
            info = db.get_table_info()
            print(f"[OK] Table contains {info['row_count']} rows")
            print(f"[OK] Table has {len(info['columns'])} columns")
            
            # Show sample data
            if info['sample_data']:
                print("[OK] Sample data:")
                for i, sample in enumerate(info['sample_data'][:3]):
                    print(f"   Row {i+1}: env_id={sample.get('env_id')}")
        
        db.disconnect()
        return True
    else:
        print("[FAIL] Database connection failed")
        return False

if __name__ == "__main__":
    # Test both basic and enhanced functionality
    print("=== Testing Basic Database Connection ===")
    test_database_connection()
    print("\n=== Testing Enhanced Database with CSV Auto-detection ===")
    test_database_v2()