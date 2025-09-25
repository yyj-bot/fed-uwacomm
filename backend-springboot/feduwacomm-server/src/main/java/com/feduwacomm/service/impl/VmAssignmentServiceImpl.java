package com.feduwacomm.service.impl;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.entity.User;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.enums.UserRole;
import com.feduwacomm.enums.VmStatus;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.service.VmAssignmentService;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 虚拟机分配服务实现
 * 基于用户角色的权限管理
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VmAssignmentServiceImpl implements VmAssignmentService {

    private final VmInstancesMapper vmInstancesMapper;
    private final UserMapper userMapper;
    private final UuidUtil uuidUtil;

    @Override
    @Transactional
    public VmAssignmentResponseVO assignVmToUser(String vmId, String userId, Set<String> permissions, String assignedBy) {
        log.info("分配虚拟机给用户: vmId={}, userId={}, permissions={}, assignedBy={}", vmId, userId, permissions, assignedBy);

        // 验证虚拟机是否存在
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new RuntimeException("虚拟机不存在: " + vmId);
        }

        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }

        // 基于角色的权限检查
        if (!hasVmAssignmentPermission(user.getRole())) {
            throw new RuntimeException("用户角色无权限使用虚拟机: " + user.getRole().getCode());
        }

        return VmAssignmentResponseVO.builder()
                .vmId(vmId)
                .userId(userId)
                .permissions(permissions)
                .assignedAt(LocalDateTime.now())
                .assignedBy(assignedBy)
                .build();
    }

    @Override
    @Transactional
    public VmUnassignmentResponseVO unassignVmFromUser(String vmId, String userId, String unassignedBy) {
        log.info("撤销用户虚拟机权限: vmId={}, userId={}, unassignedBy={}", vmId, userId, unassignedBy);

        return VmUnassignmentResponseVO.builder()
                .vmId(vmId)
                .userId(userId)
                .unassignedAt(LocalDateTime.now())
                .unassignedBy(unassignedBy)
                .build();
    }

    @Override
    public PageResult<UserVmListVO> getUserAssignedVms(String userId, String status, Integer page, Integer size) {
        log.info("获取用户分配的虚拟机列表: userId={}, status={}, page={}, size={}", userId, status, page, size);

        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }

        // 基于角色获取可用的虚拟机
        List<VmInstance> allVms = vmInstancesMapper.selectAll();
        List<UserVmListVO> userVms = allVms.stream()
                .filter(vm -> canUserAccessVm(user.getRole(), vm))
                .filter(vm -> status == null || status.equals(vm.getStatus().getCode()))
                .map(vm -> UserVmListVO.builder()
                        .vmId(vm.getId())
                        .name(vm.getName())
                        .status(vm.getStatus().getCode())
                        .permissions(getPermissionsByRole(user.getRole()))
                        .assignedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(userVms, (long) userVms.size(), page != null ? (long) page : 1L, size != null ? (long) size : 10L);
    }

    @Override
    public VmAssignmentInfoVO getVmAssignments(String vmId) {
        log.info("获取虚拟机分配信息: vmId={}", vmId);

        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new RuntimeException("虚拟机不存在: " + vmId);
        }

        return VmAssignmentInfoVO.builder()
                .vmId(vmId)
                .vmName(vmInstance.getName())
                .build();
    }

    @Override
    public PageResult<UnassignedVmListVO> getUnassignedVms(String status, Integer page, Integer size) {
        log.info("获取未分配虚拟机列表: status={}, page={}, size={}", status, page, size);

        List<VmInstance> allVms = vmInstancesMapper.selectAll();
        List<UnassignedVmListVO> unassignedVms = allVms.stream()
                .filter(vm -> status == null || status.equals(vm.getStatus().getCode()))
                .filter(vm -> VmStatus.RUNNING.getCode().equals(vm.getStatus().getCode()))
                .map(vm -> UnassignedVmListVO.builder()
                        .vmId(vm.getId())
                        .name(vm.getName())
                        .status(vm.getStatus().getCode())
                        .createdAt(vm.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PageResult.of(unassignedVms, (long) unassignedVms.size(), page != null ? (long) page : 1L, size != null ? (long) size : 10L);
    }

    @Override
    @Transactional
    public VmBatchAssignmentResponseVO batchAssignVms(String userId, List<String> vmIds, Set<String> permissions, String assignedBy) {
        log.info("批量分配虚拟机: userId={}, vmIds={}, permissions={}, assignedBy={}", userId, vmIds, permissions, assignedBy);

        // 验证用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }

        // 基于角色的权限检查
        if (!hasVmAssignmentPermission(user.getRole())) {
            throw new RuntimeException("用户角色无权限使用虚拟机: " + user.getRole().getCode());
        }

        return VmBatchAssignmentResponseVO.builder()
                .userId(userId)
                .totalCount(vmIds.size())
                .successCount(vmIds.size())
                .assignedBy(assignedBy)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public VmBatchUnassignmentResponseVO batchUnassignVms(String userId, List<String> vmIds, String unassignedBy) {
        log.info("批量取消虚拟机分配: userId={}, vmIds={}, unassignedBy={}", userId, vmIds, unassignedBy);

        return VmBatchUnassignmentResponseVO.builder()
                .userId(userId)
                .totalCount(vmIds.size())
                .successCount(vmIds.size())
                .unassignedBy(unassignedBy)
                .unassignedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public VmPermissionUpdateResponseVO updateVmPermissions(String vmId, String userId, Set<String> permissions, String updatedBy) {
        log.info("更新虚拟机权限: vmId={}, userId={}, permissions={}, updatedBy={}", vmId, userId, permissions, updatedBy);

        return VmPermissionUpdateResponseVO.builder()
                .vmId(vmId)
                .userId(userId)
                .newPermissions(permissions)
                .updatedBy(updatedBy)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean hasVmPermission(String vmId, String userId, String permission) {
        log.debug("检查用户虚拟机权限: vmId={}, userId={}, permission={}", vmId, userId, permission);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        return hasVmAssignmentPermission(user.getRole()) &&
               getPermissionsByRole(user.getRole()).contains(permission.toUpperCase());
    }

    @Override
    public Set<String> getUserVmPermissions(String vmId, String userId) {
        log.debug("获取用户虚拟机权限: vmId={}, userId={}", vmId, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptySet();
        }

        return getPermissionsByRole(user.getRole());
    }

    @Override
    public VmAssignmentOverviewVO getVmAssignmentOverview() {
        log.info("获取虚拟机分配概况");

        List<VmInstance> allVms = vmInstancesMapper.selectAll();

        return VmAssignmentOverviewVO.builder()
                .totalVmCount(allVms.size())
                .assignedVmCount((int) allVms.stream().filter(vm -> VmStatus.RUNNING.equals(vm.getStatus())).count())
                .unassignedVmCount((int) allVms.stream().filter(vm -> VmStatus.OFFLINE.equals(vm.getStatus())).count())
                .onlineVmCount((int) allVms.stream().filter(vm -> VmStatus.RUNNING.equals(vm.getStatus())).count())
                .offlineVmCount((int) allVms.stream().filter(vm -> VmStatus.STOPPED.equals(vm.getStatus())).count())
                .build();
    }

    /**
     * 检查用户角色是否有虚拟机分配权限
     */
    private boolean hasVmAssignmentPermission(UserRole role) {
        return UserRole.ADMIN.equals(role) ||
               UserRole.RESEARCHER.equals(role) ||
               UserRole.OPERATOR.equals(role);
    }

    /**
     * 检查用户是否可以访问指定虚拟机
     */
    private boolean canUserAccessVm(UserRole role, VmInstance vm) {
        // 根据角色和VM状态决定访问权限
        return hasVmAssignmentPermission(role) && VmStatus.RUNNING.equals(vm.getStatus());
    }

    /**
     * 根据用户角色获取权限集合
     */
    private Set<String> getPermissionsByRole(UserRole role) {
        switch (role) {
            case ADMIN:
                return Set.of("READ", "WRITE", "EXECUTE", "DELETE");
            case RESEARCHER:
                return Set.of("READ", "WRITE", "EXECUTE");
            case OPERATOR:
                return Set.of("READ", "EXECUTE");
            default:
                return Collections.emptySet();
        }
    }
}