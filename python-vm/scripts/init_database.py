#!/usr/bin/env python3
"""
初始化数据库 - 从CSV文件自动创建表结构并导入数据
"""

import sys
from pathlib import Path

# 添加src目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root / 'src'))

from feduwacomm.database.database_v2 import DatabaseManagerV2


def initialize_database():
    """初始化数据库表并导入CSV数据"""
    
    print("=== 数据库初始化 ===")
    
    # CSV文件路径
    csv_file = "bellhop_features_final.csv"
    
    if not Path(csv_file).exists():
        print(f"❌ CSV文件不存在: {csv_file}")
        return False
    
    print(f"✅ 找到CSV文件: {csv_file}")
    
    # 初始化数据库管理器
    db = DatabaseManagerV2()
    
    try:
        # 连接数据库
        if not db.connect():
            print("❌ 数据库连接失败")
            return False
        
        print("✅ 数据库连接成功")
        
        # 自动创建表并导入数据
        print("🔄 开始创建表并导入数据...")
        success = db.create_and_populate_from_csv(csv_file)
        
        if success:
            print("✅ 数据库初始化完成！")
            print("📊 现在可以运行ML工作流了")
            return True
        else:
            print("❌ 数据导入失败")
            return False
            
    except Exception as e:
        print(f"❌ 初始化失败: {e}")
        return False
        
    finally:
        db.disconnect()


if __name__ == "__main__":
    success = initialize_database()
    
    if success:
        print("\n🎉 成功！现在可以运行:")
        print("   python scripts/verify_database.py")
        print("   python scripts/complete_workflow.py")
    else:
        print("\n💡 请检查:")
        print("   1. 数据库连接配置 (.env文件)")
        print("   2. CSV文件是否存在")
        print("   3. 数据库权限") 