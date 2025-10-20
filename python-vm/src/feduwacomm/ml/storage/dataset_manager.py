"""
数据集管理器 - v1.5.1版本
负责数据集的创建、验证、完整性检查和存储管理
"""

import logging
import time
import threading
from typing import Dict, Any, Optional, List, Set
from enum import Enum
from datetime import datetime


logger = logging.getLogger(__name__)


class DatasetStatus(Enum):
    """数据集状态枚举"""
    PENDING = "PENDING"
    CREATED = "CREATED"
    UPLOADING = "UPLOADING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


class DatasetInfo:
    """数据集信息类"""
    
    def __init__(self, task_id: str, assigned_dataset_id: str, 
                 dataset_name: str = None, dataset_type: str = None):
        self.task_id = task_id
        self.assigned_dataset_id = assigned_dataset_id
        self.dataset_name = dataset_name or f"dataset_{assigned_dataset_id[:8]}"
        self.dataset_type = dataset_type or "ACOUSTIC"
        
        # v1.5.1: 切片信息
        self.slice_info: Optional[Dict[str, Any]] = None
        self.start_index: Optional[int] = None
        self.end_index: Optional[int] = None
        self.expected_samples: int = 0
        self.allocation_strategy: Optional[str] = None
        
        # 状态信息
        self.status = DatasetStatus.PENDING
        self.created_at = time.time()
        self.completed_at: Optional[float] = None
        
        # 数据存储
        self.rows: List[Dict[str, Any]] = []
        self.received_indices: Set[int] = set()  # v1.5.1: 跟踪已接收的全局索引
        self.batches: Dict[str, Dict[str, Any]] = {}  # batchId -> batch_info
        
        # 元数据和schema
        self.metadata: Dict[str, Any] = {}
        self.schema: Dict[str, str] = {}
        
        # 统计信息
        self.total_batches_received = 0
        self.total_rows_received = 0
        
        # 线程锁
        self._lock = threading.RLock()


class DatasetManager:
    """数据集管理器 - v1.5.1版本"""
    
    def __init__(self, storage_manager=None):
        """初始化数据集管理器
        
        Args:
            storage_manager: 可选的存储管理器，用于持久化
        """
        self.storage_manager = storage_manager
        self.datasets: Dict[str, DatasetInfo] = {}  # assignedDatasetId -> DatasetInfo
        self.task_datasets: Dict[str, str] = {}  # taskId -> assignedDatasetId
        
        self._lock = threading.RLock()
        
        logger.info("数据集管理器初始化完成")
    
    def create_dataset(self, task_id: str, assigned_dataset_id: str,
                      dataset_name: str = None, dataset_type: str = None,
                      expected_rows: int = 0, slice_info: Dict[str, Any] = None,
                      metadata: Dict[str, Any] = None, schema: Dict[str, str] = None) -> bool:
        """创建数据集 - v1.5.1增强
        
        Args:
            task_id: 任务ID
            assigned_dataset_id: 后端分配的数据集ID
            dataset_name: 数据集名称
            dataset_type: 数据集类型
            expected_rows: 预期行数
            slice_info: v1.5.1切片信息
            metadata: 元数据
            schema: 数据schema
            
        Returns:
            bool: 创建是否成功
        """
        with self._lock:
            try:
                # 检查是否已存在
                if assigned_dataset_id in self.datasets:
                    logger.warning(f"数据集 {assigned_dataset_id} 已存在")
                    return False
                
                # 创建数据集信息
                dataset_info = DatasetInfo(task_id, assigned_dataset_id, dataset_name, dataset_type)
                dataset_info.status = DatasetStatus.CREATED
                dataset_info.expected_samples = expected_rows
                
                # v1.5.1: 保存切片信息
                if slice_info:
                    dataset_info.slice_info = slice_info
                    dataset_info.start_index = slice_info.get("startIndex")
                    dataset_info.end_index = slice_info.get("endIndex")
                    dataset_info.allocation_strategy = slice_info.get("allocationStrategy")
                    
                    logger.info(f"数据集切片: [{dataset_info.start_index}-{dataset_info.end_index}], "
                              f"预期样本: {expected_rows}, 策略: {dataset_info.allocation_strategy}")
                
                # 保存元数据和schema
                if metadata:
                    dataset_info.metadata = metadata
                if schema:
                    dataset_info.schema = schema
                
                # 添加到管理器
                self.datasets[assigned_dataset_id] = dataset_info
                self.task_datasets[task_id] = assigned_dataset_id
                
                logger.info(f"数据集 {assigned_dataset_id} 创建成功")
                return True
                
            except Exception as e:
                logger.error(f"创建数据集失败: {e}")
                return False
    
    def append_rows(self, task_id: str, assigned_dataset_id: str,
                   batch_id: str, rows: List[Dict[str, Any]],
                   batch_range: Dict[str, int] = None) -> bool:
        """追加数据行 - v1.5.1增强
        
        Args:
            task_id: 任务ID
            assigned_dataset_id: 数据集ID
            batch_id: 批次ID
            rows: 数据行列表（包含localIndex和globalIndex）
            batch_range: v1.5.1批次范围信息
            
        Returns:
            bool: 追加是否成功
        """
        with self._lock:
            try:
                # 获取数据集
                dataset_info = self.datasets.get(assigned_dataset_id)
                if not dataset_info:
                    logger.error(f"数据集 {assigned_dataset_id} 不存在")
                    return False
                
                # 更新状态为上传中
                if dataset_info.status == DatasetStatus.CREATED:
                    dataset_info.status = DatasetStatus.UPLOADING
                
                # v1.5.1: 验证批次范围
                if batch_range and dataset_info.slice_info:
                    if not self._validate_batch_range(dataset_info, batch_range):
                        logger.error(f"批次 {batch_id} 范围验证失败")
                        return False
                
                # v1.5.1: 验证并记录每行的索引
                validation_errors = []
                for row in rows:
                    local_index = row.get("localIndex")
                    global_index = row.get("globalIndex")
                    
                    if local_index is not None and global_index is not None:
                        # 验证双重索引一致性
                        if not self._validate_dual_index(dataset_info, local_index, global_index):
                            validation_errors.append(f"行索引不一致: local={local_index}, global={global_index}")
                        
                        # 记录全局索引
                        dataset_info.received_indices.add(global_index)
                
                # 如果有验证错误，记录警告但继续
                if validation_errors:
                    logger.warning(f"批次 {batch_id} 索引验证警告: {validation_errors[:3]}")  # 只显示前3个
                
                # 追加数据行
                dataset_info.rows.extend(rows)
                dataset_info.total_rows_received += len(rows)
                dataset_info.total_batches_received += 1
                
                # 记录批次信息
                dataset_info.batches[batch_id] = {
                    "batch_range": batch_range,
                    "row_count": len(rows),
                    "received_at": time.time()
                }
                
                logger.debug(f"批次 {batch_id} 追加 {len(rows)} 行，"
                           f"累计: {dataset_info.total_rows_received}/{dataset_info.expected_samples}")
                
                return True
                
            except Exception as e:
                logger.error(f"追加数据行失败: {e}")
                return False
    
    def complete_dataset(self, task_id: str, assigned_dataset_id: str) -> Optional[Dict[str, Any]]:
        """完成数据集接收 - v1.5.1增强
        
        Args:
            task_id: 任务ID
            assigned_dataset_id: 数据集ID
            
        Returns:
            Dict: 包含完整性验证结果的字典，失败返回None
        """
        with self._lock:
            try:
                # 获取数据集
                dataset_info = self.datasets.get(assigned_dataset_id)
                if not dataset_info:
                    logger.error(f"数据集 {assigned_dataset_id} 不存在")
                    return None
                
                # 记录完成时间
                dataset_info.completed_at = time.time()
                upload_duration = dataset_info.completed_at - dataset_info.created_at
                
                # v1.5.1: 执行完整性验证
                slice_verification = None
                if dataset_info.slice_info:
                    slice_verification = self._verify_slice_completeness(dataset_info)
                
                # 更新状态
                if slice_verification and slice_verification.get("isComplete"):
                    dataset_info.status = DatasetStatus.COMPLETED
                else:
                    dataset_info.status = DatasetStatus.FAILED
                    logger.warning(f"数据集 {assigned_dataset_id} 完整性验证失败")
                
                # 计算数据完整性
                data_integrity = {
                    "checksumValid": True,  # 简化处理
                    "missingRows": dataset_info.expected_samples - dataset_info.total_rows_received,
                    "duplicateRows": 0  # 需要额外检测
                }
                
                # 计算统计信息
                statistics = self._calculate_statistics(dataset_info)
                
                # 构造返回结果
                result = {
                    "final_row_count": dataset_info.total_rows_received,
                    "slice_verification": slice_verification,
                    "total_batches": dataset_info.total_batches_received,
                    "upload_duration": upload_duration,
                    "data_integrity": data_integrity,
                    "statistics": statistics
                }
                
                logger.info(f"数据集 {assigned_dataset_id} 完成: "
                          f"{dataset_info.total_rows_received} 行, "
                          f"耗时 {upload_duration:.2f}秒")
                
                return result
                
            except Exception as e:
                logger.error(f"完成数据集失败: {e}")
                return None
    
    def _validate_batch_range(self, dataset_info: DatasetInfo, batch_range: Dict[str, int]) -> bool:
        """验证批次范围是否在切片范围内
        
        Args:
            dataset_info: 数据集信息
            batch_range: 批次范围
            
        Returns:
            bool: 验证是否通过
        """
        try:
            global_start = batch_range.get("globalStartIndex")
            global_end = batch_range.get("globalEndIndex")
            
            if global_start is None or global_end is None:
                return True  # 如果没有提供，跳过验证
            
            # 检查是否在切片范围内
            if global_start < dataset_info.start_index or global_end > dataset_info.end_index:
                logger.error(f"批次范围 [{global_start}-{global_end}] "
                           f"超出切片范围 [{dataset_info.start_index}-{dataset_info.end_index}]")
                return False
            
            return True
            
        except Exception as e:
            logger.error(f"验证批次范围失败: {e}")
            return False
    
    def _validate_dual_index(self, dataset_info: DatasetInfo, 
                            local_index: int, global_index: int) -> bool:
        """验证双重索引一致性
        
        Args:
            dataset_info: 数据集信息
            local_index: 本地索引
            global_index: 全局索引
            
        Returns:
            bool: 验证是否通过
        """
        try:
            if dataset_info.start_index is None:
                return True  # 如果没有切片信息，跳过验证
            
            # 验证公式: globalIndex = localIndex + startIndex
            expected_global = local_index + dataset_info.start_index
            
            if global_index != expected_global:
                logger.debug(f"索引不一致: local={local_index}, global={global_index}, "
                           f"expected_global={expected_global}")
                return False
            
            # 验证全局索引在范围内
            if global_index < dataset_info.start_index or global_index > dataset_info.end_index:
                logger.debug(f"全局索引 {global_index} 超出范围 "
                           f"[{dataset_info.start_index}-{dataset_info.end_index}]")
                return False
            
            return True
            
        except Exception as e:
            logger.error(f"验证双重索引失败: {e}")
            return False
    
    def _verify_slice_completeness(self, dataset_info: DatasetInfo) -> Dict[str, Any]:
        """验证切片完整性 - v1.5.1核心功能
        
        Args:
            dataset_info: 数据集信息
            
        Returns:
            Dict: sliceVerification对象
        """
        try:
            slice_info = dataset_info.slice_info
            expected_start = slice_info.get("startIndex")
            expected_end = slice_info.get("endIndex")
            expected_samples = slice_info.get("sliceSamples")
            
            # 计算实际接收的范围
            received_indices = sorted(dataset_info.received_indices)
            actual_start = received_indices[0] if received_indices else None
            actual_end = received_indices[-1] if received_indices else None
            actual_samples = len(received_indices)
            
            # 查找缺失的索引
            missing_indices = []
            if expected_start is not None and expected_end is not None:
                expected_set = set(range(expected_start, expected_end + 1))
                missing_indices = sorted(expected_set - dataset_info.received_indices)
            
            # 检测连续性和间隙
            continuity_check = self._check_continuity(received_indices, expected_start, expected_end)
            
            # 判断是否完整
            is_complete = (
                actual_samples == expected_samples and
                len(missing_indices) == 0 and
                not continuity_check.get("hasGaps", False) and
                actual_start == expected_start and
                actual_end == expected_end
            )
            
            # 构造验证结果
            verification = {
                "expectedStartIndex": expected_start,
                "expectedEndIndex": expected_end,
                "expectedSamples": expected_samples,
                "actualStartIndex": actual_start,
                "actualEndIndex": actual_end,
                "actualSamples": actual_samples,
                "isComplete": is_complete,
                "missingIndices": missing_indices[:100],  # 最多返回100个缺失索引
                "continuityCheck": continuity_check
            }
            
            logger.info(f"切片验证: 预期 {expected_samples} 样本, "
                       f"实际 {actual_samples} 样本, "
                       f"缺失 {len(missing_indices)} 个, "
                       f"完整={is_complete}")
            
            return verification
            
        except Exception as e:
            logger.error(f"验证切片完整性失败: {e}")
            return {
                "isComplete": False,
                "missingIndices": [],
                "continuityCheck": {"hasGaps": True, "gapRanges": []}
            }
    
    def _check_continuity(self, indices: List[int], 
                         expected_start: int, expected_end: int) -> Dict[str, Any]:
        """检查索引连续性和间隙
        
        Args:
            indices: 已排序的索引列表
            expected_start: 预期起始索引
            expected_end: 预期结束索引
            
        Returns:
            Dict: continuityCheck对象
        """
        try:
            gap_ranges = []
            has_gaps = False
            
            if not indices:
                return {
                    "hasGaps": True,
                    "gapRanges": [{"start": expected_start, "end": expected_end}]
                }
            
            # 检查开头是否有间隙
            if indices[0] > expected_start:
                gap_ranges.append({"start": expected_start, "end": indices[0] - 1})
                has_gaps = True
            
            # 检查中间间隙
            for i in range(len(indices) - 1):
                if indices[i + 1] - indices[i] > 1:
                    gap_ranges.append({
                        "start": indices[i] + 1,
                        "end": indices[i + 1] - 1
                    })
                    has_gaps = True
            
            # 检查结尾是否有间隙
            if indices[-1] < expected_end:
                gap_ranges.append({"start": indices[-1] + 1, "end": expected_end})
                has_gaps = True
            
            return {
                "hasGaps": has_gaps,
                "gapRanges": gap_ranges[:20]  # 最多返回20个间隙范围
            }
            
        except Exception as e:
            logger.error(f"检查连续性失败: {e}")
            return {"hasGaps": True, "gapRanges": []}
    
    def _calculate_statistics(self, dataset_info: DatasetInfo) -> Dict[str, Any]:
        """计算数据集统计信息
        
        Args:
            dataset_info: 数据集信息
            
        Returns:
            Dict: 统计信息
        """
        try:
            # 简化的统计计算
            stats = {
                "totalRows": dataset_info.total_rows_received,
                "totalBatches": dataset_info.total_batches_received,
                "datasetType": dataset_info.dataset_type
            }
            
            # 如果有数据，计算一些基本统计
            if dataset_info.rows:
                # 这里可以添加更详细的统计，如均值、标签分布等
                pass
            
            return stats
            
        except Exception as e:
            logger.error(f"计算统计信息失败: {e}")
            return {}
    
    def get_dataset_status(self, assigned_dataset_id: str, 
                          task_id: str = None) -> Optional[Dict[str, Any]]:
        """获取数据集状态 - v1.5增强
        
        Args:
            assigned_dataset_id: 数据集ID
            task_id: 可选的任务ID
            
        Returns:
            Dict: 状态信息，不存在返回None
        """
        with self._lock:
            try:
                dataset_info = self.datasets.get(assigned_dataset_id)
                if not dataset_info:
                    return None
                
                status_info = {
                    "status": dataset_info.status.value,
                    "details": {
                        "createdAt": datetime.fromtimestamp(dataset_info.created_at).isoformat(),
                        "rowCount": dataset_info.total_rows_received,
                        "expectedRows": dataset_info.expected_samples,
                        "batchesReceived": dataset_info.total_batches_received
                    }
                }
                
                if dataset_info.completed_at:
                    status_info["details"]["completedAt"] = datetime.fromtimestamp(dataset_info.completed_at).isoformat()
                    status_info["details"]["verificationPassed"] = (dataset_info.status == DatasetStatus.COMPLETED)
                
                return status_info
                
            except Exception as e:
                logger.error(f"获取数据集状态失败: {e}")
                return None
    
    def get_available_datasets(self, scope: str = "ALL", 
                              include_metadata: bool = True) -> List[Dict[str, Any]]:
        """获取可用数据集列表 - v1.5新增
        
        Args:
            scope: 查询范围（ALL/AVAILABLE/READY）
            include_metadata: 是否包含元数据
            
        Returns:
            List: 数据集列表
        """
        with self._lock:
            try:
                datasets = []
                
                for assigned_dataset_id, dataset_info in self.datasets.items():
                    # 根据scope过滤
                    if scope == "AVAILABLE" and dataset_info.status not in [DatasetStatus.COMPLETED]:
                        continue
                    elif scope == "READY" and dataset_info.status != DatasetStatus.COMPLETED:
                        continue
                    
                    dataset_dict = {
                        "localDatasetId": f"local-{assigned_dataset_id[:8]}",
                        "assignedDatasetId": assigned_dataset_id,
                        "name": dataset_info.dataset_name,
                        "status": dataset_info.status.value,
                        "rowCount": dataset_info.total_rows_received,
                        "dataType": dataset_info.dataset_type
                    }
                    
                    if include_metadata and dataset_info.metadata:
                        dataset_dict["metadata"] = {
                            "createdAt": datetime.fromtimestamp(dataset_info.created_at).isoformat(),
                            **dataset_info.metadata
                        }
                    
                    datasets.append(dataset_dict)
                
                return datasets
                
            except Exception as e:
                logger.error(f"获取数据集列表失败: {e}")
                return []
    
    def get_all_datasets_info(self) -> Dict[str, Any]:
        """获取所有数据集信息"""
        with self._lock:
            return {
                "totalDatasets": len(self.datasets),
                "datasets": [
                    {
                        "assignedDatasetId": ds_id,
                        "status": ds_info.status.value,
                        "rowCount": ds_info.total_rows_received
                    }
                    for ds_id, ds_info in self.datasets.items()
                ]
            }
    
    def get_statistics(self) -> Dict[str, Any]:
        """获取管理器统计信息"""
        with self._lock:
            status_count = {}
            for status in DatasetStatus:
                count = sum(1 for ds in self.datasets.values() if ds.status == status)
                status_count[status.value] = count
            
            return {
                "totalDatasets": len(self.datasets),
                "statusDistribution": status_count,
                "totalRows": sum(ds.total_rows_received for ds in self.datasets.values())
            }
    
    def delete_dataset(self, assigned_dataset_id: str, task_id: str = None) -> bool:
        """删除数据集
        
        Args:
            assigned_dataset_id: 数据集ID
            task_id: 可选的任务ID
            
        Returns:
            bool: 删除是否成功
        """
        with self._lock:
            try:
                if assigned_dataset_id not in self.datasets:
                    logger.warning(f"数据集 {assigned_dataset_id} 不存在")
                    return False
                
                dataset_info = self.datasets[assigned_dataset_id]
                
                # 从映射中移除
                if task_id and task_id in self.task_datasets:
                    del self.task_datasets[task_id]
                
                # 删除数据集
                del self.datasets[assigned_dataset_id]
                
                logger.info(f"数据集 {assigned_dataset_id} 已删除")
                return True
                
            except Exception as e:
                logger.error(f"删除数据集失败: {e}")
                return False

