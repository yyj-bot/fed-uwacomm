package com.feduwacomm.controller;

import com.feduwacomm.common.Result;
import com.feduwacomm.common.PageResult;
import com.feduwacomm.dto.VmAssignmentDTO;
import com.feduwacomm.dto.VmBatchAssignmentDTO;
import com.feduwacomm.service.VmAssignmentService;
import com.feduwacomm.vo.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 管理员虚拟机管理控制器
 * 提供虚拟机分配、管理和监控功能
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/admin/vm")
@CrossOrigin(origins = "*")
public class AdminVmController {

    private static final Logger logger = LoggerFactory.getLogger(AdminVmController.class);

    @Autowired
    private VmAssignmentService vmAssignmentService;

    /**
     * 分配虚拟机给用户
     * POST /api/admin/vm/assign
     */
    @PostMapping("/assign")
    public Result<VmAssignmentResponseVO> assignVmToUser(@Valid @RequestBody VmAssignmentDTO assignmentDTO) {
        logger.info("管理员分配虚拟机: vmId={}, userId={}", assignmentDTO.getVmId(), assignmentDTO.getUserId());
        
        String assignedBy = "admin-default"; // 实际应从JWT token获取
        VmAssignmentResponseVO response = vmAssignmentService.assignVmToUser(
                assignmentDTO.getVmId(), 
                assignmentDTO.getUserId(), 
                assignmentDTO.getPermissions(), 
                assignedBy);
        
        return Result.success(response);
    }

    /**
     * 取消虚拟机分配
     * POST /api/admin/vm/unassign
     */
    @PostMapping("/unassign")
    public Result<VmUnassignmentResponseVO> unassignVmFromUser(
            @RequestParam String vmId, @RequestParam String userId) {
        logger.info("管理员取消虚拟机分配: vmId={}, userId={}", vmId, userId);
        
        String unassignedBy = "admin-default";
        VmUnassignmentResponseVO response = vmAssignmentService.unassignVmFromUser(vmId, userId, unassignedBy);
        
        return Result.success(response);
    }

    /**
     * 批量分配虚拟机
     * POST /api/admin/vm/batch-assign
     */
    @PostMapping("/batch-assign")
    public Result<VmBatchAssignmentResponseVO> batchAssignVms(@Valid @RequestBody VmBatchAssignmentDTO batchAssignmentDTO) {
        logger.info("管理员批量分配虚拟机: userId={}, vmCount={}", 
                batchAssignmentDTO.getUserId(), batchAssignmentDTO.getVmIds().size());
        
        String assignedBy = "admin-default";
        VmBatchAssignmentResponseVO response = vmAssignmentService.batchAssignVms(
                batchAssignmentDTO.getUserId(), 
                batchAssignmentDTO.getVmIds(), 
                batchAssignmentDTO.getPermissions(), 
                assignedBy);
        
        return Result.success(response);
    }

    /**
     * 批量取消虚拟机分配
     * POST /api/admin/vm/batch-unassign
     */
    @PostMapping("/batch-unassign")
    public Result<VmBatchUnassignmentResponseVO> batchUnassignVms(
            @RequestParam String userId, @RequestParam List<String> vmIds) {
        logger.info("管理员批量取消虚拟机分配: userId={}, vmCount={}", userId, vmIds.size());
        
        String unassignedBy = "admin-default";
        VmBatchUnassignmentResponseVO response = vmAssignmentService.batchUnassignVms(userId, vmIds, unassignedBy);
        
        return Result.success(response);
    }

    /**
     * 更新用户虚拟机权限
     * PUT /api/admin/vm/permissions
     */
    @PutMapping("/permissions")
    public Result<VmPermissionUpdateResponseVO> updateVmPermissions(
            @RequestParam String vmId, @RequestParam String userId, @RequestParam Set<String> permissions) {
        logger.info("管理员更新虚拟机权限: vmId={}, userId={}, permissions={}", vmId, userId, permissions);
        
        String updatedBy = "admin-default";
        VmPermissionUpdateResponseVO response = vmAssignmentService.updateVmPermissions(vmId, userId, permissions, updatedBy);
        
        return Result.success(response);
    }

    /**
     * 获取用户分配的虚拟机列表
     * GET /api/admin/vm/user/{userId}/vms
     */
    @GetMapping("/user/{userId}/vms")
    public Result<PageResult<UserVmListVO>> getUserAssignedVms(
            @PathVariable String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        logger.debug("管理员查询用户分配的虚拟机: userId={}, status={}, page={}, size={}", userId, status, page, size);
        
        PageResult<UserVmListVO> result = vmAssignmentService.getUserAssignedVms(userId, status, page, size);
        
        return Result.success(result);
    }

    /**
     * 获取虚拟机分配详情
     * GET /api/admin/vm/{vmId}/assignments
     */
    @GetMapping("/{vmId}/assignments")
    public Result<VmAssignmentInfoVO> getVmAssignments(@PathVariable String vmId) {
        logger.debug("管理员查询虚拟机分配详情: vmId={}", vmId);
        
        VmAssignmentInfoVO result = vmAssignmentService.getVmAssignments(vmId);
        
        return Result.success(result);
    }

    /**
     * 获取未分配的虚拟机列表
     * GET /api/admin/vm/unassigned
     */
    @GetMapping("/unassigned")
    public Result<PageResult<UnassignedVmListVO>> getUnassignedVms(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        logger.debug("管理员查询未分配虚拟机: status={}, page={}, size={}", status, page, size);
        
        PageResult<UnassignedVmListVO> result = vmAssignmentService.getUnassignedVms(status, page, size);
        
        return Result.success(result);
    }

    /**
     * 获取虚拟机分配概况
     * GET /api/admin/vm/overview
     */
    @GetMapping("/overview")
    public Result<VmAssignmentOverviewVO> getVmAssignmentOverview() {
        logger.debug("管理员查询虚拟机分配概况");
        
        VmAssignmentOverviewVO overview = vmAssignmentService.getVmAssignmentOverview();
        
        return Result.success(overview);
    }

    /**
     * 检查用户虚拟机权限
     * GET /api/admin/vm/permission/check
     */
    @GetMapping("/permission/check")
    public Result<Boolean> checkVmPermission(
            @RequestParam String vmId, @RequestParam String userId, @RequestParam String permission) {
        logger.debug("管理员检查用户虚拟机权限: vmId={}, userId={}, permission={}", vmId, userId, permission);
        
        boolean hasPermission = vmAssignmentService.hasVmPermission(vmId, userId, permission);
        
        return Result.success(hasPermission);
    }

    /**
     * 获取用户的虚拟机权限列表
     * GET /api/admin/vm/permission/list
     */
    @GetMapping("/permission/list")
    public Result<Set<String>> getUserVmPermissions(@RequestParam String vmId, @RequestParam String userId) {
        logger.debug("管理员查询用户虚拟机权限: vmId={}, userId={}", vmId, userId);
        
        Set<String> permissions = vmAssignmentService.getUserVmPermissions(vmId, userId);
        
        return Result.success(permissions);
    }
}