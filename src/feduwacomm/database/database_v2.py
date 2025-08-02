#!/usr/bin/env python3
"""
Database V2: Automatically match CSV structure
Handle MySQL database operations with dynamic table creation
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

class DatabaseManagerV2:
    """Enhanced database manager with automatic table structure matching"""
    
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
    
    def analyze_csv_structure(self, csv_file_path: str) -> Dict[str, str]:
        """Analyze CSV file to determine column types"""
        df = pd.read_csv(csv_file_path)
        
        column_types = {}
        
        for col in df.columns:
            # Get sample values (non-null)
            sample_values = df[col].dropna()
            
            if len(sample_values) == 0:
                column_types[col] = "TEXT"
                continue
            
            # Check data type
            first_non_null = sample_values.iloc[0]
            
            if col in ['env_id']:
                column_types[col] = "VARCHAR(50)"
            elif col in ['timestamp']:
                column_types[col] = "DATETIME"
            elif isinstance(first_non_null, str):
                # Check if it's a list-like string
                if first_non_null.startswith('[') and first_non_null.endswith(']'):
                    column_types[col] = "TEXT"
                elif len(str(first_non_null)) > 255:
                    column_types[col] = "TEXT"
                else:
                    column_types[col] = "VARCHAR(500)"
            elif pd.api.types.is_integer_dtype(sample_values):
                column_types[col] = "INT"
            elif pd.api.types.is_float_dtype(sample_values):
                column_types[col] = "DOUBLE"
            elif pd.api.types.is_bool_dtype(sample_values):
                column_types[col] = "TINYINT"
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
            
            # Drop existing table if requested
            if drop_existing:
                with self.connection.cursor() as cursor:
                    cursor.execute(f"DROP TABLE IF EXISTS {self.table}")
                    self.logger.info(f"Dropped existing table {self.table}")
            
            # Create table SQL
            columns_sql = ["id INT AUTO_INCREMENT PRIMARY KEY"]
            
            for col, col_type in column_types.items():
                # Escape column names with backticks
                escaped_col = f"`{col}`"
                columns_sql.append(f"{escaped_col} {col_type}")
            
            # Add indexes
            columns_sql.append("INDEX idx_env_id (`env_id`)")
            columns_sql.append("INDEX idx_timestamp (`timestamp`)")
            
            create_sql = f"""
            CREATE TABLE {self.table} (
                {', '.join(columns_sql)}
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """
            
            with self.connection.cursor() as cursor:
                cursor.execute(create_sql)
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
            
            # Prepare data for insertion
            for index, row in df.iterrows():
                # Prepare column names and values
                columns = []
                values = []
                
                for col in df.columns:
                    columns.append(f"`{col}`")  # Escape column names
                    
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
                
                # Create insert SQL
                placeholders = ', '.join(['%s'] * len(values))
                columns_str = ', '.join(columns)
                
                insert_sql = f"INSERT INTO {self.table} ({columns_str}) VALUES ({placeholders})"
                
                # Execute insert
                with self.connection.cursor() as cursor:
                    cursor.execute(insert_sql, values)
                
                self.logger.info(f"Inserted row {index + 1}: {row.get('env_id', 'unknown')}")
            
            self.logger.info(f"Successfully inserted {len(df)} rows")
            return True
            
        except Exception as e:
            self.logger.error(f"Error inserting CSV data: {e}")
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

def test_database_v2():
    """Test the enhanced database functionality"""
    
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
    db = DatabaseManagerV2()
    
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
    test_database_v2()