"""
SQLite本地存储模块，用于存储虚拟机配置和认证信息
v1.5.1: 添加数据集存储和验证功能
"""

import sqlite3
import json
import logging
from pathlib import Path
from typing import Optional, Dict, Any, List
from datetime import datetime


class VMStorage:
    """虚拟机本地存储管理器"""
    
    def __init__(self, db_path: str = "vm_storage.db"):
        """初始化存储管理器
        
        Args:
            db_path: 数据库文件路径
        """
        self.db_path = db_path
        self.logger = logging.getLogger("VMStorage")
        self._init_database()
    
    def _init_database(self):
        """初始化数据库表结构"""
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                # 创建虚拟机认证信息表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS vm_auth (
                        vm_id TEXT PRIMARY KEY,
                        secret_id TEXT NOT NULL,
                        access_token TEXT,
                        token_expire_time INTEGER,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                
                # 创建虚拟机配置信息表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS vm_config (
                        vm_id TEXT PRIMARY KEY,
                        config_data TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                
                # 创建WebSocket连接信息表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS vm_connection (
                        vm_id TEXT PRIMARY KEY,
                        websocket_url TEXT,
                        connection_status TEXT DEFAULT 'DISCONNECTED',
                        last_heartbeat TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                
                # v1.5.1: 创建数据集信息表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS datasets (
                        assigned_dataset_id TEXT PRIMARY KEY,
                        task_id TEXT NOT NULL,
                        dataset_name TEXT,
                        dataset_type TEXT,
                        status TEXT DEFAULT 'PENDING',
                        expected_rows INTEGER DEFAULT 0,
                        actual_rows INTEGER DEFAULT 0,
                        start_index INTEGER,
                        end_index INTEGER,
                        allocation_strategy TEXT,
                        slice_info TEXT,
                        metadata TEXT,
                        schema TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        completed_at TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """)
                
                # v1.5.1: 创建数据集行数据表
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS dataset_rows (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        assigned_dataset_id TEXT NOT NULL,
                        batch_id TEXT,
                        row_id TEXT,
                        local_index INTEGER,
                        global_index INTEGER,
                        row_data TEXT NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (assigned_dataset_id) REFERENCES datasets(assigned_dataset_id)
                    )
                """)
                
                # v1.5.1: 创建索引以提高查询性能
                cursor.execute("""
                    CREATE INDEX IF NOT EXISTS idx_datasets_task_id 
                    ON datasets(task_id)
                """)
                cursor.execute("""
                    CREATE INDEX IF NOT EXISTS idx_datasets_status 
                    ON datasets(status)
                """)
                cursor.execute("""
                    CREATE INDEX IF NOT EXISTS idx_dataset_rows_dataset_id 
                    ON dataset_rows(assigned_dataset_id)
                """)
                cursor.execute("""
                    CREATE INDEX IF NOT EXISTS idx_dataset_rows_global_index 
                    ON dataset_rows(assigned_dataset_id, global_index)
                """)
                
                conn.commit()
                self.logger.info("数据库初始化完成")
                
        except Exception as e:
            self.logger.error(f"数据库初始化失败: {e}")
            raise
    
    def save_secret_id(self, vm_id: str, secret_id: str, access_token: str = None, 
                      token_expire_seconds: int = None) -> bool:
        """保存虚拟机的secretId和访问令牌
        
        Args:
            vm_id: 虚拟机ID
            secret_id: 长期刷新凭证
            access_token: 访问令牌
            token_expire_seconds: 令牌过期时间（秒）
            
        Returns:
            bool: 保存是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                # 计算令牌过期时间戳
                token_expire_time = None
                if token_expire_seconds:
                    token_expire_time = int(datetime.now().timestamp()) + token_expire_seconds
                
                cursor.execute("""
                    INSERT OR REPLACE INTO vm_auth 
                    (vm_id, secret_id, access_token, token_expire_time, updated_at)
                    VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, (vm_id, secret_id, access_token, token_expire_time))
                
                conn.commit()
                self.logger.info(f"已保存虚拟机 {vm_id} 的认证信息")
                return True
                
        except Exception as e:
            self.logger.error(f"保存认证信息失败: {e}")
            return False
    
    def get_secret_id(self, vm_id: str) -> Optional[str]:
        """获取虚拟机的secretId
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            str: secretId，如果不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT secret_id FROM vm_auth WHERE vm_id = ?", (vm_id,))
                result = cursor.fetchone()
                return result[0] if result else None
                
        except Exception as e:
            self.logger.error(f"获取secretId失败: {e}")
            return None
    
    def get_access_token(self, vm_id: str) -> Optional[Dict[str, Any]]:
        """获取虚拟机的访问令牌信息
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            dict: 包含access_token和过期时间的字典，如果不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT access_token, token_expire_time 
                    FROM vm_auth WHERE vm_id = ?
                """, (vm_id,))
                result = cursor.fetchone()
                
                if result:
                    access_token, expire_time = result
                    return {
                        'access_token': access_token,
                        'expire_time': expire_time,
                        'is_expired': expire_time and expire_time < datetime.now().timestamp()
                    }
                return None
                
        except Exception as e:
            self.logger.error(f"获取访问令牌失败: {e}")
            return None
    
    def update_access_token(self, vm_id: str, access_token: str, 
                           token_expire_seconds: int) -> bool:
        """更新访问令牌
        
        Args:
            vm_id: 虚拟机ID
            access_token: 新的访问令牌
            token_expire_seconds: 令牌过期时间（秒）
            
        Returns:
            bool: 更新是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                token_expire_time = int(datetime.now().timestamp()) + token_expire_seconds
                
                cursor.execute("""
                    UPDATE vm_auth 
                    SET access_token = ?, token_expire_time = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE vm_id = ?
                """, (access_token, token_expire_time, vm_id))
                
                conn.commit()
                self.logger.info(f"已更新虚拟机 {vm_id} 的访问令牌")
                return cursor.rowcount > 0
                
        except Exception as e:
            self.logger.error(f"更新访问令牌失败: {e}")
            return False
    
    def save_vm_config(self, vm_id: str, config_data: Dict[str, Any]) -> bool:
        """保存虚拟机配置信息
        
        Args:
            vm_id: 虚拟机ID
            config_data: 配置数据字典
            
        Returns:
            bool: 保存是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                config_json = json.dumps(config_data, ensure_ascii=False, indent=2)
                
                cursor.execute("""
                    INSERT OR REPLACE INTO vm_config 
                    (vm_id, config_data, updated_at)
                    VALUES (?, ?, CURRENT_TIMESTAMP)
                """, (vm_id, config_json))
                
                conn.commit()
                self.logger.info(f"已保存虚拟机 {vm_id} 的配置信息")
                return True
                
        except Exception as e:
            self.logger.error(f"保存配置信息失败: {e}")
            return False
    
    def get_vm_config(self, vm_id: str) -> Optional[Dict[str, Any]]:
        """获取虚拟机配置信息
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            dict: 配置数据字典，如果不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("SELECT config_data FROM vm_config WHERE vm_id = ?", (vm_id,))
                result = cursor.fetchone()
                
                if result:
                    return json.loads(result[0])
                return None
                
        except Exception as e:
            self.logger.error(f"获取配置信息失败: {e}")
            return None
    
    def save_connection_info(self, vm_id: str, websocket_url: str, 
                           connection_status: str = "DISCONNECTED") -> bool:
        """保存WebSocket连接信息
        
        Args:
            vm_id: 虚拟机ID
            websocket_url: WebSocket连接URL
            connection_status: 连接状态
            
        Returns:
            bool: 保存是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                cursor.execute("""
                    INSERT OR REPLACE INTO vm_connection 
                    (vm_id, websocket_url, connection_status, updated_at)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                """, (vm_id, websocket_url, connection_status))
                
                conn.commit()
                self.logger.info(f"已保存虚拟机 {vm_id} 的连接信息")
                return True
                
        except Exception as e:
            self.logger.error(f"保存连接信息失败: {e}")
            return False
    
    def update_connection_status(self, vm_id: str, status: str) -> bool:
        """更新连接状态
        
        Args:
            vm_id: 虚拟机ID
            status: 连接状态
            
        Returns:
            bool: 更新是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                cursor.execute("""
                    UPDATE vm_connection 
                    SET connection_status = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE vm_id = ?
                """, (status, vm_id))
                
                conn.commit()
                return cursor.rowcount > 0
                
        except Exception as e:
            self.logger.error(f"更新连接状态失败: {e}")
            return False
    
    def update_heartbeat(self, vm_id: str) -> bool:
        """更新心跳时间
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            bool: 更新是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                cursor.execute("""
                    UPDATE vm_connection 
                    SET last_heartbeat = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                    WHERE vm_id = ?
                """, (vm_id,))
                
                conn.commit()
                return cursor.rowcount > 0
                
        except Exception as e:
            self.logger.error(f"更新心跳时间失败: {e}")
            return False
    
    def get_connection_info(self, vm_id: str) -> Optional[Dict[str, Any]]:
        """获取连接信息
        
        Args:
            vm_id: 虚拟机ID
            
        Returns:
            dict: 连接信息字典，如果不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT websocket_url, connection_status, last_heartbeat
                    FROM vm_connection WHERE vm_id = ?
                """, (vm_id,))
                result = cursor.fetchone()
                
                if result:
                    websocket_url, connection_status, last_heartbeat = result
                    return {
                        'websocket_url': websocket_url,
                        'connection_status': connection_status,
                        'last_heartbeat': last_heartbeat
                    }
                return None
                
        except Exception as e:
            self.logger.error(f"获取连接信息失败: {e}")
            return None
    
    def cleanup_expired_tokens(self) -> int:
        """清理过期的访问令牌
        
        Returns:
            int: 清理的记录数量
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                current_time = int(datetime.now().timestamp())
                cursor.execute("""
                    UPDATE vm_auth 
                    SET access_token = NULL, token_expire_time = NULL
                    WHERE token_expire_time IS NOT NULL AND token_expire_time < ?
                """, (current_time,))
                
                conn.commit()
                cleaned_count = cursor.rowcount
                
                if cleaned_count > 0:
                    self.logger.info(f"清理了 {cleaned_count} 个过期的访问令牌")
                
                return cleaned_count
                
        except Exception as e:
            self.logger.error(f"清理过期令牌失败: {e}")
            return 0
    
    # ========== v1.5.1: 数据集存储方法 ==========
    def save_dataset(self, assigned_dataset_id: str, task_id: str,
                    dataset_name: str = None, dataset_type: str = None,
                    expected_rows: int = 0, slice_info: Dict[str, Any] = None,
                    metadata: Dict[str, Any] = None, schema: Dict[str, str] = None) -> bool:
        """保存数据集信息 - v1.5.1
        
        Args:
            assigned_dataset_id: 后端分配的数据集ID
            task_id: 任务ID
            dataset_name: 数据集名称
            dataset_type: 数据集类型
            expected_rows: 预期行数
            slice_info: 切片信息
            metadata: 元数据
            schema: 数据schema
            
        Returns:
            bool: 保存是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                # 提取切片信息
                start_index = None
                end_index = None
                allocation_strategy = None
                slice_info_json = None
                
                if slice_info:
                    start_index = slice_info.get("startIndex")
                    end_index = slice_info.get("endIndex")
                    allocation_strategy = slice_info.get("allocationStrategy")
                    slice_info_json = json.dumps(slice_info, ensure_ascii=False)
                
                # 转换元数据和schema为JSON
                metadata_json = json.dumps(metadata, ensure_ascii=False) if metadata else None
                schema_json = json.dumps(schema, ensure_ascii=False) if schema else None
                
                cursor.execute("""
                    INSERT OR REPLACE INTO datasets 
                    (assigned_dataset_id, task_id, dataset_name, dataset_type,
                     status, expected_rows, start_index, end_index, 
                     allocation_strategy, slice_info, metadata, schema, updated_at)
                    VALUES (?, ?, ?, ?, 'CREATED', ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, (assigned_dataset_id, task_id, dataset_name, dataset_type,
                     expected_rows, start_index, end_index, allocation_strategy,
                     slice_info_json, metadata_json, schema_json))
                
                conn.commit()
                self.logger.info(f"已保存数据集 {assigned_dataset_id}")
                return True
                
        except Exception as e:
            self.logger.error(f"保存数据集失败: {e}")
            return False
    
    def save_dataset_rows(self, assigned_dataset_id: str, batch_id: str,
                         rows: List[Dict[str, Any]]) -> bool:
        """保存数据集行数据 - v1.5.1
        
        Args:
            assigned_dataset_id: 数据集ID
            batch_id: 批次ID
            rows: 数据行列表（包含localIndex, globalIndex和data）
            
        Returns:
            bool: 保存是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                for row in rows:
                    row_id = row.get("rowId")
                    local_index = row.get("localIndex")
                    global_index = row.get("globalIndex")
                    row_data_json = json.dumps(row.get("data", {}), ensure_ascii=False)
                    
                    cursor.execute("""
                        INSERT INTO dataset_rows 
                        (assigned_dataset_id, batch_id, row_id, local_index, 
                         global_index, row_data)
                        VALUES (?, ?, ?, ?, ?, ?)
                    """, (assigned_dataset_id, batch_id, row_id, local_index,
                         global_index, row_data_json))
                
                conn.commit()
                self.logger.debug(f"已保存 {len(rows)} 行数据到数据集 {assigned_dataset_id}")
                return True
                
        except Exception as e:
            self.logger.error(f"保存数据集行失败: {e}")
            return False
    
    def update_dataset_status(self, assigned_dataset_id: str, status: str,
                             actual_rows: int = None, completed_at: str = None) -> bool:
        """更新数据集状态 - v1.5.1
        
        Args:
            assigned_dataset_id: 数据集ID
            status: 状态
            actual_rows: 实际行数
            completed_at: 完成时间
            
        Returns:
            bool: 更新是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                if actual_rows is not None:
                    cursor.execute("""
                        UPDATE datasets 
                        SET status = ?, actual_rows = ?, completed_at = ?, 
                            updated_at = CURRENT_TIMESTAMP
                        WHERE assigned_dataset_id = ?
                    """, (status, actual_rows, completed_at, assigned_dataset_id))
                else:
                    cursor.execute("""
                        UPDATE datasets 
                        SET status = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE assigned_dataset_id = ?
                    """, (status, assigned_dataset_id))
                
                conn.commit()
                return cursor.rowcount > 0
                
        except Exception as e:
            self.logger.error(f"更新数据集状态失败: {e}")
            return False
    
    def get_dataset_info(self, assigned_dataset_id: str) -> Optional[Dict[str, Any]]:
        """获取数据集信息 - v1.5.1
        
        Args:
            assigned_dataset_id: 数据集ID
            
        Returns:
            dict: 数据集信息，不存在返回None
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute("""
                    SELECT task_id, dataset_name, dataset_type, status,
                           expected_rows, actual_rows, start_index, end_index,
                           allocation_strategy, slice_info, metadata, schema,
                           created_at, completed_at
                    FROM datasets WHERE assigned_dataset_id = ?
                """, (assigned_dataset_id,))
                result = cursor.fetchone()
                
                if result:
                    (task_id, dataset_name, dataset_type, status, expected_rows,
                     actual_rows, start_index, end_index, allocation_strategy,
                     slice_info_json, metadata_json, schema_json,
                     created_at, completed_at) = result
                    
                    return {
                        'task_id': task_id,
                        'dataset_name': dataset_name,
                        'dataset_type': dataset_type,
                        'status': status,
                        'expected_rows': expected_rows,
                        'actual_rows': actual_rows,
                        'start_index': start_index,
                        'end_index': end_index,
                        'allocation_strategy': allocation_strategy,
                        'slice_info': json.loads(slice_info_json) if slice_info_json else None,
                        'metadata': json.loads(metadata_json) if metadata_json else None,
                        'schema': json.loads(schema_json) if schema_json else None,
                        'created_at': created_at,
                        'completed_at': completed_at
                    }
                return None
                
        except Exception as e:
            self.logger.error(f"获取数据集信息失败: {e}")
            return None
    
    def get_dataset_rows(self, assigned_dataset_id: str, 
                        start_global_index: int = None,
                        end_global_index: int = None) -> List[Dict[str, Any]]:
        """获取数据集行数据 - v1.5.1
        
        Args:
            assigned_dataset_id: 数据集ID
            start_global_index: 起始全局索引（可选）
            end_global_index: 结束全局索引（可选）
            
        Returns:
            list: 数据行列表
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                if start_global_index is not None and end_global_index is not None:
                    cursor.execute("""
                        SELECT row_id, local_index, global_index, row_data
                        FROM dataset_rows
                        WHERE assigned_dataset_id = ? 
                          AND global_index >= ? AND global_index <= ?
                        ORDER BY global_index
                    """, (assigned_dataset_id, start_global_index, end_global_index))
                else:
                    cursor.execute("""
                        SELECT row_id, local_index, global_index, row_data
                        FROM dataset_rows
                        WHERE assigned_dataset_id = ?
                        ORDER BY global_index
                    """, (assigned_dataset_id,))
                
                rows = []
                for result in cursor.fetchall():
                    row_id, local_index, global_index, row_data_json = result
                    rows.append({
                        'rowId': row_id,
                        'localIndex': local_index,
                        'globalIndex': global_index,
                        'data': json.loads(row_data_json)
                    })
                
                return rows
                
        except Exception as e:
            self.logger.error(f"获取数据集行失败: {e}")
            return []
    
    def verify_dataset_completeness(self, assigned_dataset_id: str) -> Dict[str, Any]:
        """验证数据集完整性 - v1.5.1
        
        Args:
            assigned_dataset_id: 数据集ID
            
        Returns:
            dict: 验证结果
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                # 获取数据集信息
                dataset_info = self.get_dataset_info(assigned_dataset_id)
                if not dataset_info:
                    return {"isComplete": False, "error": "数据集不存在"}
                
                # 获取所有全局索引
                cursor.execute("""
                    SELECT global_index FROM dataset_rows
                    WHERE assigned_dataset_id = ?
                    ORDER BY global_index
                """, (assigned_dataset_id,))
                
                received_indices = [row[0] for row in cursor.fetchall() if row[0] is not None]
                
                if not received_indices:
                    return {
                        "isComplete": False,
                        "expectedSamples": dataset_info['expected_rows'],
                        "actualSamples": 0,
                        "missingIndices": []
                    }
                
                # 验证完整性
                expected_start = dataset_info['start_index']
                expected_end = dataset_info['end_index']
                expected_samples = dataset_info['expected_rows']
                
                actual_samples = len(received_indices)
                actual_start = received_indices[0]
                actual_end = received_indices[-1]
                
                # 查找缺失索引
                missing_indices = []
                if expected_start is not None and expected_end is not None:
                    expected_set = set(range(expected_start, expected_end + 1))
                    received_set = set(received_indices)
                    missing_indices = sorted(expected_set - received_set)
                
                is_complete = (
                    actual_samples == expected_samples and
                    len(missing_indices) == 0 and
                    actual_start == expected_start and
                    actual_end == expected_end
                )
                
                return {
                    "isComplete": is_complete,
                    "expectedStartIndex": expected_start,
                    "expectedEndIndex": expected_end,
                    "expectedSamples": expected_samples,
                    "actualStartIndex": actual_start,
                    "actualEndIndex": actual_end,
                    "actualSamples": actual_samples,
                    "missingIndices": missing_indices[:100]
                }
                
        except Exception as e:
            self.logger.error(f"验证数据集完整性失败: {e}")
            return {"isComplete": False, "error": str(e)}
    
    def delete_dataset(self, assigned_dataset_id: str) -> bool:
        """删除数据集 - v1.5.1
        
        Args:
            assigned_dataset_id: 数据集ID
            
        Returns:
            bool: 删除是否成功
        """
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                
                # 删除数据行
                cursor.execute("""
                    DELETE FROM dataset_rows WHERE assigned_dataset_id = ?
                """, (assigned_dataset_id,))
                
                # 删除数据集信息
                cursor.execute("""
                    DELETE FROM datasets WHERE assigned_dataset_id = ?
                """, (assigned_dataset_id,))
                
                conn.commit()
                
                if cursor.rowcount > 0:
                    self.logger.info(f"已删除数据集 {assigned_dataset_id}")
                    return True
                return False
                
        except Exception as e:
            self.logger.error(f"删除数据集失败: {e}")
            return False

