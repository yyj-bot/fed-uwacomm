package com.feduwacomm.service;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.VmAssignmentDTO;
import com.feduwacomm.dto.VmBatchAssignmentDTO;
import com.feduwacomm.vo.*;

import java.util.List;
import java.util.Set;

/**
 * 虚拟机分配服务接口
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
public interface VmAssignmentService {

    /**
     * 分配虚拟机给用户
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @param permissions 权限集合
     * @param assignedBy 分配者ID
     * @return 分配结果
     */
    VmAssignmentResponseVO assignVmToUser(String vmId, String userId, Set<String> permissions, String assignedBy);

    /**
     * 取消虚拟机分配
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @param unassignedBy 取消分配者ID
     * @return 取消分配结果
     */
    VmUnassignmentResponseVO unassignVmFromUser(String vmId, String userId, String unassignedBy);

    /**
     * 获取用户被分配的虚拟机列表
     *
     * @param userId 用户ID
     * @param status 状态过滤（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 分页的用户虚拟机列表
     */
    PageResult<UserVmListVO> getUserAssignedVms(String userId, String status, Integer page, Integer size);

    /**
     * 获取虚拟机的分配情况
     *
     * @param vmId 虚拟机ID
     * @return 虚拟机分配信息
     */
    VmAssignmentInfoVO getVmAssignments(String vmId);

    /**
     * 获取未分配的虚拟机列表
     *
     * @param status 虚拟机状态过滤（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 未分配虚拟机列表
     */
    PageResult<UnassignedVmListVO> getUnassignedVms(String status, Integer page, Integer size);

    /**
     * 批量分配虚拟机
     *
     * @param userId 用户ID
     * @param vmIds 虚拟机ID列表
     * @param permissions 权限集合
     * @param assignedBy 分配者ID
     * @return 批量分配结果
     */
    VmBatchAssignmentResponseVO batchAssignVms(String userId, List<String> vmIds, Set<String> permissions, String assignedBy);

    /**
     * 批量取消虚拟机分配
     *
     * @param userId 用户ID
     * @param vmIds 虚拟机ID列表
     * @param unassignedBy 取消分配者ID
     * @return 批量取消分配结果
     */
    VmBatchUnassignmentResponseVO batchUnassignVms(String userId, List<String> vmIds, String unassignedBy);

    /**
     * 更新用户对虚拟机的权限
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @param permissions 新权限集合
     * @param updatedBy 更新者ID
     * @return 权限更新结果
     */
    VmPermissionUpdateResponseVO updateVmPermissions(String vmId, String userId, Set<String> permissions, String updatedBy);

    /**
     * 检查用户是否对虚拟机有指定权限
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @param permission 权限类型
     * @return 是否有权限
     */
    boolean hasVmPermission(String vmId, String userId, String permission);

    /**
     * 获取用户对虚拟机的权限列表
     *
     * @param vmId 虚拟机ID
     * @param userId 用户ID
     * @return 权限列表
     */
    Set<String> getUserVmPermissions(String vmId, String userId);

    /**
     * 获取虚拟机分配概况
     *
     * @return 虚拟机分配概况统计
     */
    VmAssignmentOverviewVO getVmAssignmentOverview();
}