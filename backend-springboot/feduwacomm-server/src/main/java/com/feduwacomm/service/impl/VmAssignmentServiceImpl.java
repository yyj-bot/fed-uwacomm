package com.feduwacomm.service.impl;

import com.feduwacomm.common.PageResult;
import com.feduwacomm.entity.UserPermission;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.UserPermissionMapper;
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
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VmAssignmentServiceImpl implements VmAssignmentService {

    private final UserPermissionMapper userPermissionMapper;
    private final VmInstancesMapper vmInstancesMapper;
    private final UserMapper userMapper;
    private final UuidUtil uuidUtil;

    private static final String RESOURCE_TYPE_VM = "VM";

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
        if (userMapper.selectById(userId) == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }

        // 检查是否已经分配过
        List<UserPermission> existingPermissions = userPermissionMapper.selectByUserIdAndResourceType(userId, RESOURCE_TYPE_VM);
        boolean alreadyAssigned = existingPermissions.stream()
                .anyMatch(p -> vmId.equals(p.getResourceId()));

        if (alreadyAssigned) {
            throw new RuntimeException("用户已被分配该虚拟机");
        }

        // 创建权限记录
        List<UserPermission> permissionRecords = new ArrayList<>();
        for (String permission : permissions) {
            UserPermission userPermission = UserPermission.builder()
                    .id(uuidUtil.generateUuid())
                    .userId(userId)
                    .resourceType(RESOURCE_TYPE_VM)
                    .resourceId(vmId)
                    .permission(permission.toUpperCase())
                    .grantedAt(LocalDateTime.now())
                    .grantedBy(assignedBy)
                    .build();
            
            userPermissionMapper.insert(userPermission);
            permissionRecords.add(userPermission);
        }

        // 构造响应
        return VmAssignmentResponseVO.builder()
                .assignmentId(permissionRecords.get(0).getId())
                .vmId(vmId)
                .vmName(vmInstance.getName())
                .userId(userId)
                .username(userMapper.selectById(userId).getUsername())
                .permissions(permissions)
                .status("ASSIGNED")
                .assignedAt(LocalDateTime.now())
                .assignedBy(assignedBy)
                .assignedByUsername(userMapper.selectById(assignedBy).getUsername())
                .build();
    }

    @Override
    @Transactional
    public VmUnassignmentResponseVO unassignVmFromUser(String vmId, String userId, String unassignedBy) {
        log.info("取消用户虚拟机分配: vmId={}, userId={}, unassignedBy={}", vmId, userId, unassignedBy);

        // 验证分配关系存在
        List<UserPermission> permissions = userPermissionMapper.selectByUserIdAndResourceType(userId, RESOURCE_TYPE_VM)
                .stream()
                .filter(p -> vmId.equals(p.getResourceId()))
                .collect(Collectors.toList());

        if (permissions.isEmpty()) {
            throw new RuntimeException("用户未被分配该虚拟机");
        }

        // 获取原始分配信息
        UserPermission firstPermission = permissions.get(0);
        LocalDateTime originalAssignedAt = firstPermission.getGrantedAt();

        // 删除所有相关权限
        for (UserPermission permission : permissions) {
            userPermissionMapper.deleteById(permission.getId());
        }

        // 构造响应
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        return VmUnassignmentResponseVO.builder()
                .vmId(vmId)
                .vmName(vmInstance.getName())
                .userId(userId)
                .username(userMapper.selectById(userId).getUsername())
                .status("UNASSIGNED")
                .unassignedAt(LocalDateTime.now())
                .unassignedBy(unassignedBy)
                .unassignedByUsername(userMapper.selectById(unassignedBy).getUsername())
                .originalAssignedAt(originalAssignedAt)
                .build();
    }

    @Override
    public PageResult<UserVmListVO> getUserAssignedVms(String userId, String status, Integer page, Integer size) {
        log.info("获取用户分配的虚拟机列表: userId={}, status={}, page={}, size={}", userId, status, page, size);

        // 获取用户的VM权限
        List<UserPermission> vmPermissions = userPermissionMapper.selectByUserIdAndResourceType(userId, RESOURCE_TYPE_VM);
        
        // 按虚拟机ID分组权限
        Map<String, List<UserPermission>> permissionsByVm = vmPermissions.stream()
                .collect(Collectors.groupingBy(UserPermission::getResourceId));

        // 获取虚拟机详情并构造响应
        List<UserVmListVO> vmList = new ArrayList<>();
        for (String vmId : permissionsByVm.keySet()) {
            VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
            if (vmInstance != null && (status == null || status.equals(vmInstance.getStatus()))) {
                List<UserPermission> permissions = permissionsByVm.get(vmId);
                Set<String> permissionSet = permissions.stream()
                        .map(UserPermission::getPermission)
                        .collect(Collectors.toSet());

                UserPermission firstPermission = permissions.get(0);
                UserVmListVO vo = UserVmListVO.builder()
                        .vmId(vmId)
                        .name(vmInstance.getName())
                        .ipAddress(vmInstance.getIpAddress())
                        .port(vmInstance.getPort())
                        .osType(vmInstance.getOsType())
                        .cpuCores(vmInstance.getCpuCores())
                        .memoryMb(vmInstance.getMemoryMb())
                        .diskGb(vmInstance.getDiskGb())
                        .status(vmInstance.getStatus())
                        .connectionStatus(vmInstance.getConnectionStatus())
                        .permissions(permissionSet)
                        .assignedAt(firstPermission.getGrantedAt())
                        .assignedByUsername(userMapper.selectById(firstPermission.getGrantedBy()).getUsername())
                        .lastHeartbeat(vmInstance.getLastHeartbeat())
                        .build();
                vmList.add(vo);
            }
        }

        // 简单分页
        int offset = (page - 1) * size;
        List<UserVmListVO> pagedList = vmList.stream()
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());

        return PageResult.of(pagedList, (long) vmList.size(), (long) page, (long) size);
    }

    @Override
    public VmAssignmentInfoVO getVmAssignments(String vmId) {
        log.info("获取虚拟机分配信息: vmId={}", vmId);

        // 获取虚拟机基本信息
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new RuntimeException("虚拟机不存在: " + vmId);
        }

        // 获取所有分配给该虚拟机的权限
        List<UserPermission> permissions = userPermissionMapper.selectAll()
                .stream()
                .filter(p -> RESOURCE_TYPE_VM.equals(p.getResourceType()) && vmId.equals(p.getResourceId()))
                .collect(Collectors.toList());

        // 按用户分组权限
        Map<String, List<UserPermission>> permissionsByUser = permissions.stream()
                .collect(Collectors.groupingBy(UserPermission::getUserId));

        // 构造分配用户信息
        List<VmAssignmentInfoVO.AssignedUser> assignedUsers = permissionsByUser.entrySet().stream()
                .map(entry -> {
                    String userId = entry.getKey();
                    List<UserPermission> userPermissions = entry.getValue();
                    
                    Set<String> permissionSet = userPermissions.stream()
                            .map(UserPermission::getPermission)
                            .collect(Collectors.toSet());

                    UserPermission firstPermission = userPermissions.get(0);
                    return VmAssignmentInfoVO.AssignedUser.builder()
                            .userId(userId)
                            .username(userMapper.selectById(userId).getUsername())
                            .email(userMapper.selectById(userId).getEmail())
                            .permissions(permissionSet)
                            .assignedAt(firstPermission.getGrantedAt())
                            .assignedByUsername(userMapper.selectById(firstPermission.getGrantedBy()).getUsername())
                            .build();
                })
                .collect(Collectors.toList());

        // 构造VM基本信息
        VmAssignmentInfoVO.VmBasicInfo vmInfo = VmAssignmentInfoVO.VmBasicInfo.builder()
                .ipAddress(vmInstance.getIpAddress())
                .port(vmInstance.getPort())
                .osType(vmInstance.getOsType())
                .status(vmInstance.getStatus())
                .connectionStatus(vmInstance.getConnectionStatus())
                .lastHeartbeat(vmInstance.getLastHeartbeat())
                .build();

        return VmAssignmentInfoVO.builder()
                .vmId(vmId)
                .vmName(vmInstance.getName())
                .vmInfo(vmInfo)
                .isAssigned(!assignedUsers.isEmpty())
                .assignedUsers(assignedUsers)
                .totalAssignedUsers(assignedUsers.size())
                .createdAt(vmInstance.getCreatedAt())
                .updatedAt(vmInstance.getUpdatedAt())
                .build();
    }

    @Override
    public PageResult<UnassignedVmListVO> getUnassignedVms(String status, Integer page, Integer size) {
        log.info("获取未分配的虚拟机列表: status={}, page={}, size={}", status, page, size);

        // 获取所有虚拟机
        List<VmInstance> allVms = vmInstancesMapper.selectAll();
        
        // 获取所有已分配的虚拟机ID
        Set<String> assignedVmIds = userPermissionMapper.selectAll()
                .stream()
                .filter(p -> RESOURCE_TYPE_VM.equals(p.getResourceType()))
                .map(UserPermission::getResourceId)
                .collect(Collectors.toSet());

        // 过滤未分配的虚拟机
        List<UnassignedVmListVO> unassignedVms = allVms.stream()
                .filter(vm -> !assignedVmIds.contains(vm.getId()))
                .filter(vm -> status == null || status.equals(vm.getStatus()))
                .map(vm -> UnassignedVmListVO.builder()
                        .vmId(vm.getId())
                        .name(vm.getName())
                        .ipAddress(vm.getIpAddress())
                        .port(vm.getPort())
                        .osType(vm.getOsType())
                        .cpuCores(vm.getCpuCores())
                        .memoryMb(vm.getMemoryMb())
                        .diskGb(vm.getDiskGb())
                        .status(vm.getStatus())
                        .connectionStatus(vm.getConnectionStatus())
                        .createdAt(vm.getCreatedAt())
                        .lastHeartbeat(vm.getLastHeartbeat())
                        .systemInfo(vm.getSystemInfo())
                        .available("RUNNING".equals(vm.getStatus()))
                        .build())
                .collect(Collectors.toList());

        // 分页
        int offset = (page - 1) * size;
        List<UnassignedVmListVO> pagedList = unassignedVms.stream()
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());

        return PageResult.of(pagedList, (long) unassignedVms.size(), (long) page, (long) size);
    }

    @Override
    @Transactional
    public VmBatchAssignmentResponseVO batchAssignVms(String userId, List<String> vmIds, Set<String> permissions, String assignedBy) {
        log.info("批量分配虚拟机: userId={}, vmIds={}, permissions={}, assignedBy={}", userId, vmIds, permissions, assignedBy);

        String batchId = uuidUtil.generateUuid();
        List<VmBatchAssignmentResponseVO.BatchAssignmentDetail> details = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (String vmId : vmIds) {
            try {
                assignVmToUser(vmId, userId, permissions, assignedBy);
                
                VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
                details.add(VmBatchAssignmentResponseVO.BatchAssignmentDetail.builder()
                        .vmId(vmId)
                        .vmName(vmInstance.getName())
                        .status("SUCCESS")
                        .processedAt(LocalDateTime.now())
                        .build());
                successCount++;
                
            } catch (Exception e) {
                log.error("批量分配虚拟机失败: vmId={}, error={}", vmId, e.getMessage());
                
                VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
                details.add(VmBatchAssignmentResponseVO.BatchAssignmentDetail.builder()
                        .vmId(vmId)
                        .vmName(vmInstance != null ? vmInstance.getName() : "未知")
                        .status("FAILED")
                        .errorMessage(e.getMessage())
                        .processedAt(LocalDateTime.now())
                        .build());
                failureCount++;
            }
        }

        String overallStatus = failureCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAILED" : "PARTIAL");

        return VmBatchAssignmentResponseVO.builder()
                .batchId(batchId)
                .userId(userId)
                .username(userMapper.selectById(userId).getUsername())
                .permissions(permissions)
                .overallStatus(overallStatus)
                .assignedAt(LocalDateTime.now())
                .assignedBy(assignedBy)
                .assignedByUsername(userMapper.selectById(assignedBy).getUsername())
                .successCount(successCount)
                .failureCount(failureCount)
                .totalCount(vmIds.size())
                .details(details)
                .build();
    }

    @Override
    @Transactional
    public VmBatchUnassignmentResponseVO batchUnassignVms(String userId, List<String> vmIds, String unassignedBy) {
        log.info("批量取消虚拟机分配: userId={}, vmIds={}, unassignedBy={}", userId, vmIds, unassignedBy);

        String batchId = uuidUtil.generateUuid();
        List<VmBatchUnassignmentResponseVO.BatchUnassignmentDetail> details = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (String vmId : vmIds) {
            try {
                VmUnassignmentResponseVO result = unassignVmFromUser(vmId, userId, unassignedBy);
                
                details.add(VmBatchUnassignmentResponseVO.BatchUnassignmentDetail.builder()
                        .vmId(vmId)
                        .vmName(result.getVmName())
                        .status("SUCCESS")
                        .originalAssignedAt(result.getOriginalAssignedAt())
                        .processedAt(LocalDateTime.now())
                        .build());
                successCount++;
                
            } catch (Exception e) {
                log.error("批量取消虚拟机分配失败: vmId={}, error={}", vmId, e.getMessage());
                
                VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
                details.add(VmBatchUnassignmentResponseVO.BatchUnassignmentDetail.builder()
                        .vmId(vmId)
                        .vmName(vmInstance != null ? vmInstance.getName() : "未知")
                        .status("FAILED")
                        .errorMessage(e.getMessage())
                        .processedAt(LocalDateTime.now())
                        .build());
                failureCount++;
            }
        }

        String overallStatus = failureCount == 0 ? "SUCCESS" : (successCount == 0 ? "FAILED" : "PARTIAL");

        return VmBatchUnassignmentResponseVO.builder()
                .batchId(batchId)
                .userId(userId)
                .username(userMapper.selectById(userId).getUsername())
                .overallStatus(overallStatus)
                .unassignedAt(LocalDateTime.now())
                .unassignedBy(unassignedBy)
                .unassignedByUsername(userMapper.selectById(unassignedBy).getUsername())
                .successCount(successCount)
                .failureCount(failureCount)
                .totalCount(vmIds.size())
                .details(details)
                .build();
    }

    @Override
    @Transactional
    public VmPermissionUpdateResponseVO updateVmPermissions(String vmId, String userId, Set<String> permissions, String updatedBy) {
        log.info("更新虚拟机权限: vmId={}, userId={}, permissions={}, updatedBy={}", vmId, userId, permissions, updatedBy);

        // 获取当前权限
        List<UserPermission> currentPermissions = userPermissionMapper.selectByUserIdAndResourceType(userId, RESOURCE_TYPE_VM)
                .stream()
                .filter(p -> vmId.equals(p.getResourceId()))
                .collect(Collectors.toList());

        if (currentPermissions.isEmpty()) {
            throw new RuntimeException("用户未被分配该虚拟机");
        }

        Set<String> oldPermissions = currentPermissions.stream()
                .map(UserPermission::getPermission)
                .collect(Collectors.toSet());

        LocalDateTime originalAssignedAt = currentPermissions.get(0).getGrantedAt();

        // 删除旧权限
        for (UserPermission permission : currentPermissions) {
            userPermissionMapper.deleteById(permission.getId());
        }

        // 添加新权限
        for (String permission : permissions) {
            UserPermission userPermission = UserPermission.builder()
                    .id(uuidUtil.generateUuid())
                    .userId(userId)
                    .resourceType(RESOURCE_TYPE_VM)
                    .resourceId(vmId)
                    .permission(permission.toUpperCase())
                    .grantedAt(LocalDateTime.now())
                    .grantedBy(updatedBy)
                    .build();
            
            userPermissionMapper.insert(userPermission);
        }

        // 构造响应
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        return VmPermissionUpdateResponseVO.builder()
                .vmId(vmId)
                .vmName(vmInstance.getName())
                .userId(userId)
                .username(userMapper.selectById(userId).getUsername())
                .oldPermissions(oldPermissions)
                .newPermissions(permissions)
                .status("UPDATED")
                .updatedAt(LocalDateTime.now())
                .updatedBy(updatedBy)
                .updatedByUsername(userMapper.selectById(updatedBy).getUsername())
                .originalAssignedAt(originalAssignedAt)
                .build();
    }

    @Override
    public boolean hasVmPermission(String vmId, String userId, String permission) {
        log.debug("检查用户虚拟机权限: vmId={}, userId={}, permission={}", vmId, userId, permission);
        
        int count = userPermissionMapper.checkPermission(userId, RESOURCE_TYPE_VM, vmId, permission.toUpperCase());
        return count > 0;
    }

    @Override
    public Set<String> getUserVmPermissions(String vmId, String userId) {
        log.debug("获取用户虚拟机权限: vmId={}, userId={}", vmId, userId);
        
        return userPermissionMapper.selectByUserIdAndResourceType(userId, RESOURCE_TYPE_VM)
                .stream()
                .filter(p -> vmId.equals(p.getResourceId()))
                .map(UserPermission::getPermission)
                .collect(Collectors.toSet());
    }

    @Override
    public VmAssignmentOverviewVO getVmAssignmentOverview() {
        log.info("获取虚拟机分配概况");

        // 获取所有虚拟机
        List<VmInstance> allVms = vmInstancesMapper.selectAll();
        
        // 获取所有VM权限
        List<UserPermission> allVmPermissions = userPermissionMapper.selectAll()
                .stream()
                .filter(p -> RESOURCE_TYPE_VM.equals(p.getResourceType()))
                .collect(Collectors.toList());

        // 计算统计数据
        Set<String> assignedVmIds = allVmPermissions.stream()
                .map(UserPermission::getResourceId)
                .collect(Collectors.toSet());

        Set<String> usersWithVms = allVmPermissions.stream()
                .map(UserPermission::getUserId)
                .collect(Collectors.toSet());

        // 虚拟机状态统计
        Map<String, Integer> vmStatusStats = allVms.stream()
                .collect(Collectors.groupingBy(VmInstance::getStatus, 
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));

        // 权限类型统计
        Map<String, Integer> permissionStats = allVmPermissions.stream()
                .collect(Collectors.groupingBy(UserPermission::getPermission,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)));

        // 最近分配记录
        List<VmAssignmentOverviewVO.RecentAssignment> recentAssignments = allVmPermissions.stream()
                .sorted((a, b) -> b.getGrantedAt().compareTo(a.getGrantedAt()))
                .limit(10)
                .map(p -> {
                    VmInstance vm = vmInstancesMapper.selectByVmId(p.getResourceId());
                    return VmAssignmentOverviewVO.RecentAssignment.builder()
                            .vmId(p.getResourceId())
                            .vmName(vm != null ? vm.getName() : "未知")
                            .userId(p.getUserId())
                            .username(userMapper.selectById(p.getUserId()).getUsername())
                            .assignedByUsername(userMapper.selectById(p.getGrantedBy()).getUsername())
                            .assignedAt(p.getGrantedAt().toString())
                            .operation("ASSIGN")
                            .build();
                })
                .collect(Collectors.toList());

        // 利用率统计
        double averageAssignmentsPerVm = assignedVmIds.isEmpty() ? 0.0 : 
                (double) allVmPermissions.size() / assignedVmIds.size();
        double averageVmsPerUser = usersWithVms.isEmpty() ? 0.0 : 
                (double) assignedVmIds.size() / usersWithVms.size();
        double vmUtilizationRate = allVms.isEmpty() ? 0.0 : 
                (double) assignedVmIds.size() / allVms.size();

        VmAssignmentOverviewVO.VmUtilizationStats utilizationStats = 
                VmAssignmentOverviewVO.VmUtilizationStats.builder()
                        .averageAssignmentsPerVm(averageAssignmentsPerVm)
                        .averageVmsPerUser(averageVmsPerUser)
                        .vmUtilizationRate(vmUtilizationRate)
                        .build();

        return VmAssignmentOverviewVO.builder()
                .totalVmCount(allVms.size())
                .assignedVmCount(assignedVmIds.size())
                .unassignedVmCount(allVms.size() - assignedVmIds.size())
                .onlineVmCount((int) allVms.stream().filter(vm -> "RUNNING".equals(vm.getStatus())).count())
                .offlineVmCount((int) allVms.stream().filter(vm -> !"RUNNING".equals(vm.getStatus())).count())
                .totalUserCount(userMapper.countAll())
                .usersWithVmCount(usersWithVms.size())
                .totalAssignmentCount(allVmPermissions.size())
                .vmStatusStats(vmStatusStats)
                .permissionStats(permissionStats)
                .recentAssignments(recentAssignments)
                .utilizationStats(utilizationStats)
                .build();
    }
}