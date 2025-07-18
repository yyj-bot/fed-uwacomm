#!/usr/bin/env python3
"""
查询数据库中的特征数据，检查是否每个文件都有不同的特征值
"""

import pymysql

def main():
    """主函数"""
    # 连接数据库
    conn = pymysql.connect(
        host='localhost',
        port=3306,
        user='root',
        password='123456Lrn.',
        database='bellhop_data'
    )
    
    try:
        cursor = conn.cursor()
        
        # 查询声速剖面特征
        print("查询声速剖面特征...")
        cursor.execute("""
            SELECT id, filename, success, freq, ssp_points, ssp_min, ssp_max, ssp_mean 
            FROM features
        """)
        rows = cursor.fetchall()
        
        # 打印表头
        print("\nID | 文件名 | 成功 | 频率 | SSP点数 | SSP最小值 | SSP最大值 | SSP平均值")
        print("-" * 100)
        
        # 打印数据
        for row in rows:
            id, filename, success, freq, ssp_points, ssp_min, ssp_max, ssp_mean = row
            print(f"{id} | {filename} | {success} | {freq} | {ssp_points} | {ssp_min} | {ssp_max} | {ssp_mean}")
        
        # 查询传播损失特征
        print("\n查询传播损失特征...")
        cursor.execute("""
            SELECT id, filename, shd_tl_min, shd_tl_max, shd_tl_mean, shd_tl_std, shd_tl_median, shd_coherent
            FROM features
        """)
        rows = cursor.fetchall()
        
        # 打印表头
        print("\nID | 文件名 | TL最小值 | TL最大值 | TL平均值 | TL标准差 | TL中位数 | 相干场")
        print("-" * 110)
        
        # 打印数据
        for row in rows:
            id, filename, tl_min, tl_max, tl_mean, tl_std, tl_median, coherent = row
            print(f"{id} | {filename} | {tl_min} | {tl_max} | {tl_mean} | {tl_std} | {tl_median} | {coherent}")
        
        # 查询射线路径特征
        print("\n查询射线路径特征...")
        cursor.execute("""
            SELECT id, filename, ray_ray_count, ray_max_depth, ray_min_depth, ray_avg_depth, ray_bounce_count
            FROM features
        """)
        rows = cursor.fetchall()
        
        # 打印表头
        print("\nID | 文件名 | 射线数量 | 最大深度 | 最小深度 | 平均深度 | 反射次数")
        print("-" * 100)
        
        # 打印数据
        for row in rows:
            id, filename, ray_count, max_depth, min_depth, avg_depth, bounce_count = row
            print(f"{id} | {filename} | {ray_count} | {max_depth} | {min_depth} | {avg_depth} | {bounce_count}")
    
    finally:
        # 关闭连接
        cursor.close()
        conn.close()

if __name__ == '__main__':
    main() 