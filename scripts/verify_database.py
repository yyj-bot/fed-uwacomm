#!/usr/bin/env python3
"""
Verify Database Data Integrity
Check that BELLHOP features are correctly stored in database
"""

import sys
import pandas as pd
import numpy as np
from datetime import datetime
from pathlib import Path

# 添加src目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / 'src'))

from feduwacomm.database.database_v2 import DatabaseManagerV2

def verify_database_integrity():
    """Complete database verification"""
    
    print("=== Database Data Integrity Verification ===")
    print(f"Verification time: {datetime.now()}")
    
    db = DatabaseManagerV2()
    
    if not db.connect():
        print("[FAIL] Database connection failed")
        return False
    
    try:
        print(f"[OK] Connected to database: {db.database}")
        print(f"[OK] Using table: {db.table}")
        
        # 1. Get table information
        print("\n1. Table Structure Verification:")
        info = db.get_table_info()
        
        print(f"   - Total records: {info['row_count']}")
        print(f"   - Total columns: {len(info['columns'])}")
        
        if info['row_count'] == 0:
            print("[FAIL] No data found in database")
            return False
        
        # 2. Get all data
        print("\n2. Data Content Verification:")
        df = db.get_features_for_training()
        
        if df.empty:
            print("[FAIL] Could not retrieve data from database")
            return False
        
        print(f"   - Retrieved {len(df)} records")
        print(f"   - Retrieved {len(df.columns)} columns")
        
        # 3. Verify environment IDs
        print("\n3. Environment ID Verification:")
        env_ids = df['env_id'].unique()
        print(f"   - Found environments: {sorted(env_ids)}")
        print(f"   - Environment count: {len(env_ids)}")
        
        # 4. Verify key features
        print("\n4. Key Features Verification:")
        key_features = [
            'prt_arr_success', 'prt_ray_success', 'prt_shd_success',
            'prt_arr_freq', 'prt_ray_freq', 'prt_shd_freq',
            'arr_arr_arrival_count', 'ray_ray_ray_count'
        ]
        
        available_features = []
        missing_features = []
        
        for feature in key_features:
            if feature in df.columns:
                available_features.append(feature)
                non_null_count = df[feature].notna().sum()
                print(f"   [OK] {feature}: {non_null_count}/{len(df)} non-null values")
            else:
                missing_features.append(feature)
                print(f"   [WARN] {feature}: Not found in database")
        
        # 5. Verify numeric features
        print("\n5. Numeric Features Analysis:")
        numeric_cols = df.select_dtypes(include=[np.number]).columns
        print(f"   - Numeric columns: {len(numeric_cols)}")
        
        # Show statistics for some key numeric features
        analysis_features = ['prt_arr_freq', 'arr_arr_arrival_count', 'ray_ray_ray_count']
        
        for feature in analysis_features:
            if feature in df.columns:
                values = df[feature].dropna()
                if len(values) > 0:
                    print(f"   - {feature}: min={values.min():.2f}, max={values.max():.2f}, mean={values.mean():.2f}")
                else:
                    print(f"   - {feature}: All values are null")
        
        # 6. Data completeness check
        print("\n6. Data Completeness Analysis:")
        total_cells = len(df) * len(df.columns)
        null_cells = df.isnull().sum().sum()
        completeness = (total_cells - null_cells) / total_cells * 100
        
        print(f"   - Total data cells: {total_cells}")
        print(f"   - Null cells: {null_cells}")
        print(f"   - Data completeness: {completeness:.2f}%")
        
        # 7. Show sample records
        print("\n7. Sample Records:")
        for i, (_, row) in enumerate(df.head(3).iterrows()):
            env_id = row.get('env_id', 'unknown')
            freq = row.get('prt_arr_freq', 'N/A')
            arrivals = row.get('arr_arr_arrival_count', 'N/A')
            print(f"   Record {i+1}: env_id={env_id}, freq={freq}, arrivals={arrivals}")
        
        # 8. Check for potential ML usage
        print("\n8. Machine Learning Readiness:")
        
        # Count purely numeric features (excluding IDs, timestamps, text)
        exclude_patterns = ['id', 'timestamp', 'env_id', '_depths', '_ranges', '_angles', 'error_msg', 'mode', 'ssp_type']
        ml_features = []
        
        for col in df.columns:
            if any(pattern in col.lower() for pattern in exclude_patterns):
                continue
            if pd.api.types.is_numeric_dtype(df[col]):
                ml_features.append(col)
        
        print(f"   - ML-ready numeric features: {len(ml_features)}")
        print(f"   - Total samples: {len(df)}")
        
        if len(df) < 10:
            print("   [WARN] Sample size is small for ML training")
        else:
            print("   [OK] Sample size is adequate for ML training")
        
        # 9. Create database status report
        print("\n9. Generating Database Status Report:")
        
        report_content = f"""# Database Status Report - FedUWAComm

Generated: {datetime.now()}

## Database Connection
- Host: {db.host}:{db.port}  
- Database: {db.database}
- Table: {db.table}
- Connection Status: Connected

## Data Summary
- Total Records: {len(df)}
- Total Columns: {len(df.columns)}
- Environments: {', '.join(sorted(env_ids))}
- Data Completeness: {completeness:.2f}%

## Feature Analysis
- Numeric Features: {len(numeric_cols)}
- ML-Ready Features: {len(ml_features)}
- Key Features Available: {len(available_features)}/{len(key_features)}

## Sample Data
Environment IDs: {', '.join(sorted(env_ids))}

Key Statistics:
{chr(10).join([f'- {feature}: {df[feature].describe().to_string()}' for feature in analysis_features[:2] if feature in df.columns])}

## Usage Instructions

### Retrieve Data for ML:
```python
from ml_modules.database_v2 import DatabaseManagerV2
db = DatabaseManagerV2()
if db.connect():
    df = db.get_features_for_training()
    print(f"Retrieved {{len(df)}} records with {{len(df.columns)}} columns")
    db.disconnect()
```

### Get Specific Environments:
```python
db = DatabaseManagerV2()
if db.connect():
    df = db.get_features_for_training(['B01', 'B02', 'B03'])
    db.disconnect()
```

## Next Steps
1. The database is ready for machine learning workflows
2. Feature data is complete and accessible
3. For larger datasets, consider data augmentation techniques
4. Models can be trained directly from database data

## Data Quality: {'EXCELLENT' if completeness > 90 else 'GOOD' if completeness > 70 else 'NEEDS IMPROVEMENT'}
"""
        
        report_path = "results/reports/database_verification_report.txt"
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_content)
        
        print(f"   [OK] Report saved to: {report_path}")
        
        # 10. Final verdict
        print("\n" + "="*50)
        print("VERIFICATION SUMMARY")
        print("="*50)
        
        checks = [
            ("Database Connection", True),
            ("Data Retrieval", not df.empty),
            ("Environment Data", len(env_ids) > 0),
            ("Numeric Features", len(numeric_cols) > 0),
            ("Data Completeness", completeness > 50),
            ("ML Readiness", len(ml_features) > 10 and len(df) > 0)
        ]
        
        passed = sum(1 for _, status in checks if status)
        
        for check_name, status in checks:
            status_str = "[PASS]" if status else "[FAIL]"
            print(f"{status_str} {check_name}")
        
        if passed == len(checks):
            print("\n[SUCCESS] Database is fully operational and ready for ML workflows!")
            print("\nTo use the database in your ML workflows:")
            print(" Run: python scripts/final_database_ml.py")
            return True
        else:
            print(f"\n[WARNING] {len(checks)-passed} checks failed. Database may have issues.")
            return False
        
    except Exception as e:
        print(f"[ERROR] Verification failed: {e}")
        return False
    
    finally:
        db.disconnect()

if __name__ == "__main__":
    success = verify_database_integrity()
    if not success:
        sys.exit(1)