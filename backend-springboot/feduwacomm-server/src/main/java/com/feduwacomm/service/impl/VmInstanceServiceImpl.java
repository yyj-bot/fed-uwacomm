package com.feduwacomm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.PageResult;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.VmJwtUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.utils.ApiKeyUtil;
import com.feduwacomm.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 虚拟机实例服务实现类
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Service
@Transactional
public class VmInstanceServiceImpl implements VmInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(VmInstanceServiceImpl.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private VmInstancesMapper vmInstancesMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VmJwtUtil vmJwtUtil;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${jwt.expire-seconds:86400}")
    private Long tokenExpireSeconds;

    @Value("${vm.secret.expire-days:30}")
    private int secretExpireDays;

    @Override
    public VmRegisterResponseVO register(VmRegisterDTO registerDTO) {
        logger.info("开始注册虚拟机: vmId={}, name={}, ip={}", 
                   registerDTO.getVmId(), registerDTO.getName(), registerDTO.getIpAddress());

        // 1. 检查虚拟机是否已存在
        if (vmInstancesMapper.existsByVmId(registerDTO.getVmId()) > 0) {
            logger.warn("虚拟机已存在: vmId={}", registerDTO.getVmId());
            throw new BusinessException("虚拟机已存在");
        }

        // 2. 生成API Key和会话ID
        String rawApiKey = ApiKeyUtil.generateApiKey(); // 明文API Key，只返回一次
        String hashedApiKey = ApiKeyUtil.encodeApiKey(rawApiKey); // BCrypt哈希后存储
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 3. 创建虚拟机实例
        VmInstance vmInstance = new VmInstance();
        vmInstance.setId(registerDTO.getVmId());
        vmInstance.setName(registerDTO.getName());
        vmInstance.setIpAddress(registerDTO.getIpAddress());
        vmInstance.setPort(registerDTO.getPort());
        vmInstance.setOsType(registerDTO.getOsType());
        vmInstance.setCpuCores(registerDTO.getCpuCores());
        vmInstance.setMemoryMb(registerDTO.getMemoryMb());
        vmInstance.setDiskGb(registerDTO.getDiskGb());
        vmInstance.setStatus("OFFLINE");
        vmInstance.setConnectionStatus("DISCONNECTED");
        vmInstance.setWsSessionId(sessionId);
        // 存储BCrypt哈希后的API Key到secretId字段
        vmInstance.setSecretId(hashedApiKey);
        vmInstance.setSecretExpireTime(null); // API Key无期限
        vmInstance.setCreatedAt(LocalDateTime.now());
        vmInstance.setUpdatedAt(LocalDateTime.now());

        // 4. 转换JSON字段
        try {
            if (registerDTO.getSystemInfo() != null) {
                vmInstance.setSystemInfo(objectMapper.writeValueAsString(registerDTO.getSystemInfo()));
            }
            if (registerDTO.getCapabilities() != null) {
                vmInstance.setCapabilities(objectMapper.writeValueAsString(registerDTO.getCapabilities()));
            }
            if (registerDTO.getNetworkConfig() != null) {
                vmInstance.setNetworkConfig(objectMapper.writeValueAsString(registerDTO.getNetworkConfig()));
            }
            if (registerDTO.getMetadata() != null) {
                vmInstance.setMetadata(objectMapper.writeValueAsString(registerDTO.getMetadata()));
            }
        } catch (JsonProcessingException e) {
            logger.error("JSON序列化失败: vmId={}", registerDTO.getVmId(), e);
            throw new BusinessException("数据格式错误");
        }

        // 5. 插入数据库
        int result = vmInstancesMapper.insert(vmInstance);
        if (result <= 0) {
            logger.error("虚拟机注册失败: vmId={}", registerDTO.getVmId());
            throw new BusinessException("虚拟机注册失败");
        }

        // 6. 生成访问令牌（在数据库插入成功后）
        String accessToken = generateAccessToken(registerDTO.getVmId());

        logger.info("虚拟机注册成功: vmId={}, sessionId={}", registerDTO.getVmId(), sessionId);

        // 7. 构建响应
        return VmRegisterResponseVO.builder()
                .vmId(registerDTO.getVmId())
                .name(registerDTO.getName())
                .status("OFFLINE")
                .connectionStatus("DISCONNECTED")
                .createdAt(vmInstance.getCreatedAt())
                .sessionId(sessionId)
                .accessToken(accessToken) // JWT令牌只返回给客户端，不存储
                .secretId(rawApiKey) // 返回明文API Key，仅此一次
                .tokenExpireSeconds(tokenExpireSeconds)
                .websocket(VmRegisterResponseVO.WebSocketInfo.builder()
                        .sockjs("http://localhost:" + serverPort + "/ws")
                        .nativeWs("ws://localhost:" + serverPort + "/ws-native")
                        .build())
                .apiEndpoints(VmRegisterResponseVO.ApiEndpoints.builder()
                        .status("/api/v1/vm/" + registerDTO.getVmId() + "/status")
                        .control("/api/v1/vm/" + registerDTO.getVmId() + "/control")
                        .tokenRefresh("/api/v1/vm/token/refresh")
                        .build())
                .build();
    }

    @Override
    public VmTokenRefreshResponseVO refreshToken(VmTokenRefreshDTO refreshDTO) {
        logger.info("开始刷新Token: vmId={}", refreshDTO.getVmId());

        // 1. 先根据vmId查询虚拟机实例
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(refreshDTO.getVmId());
        if (vmInstance == null) {
            logger.warn("虚拟机不存在: vmId={}", refreshDTO.getVmId());
            throw new BusinessException("虚拟机不存在");
        }

        // 2. 验证API Key格式
        if (!ApiKeyUtil.isValidFormat(refreshDTO.getSecretId())) {
            logger.warn("API Key格式错误: vmId={}", refreshDTO.getVmId());
            throw new BusinessException("API Key格式错误");
        }

        // 3. 验证API Key是否匹配
        if (vmInstance.getSecretId() == null || !ApiKeyUtil.matches(refreshDTO.getSecretId(), vmInstance.getSecretId())) {
            logger.warn("API Key验证失败: vmId={}", refreshDTO.getVmId());
            throw new BusinessException("API Key验证失败");
        }

        // 4. 检查API Key是否过期（如果设置了过期时间）
        if (vmInstance.getSecretExpireTime() != null && vmInstance.getSecretExpireTime().isBefore(LocalDateTime.now())) {
            logger.warn("API Key已过期: vmId={}", refreshDTO.getVmId());
            throw new BusinessException("API Key已过期");
        }

        // 5. 生成新的访问令牌
        String newAccessToken = generateAccessToken(refreshDTO.getVmId());

        logger.info("Token刷新成功: vmId={}", refreshDTO.getVmId());

        // 6. 返回新的访问令牌，API Key保持不变
        return VmTokenRefreshResponseVO.builder()
                .accessToken(newAccessToken)
                .tokenExpireSeconds(tokenExpireSeconds)
                .secretId(refreshDTO.getSecretId()) // 返回原始API Key，保持不变
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String validateAccessToken(String accessToken) {
        try {
            // JWT令牌验证（自包含，不需要数据库查询）
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor("feduwacomm_jwt_secret_key_2024_default_256_bit_length_for_security".getBytes()))
                    .build()
                    .parseClaimsJws(accessToken)
                    .getBody();

            return claims.get("vmId", String.class);
        } catch (Exception e) {
            logger.error("令牌验证失败: token={}", accessToken, e);
            return null;
        }
    }

    @Override
    public void updateConnectionStatus(String vmId, String connectionStatus, String wsSessionId) {
        logger.debug("更新虚拟机连接状态: vmId={}, status={}, sessionId={}", 
                    vmId, connectionStatus, wsSessionId);

        int result = vmInstancesMapper.updateWebSocketSession(vmId, wsSessionId, connectionStatus);
        if (result <= 0) {
            logger.warn("更新连接状态失败: vmId={}", vmId);
        }
    }

    @Override
    public void updateHeartbeat(String vmId) {
        logger.debug("更新虚拟机心跳: vmId={}", vmId);

        String currentTime = LocalDateTime.now().toString();
        int result = vmInstancesMapper.updateConnection(vmId, "CONNECTED", currentTime);
        if (result <= 0) {
            logger.warn("更新心跳失败: vmId={}", vmId);
        }
    }

    @Override
    public void disconnect(String vmId) {
        logger.info("虚拟机断开连接: vmId={}", vmId);

        int result = vmInstancesMapper.updateWebSocketSession(vmId, null, "DISCONNECTED");
        if (result <= 0) {
            logger.warn("断开连接状态更新失败: vmId={}", vmId);
        }
    }

    // ==================== 用户端CRUD接口实现 ====================

    @Override
    @Transactional(readOnly = true)
    public PageResult<VmListVO> queryVmList(VmQueryDTO queryDTO) {
        logger.info("查询虚拟机列表: page={}, size={}, userId={}", 
                   queryDTO.getPage(), queryDTO.getSize(), queryDTO.getUserId());

        // 1. 参数校验
        if (queryDTO.getPage() < 1) queryDTO.setPage(1);
        if (queryDTO.getSize() < 1 || queryDTO.getSize() > 100) queryDTO.setSize(20);

        // 2. 计算偏移量
        int offset = (queryDTO.getPage() - 1) * queryDTO.getSize();

        try {
            // 3. 查询总数
            int total = vmInstancesMapper.countVmInstances(
                queryDTO.getStatus(),
                queryDTO.getOsType(), 
                queryDTO.getKeyword(),
                queryDTO.getConnectionStatus()
            );

            if (total == 0) {
                return PageResult.<VmListVO>builder()
                    .current(queryDTO.getPage().longValue())
                    .size(queryDTO.getSize().longValue())
                    .total(0L)
                    .pages(0L)
                    .records(Collections.emptyList())
                    .build();
            }

            // 4. 查询数据列表
            List<VmInstance> vmInstances = vmInstancesMapper.selectPagedList(
                offset,
                queryDTO.getSize(),
                queryDTO.getStatus(),
                queryDTO.getOsType(),
                queryDTO.getKeyword(),
                queryDTO.getConnectionStatus(),
                queryDTO.getSortField(),
                queryDTO.getSortOrder()
            );

            // 5. 转换为VO
            List<VmListVO> voList = vmInstances.stream()
                .map(this::convertToVmListVO)
                .collect(Collectors.toList());

            // 6. 计算总页数
            long pages = (long) Math.ceil((double) total / queryDTO.getSize());

            return PageResult.<VmListVO>builder()
                .current(queryDTO.getPage().longValue())
                .size(queryDTO.getSize().longValue())
                .total((long) total)
                .pages(pages)
                .records(voList)
                .build();

        } catch (Exception e) {
            logger.error("查询虚拟机列表失败: userId={}", queryDTO.getUserId(), e);
            throw new BusinessException("查询虚拟机列表失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public VmDetailVO getVmDetail(String vmId, String userId) {
        logger.info("查询虚拟机详情: vmId={}, userId={}", vmId, userId);

        // 1. 权限检查
        if (!hasVmPermission(vmId, userId)) {
            throw new BusinessException("无权限访问该虚拟机");
        }

        // 2. 查询虚拟机详情
        VmInstance vmInstance = vmInstancesMapper.selectDetailById(vmId);
        if (vmInstance == null) {
            throw new BusinessException("虚拟机不存在: " + vmId);
        }

        // 3. 转换为VO
        return convertToVmDetailVO(vmInstance);
    }

    @Override
    @Transactional
    public VmUpdateResponseVO updateVm(String vmId, VmUpdateDTO updateDTO, String userId) {
        logger.info("更新虚拟机: vmId={}, userId={}", vmId, userId);

        // 1. 权限检查
        if (!hasVmPermission(vmId, userId)) {
            throw new BusinessException("无权限操作该虚拟机");
        }

        // 2. 查询现有虚拟机
        VmInstance existingVm = vmInstancesMapper.selectByVmId(vmId);
        if (existingVm == null) {
            throw new BusinessException("虚拟机不存在: " + vmId);
        }

        try {
            // 3. 构建更新对象
            VmInstance updateVm = VmInstance.builder()
                .id(vmId)
                .updatedAt(LocalDateTime.now())
                .updatedBy(userId)
                .build();

            List<String> updatedFields = new ArrayList<>();

            // 4. 设置更新字段
            if (updateDTO.getName() != null) {
                updateVm.setName(updateDTO.getName());
                updatedFields.add("name");
            }
            if (updateDTO.getIpAddress() != null) {
                updateVm.setIpAddress(updateDTO.getIpAddress());
                updatedFields.add("ipAddress");
            }
            if (updateDTO.getPort() != null) {
                updateVm.setPort(updateDTO.getPort());
                updatedFields.add("port");
            }
            if (updateDTO.getOsType() != null) {
                updateVm.setOsType(updateDTO.getOsType());
                updatedFields.add("osType");
            }
            if (updateDTO.getCpuCores() != null) {
                updateVm.setCpuCores(updateDTO.getCpuCores());
                updatedFields.add("cpuCores");
            }
            if (updateDTO.getMemoryMb() != null) {
                updateVm.setMemoryMb(updateDTO.getMemoryMb());
                updatedFields.add("memoryMb");
            }
            if (updateDTO.getDiskGb() != null) {
                updateVm.setDiskGb(updateDTO.getDiskGb());
                updatedFields.add("diskGb");
            }

            // 5. 处理JSON字段
            if (updateDTO.getSystemInfo() != null) {
                updateVm.setSystemInfo(objectMapper.writeValueAsString(updateDTO.getSystemInfo()));
                updatedFields.add("systemInfo");
            }
            if (updateDTO.getCapabilities() != null) {
                updateVm.setCapabilities(objectMapper.writeValueAsString(updateDTO.getCapabilities()));
                updatedFields.add("capabilities");
            }
            if (updateDTO.getNetworkConfig() != null) {
                updateVm.setNetworkConfig(objectMapper.writeValueAsString(updateDTO.getNetworkConfig()));
                updatedFields.add("networkConfig");
            }
            if (updateDTO.getMetadata() != null) {
                updateVm.setMetadata(objectMapper.writeValueAsString(updateDTO.getMetadata()));
                updatedFields.add("metadata");
            }

            // 6. 执行更新
            int result = vmInstancesMapper.updateBasicInfo(updateVm);
            if (result <= 0) {
                throw new BusinessException("更新虚拟机失败");
            }

            logger.info("虚拟机更新成功: vmId={}, updatedFields={}", vmId, updatedFields);

            // 7. 构建响应
            return VmUpdateResponseVO.builder()
                .vmId(vmId)
                .name(updateDTO.getName() != null ? updateDTO.getName() : existingVm.getName())
                .updatedAt(LocalDateTime.now())
                .updatedFields(updatedFields.toArray(new String[0]))
                .requiresRestart(isRestartRequired(updatedFields))
                .message("虚拟机信息更新成功")
                .build();

        } catch (JsonProcessingException e) {
            logger.error("JSON序列化失败: vmId={}", vmId, e);
            throw new BusinessException("数据格式错误");
        } catch (Exception e) {
            logger.error("更新虚拟机失败: vmId={}", vmId, e);
            throw new BusinessException("更新虚拟机失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public VmDeleteResponseVO deleteVm(String vmId, Boolean force, String userId) {
        logger.info("删除虚拟机: vmId={}, force={}, userId={}", vmId, force, userId);

        // 1. 权限检查
        if (!hasVmPermission(vmId, userId)) {
            throw new BusinessException("无权限操作该虚拟机");
        }

        // 2. 查询现有虚拟机
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new BusinessException("虚拟机不存在: " + vmId);
        }

        // 3. 检查虚拟机状态
        if (!force && "RUNNING".equals(vmInstance.getStatus())) {
            throw new BusinessException("虚拟机正在运行，请先停止后再删除，或使用强制删除");
        }

        try {
            // 4. 删除相关资源（这里简化处理，实际需要删除关联的任务、模型等）
            VmDeleteResponseVO.DeletedResources deletedResources = VmDeleteResponseVO.DeletedResources.builder()
                .tasks(0)
                .models(0)
                .datasets(0)
                .logs(0)
                .build();

            // 5. 执行删除
            int result = vmInstancesMapper.deleteByVmId(vmId);
            if (result <= 0) {
                throw new BusinessException("删除虚拟机失败");
            }

            logger.info("虚拟机删除成功: vmId={}", vmId);

            // 6. 构建响应
            return VmDeleteResponseVO.builder()
                .vmId(vmId)
                .name(vmInstance.getName())
                .deletedAt(LocalDateTime.now())
                .force(force)
                .deletedResources(deletedResources)
                .message("虚拟机删除成功")
                .warnings(new String[0])
                .build();

        } catch (Exception e) {
            logger.error("删除虚拟机失败: vmId={}", vmId, e);
            throw new BusinessException("删除虚拟机失败: " + e.getMessage());
        }
    }

    // ==================== 虚拟机控制接口实现 ====================

    @Override
    public VmControlResponseVO startVm(String vmId, VmControlDTO controlDTO, String userId) {
        logger.info("启动虚拟机: vmId={}, userId={}", vmId, userId);

        // 1. 权限检查
        if (!hasVmPermission(vmId, userId)) {
            throw new BusinessException("无权限操作该虚拟机");
        }

        // 2. 查询虚拟机状态
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new BusinessException("虚拟机不存在: " + vmId);
        }

        if ("RUNNING".equals(vmInstance.getStatus())) {
            throw new BusinessException("虚拟机已在运行状态");
        }

        try {
            // 3. 更新状态为启动中
            vmInstancesMapper.updateStatus(vmId, "STARTING");

            // 4. 生成命令ID
            String commandId = UUID.randomUUID().toString();

            // 5. 这里应该通过WebSocket向虚拟机发送启动命令，暂时模拟
            // TODO: 实现WebSocket命令发送

            logger.info("虚拟机启动命令已发送: vmId={}, commandId={}", vmId, commandId);

            return VmControlResponseVO.builder()
                .vmId(vmId)
                .operation("start")
                .expectedStatus("STARTING")
                .commandId(commandId)
                .estimatedTime(controlDTO.getTimeout())
                .startedAt(LocalDateTime.now())
                .async(true)
                .operationStatus("PENDING")
                .progress(0)
                .message("虚拟机启动命令已发送")
                .details(VmControlResponseVO.OperationDetails.builder()
                    .force(controlDTO.getForce())
                    .timeout(controlDTO.getTimeout())
                    .reason(controlDTO.getReason())
                    .config(controlDTO.getConfig())
                    .build())
                .nextAction("使用状态查询接口监控启动进度")
                .monitorUrl("/api/v1/vm/" + vmId + "/status")
                .build();

        } catch (Exception e) {
            logger.error("启动虚拟机失败: vmId={}", vmId, e);
            throw new BusinessException("启动虚拟机失败: " + e.getMessage());
        }
    }

    @Override
    public VmControlResponseVO stopVm(String vmId, VmControlDTO controlDTO, String userId) {
        logger.info("停止虚拟机: vmId={}, userId={}", vmId, userId);

        // 权限检查和状态检查逻辑类似startVm
        if (!hasVmPermission(vmId, userId)) {
            throw new BusinessException("无权限操作该虚拟机");
        }

        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new BusinessException("虚拟机不存在: " + vmId);
        }

        if ("STOPPED".equals(vmInstance.getStatus()) || "OFFLINE".equals(vmInstance.getStatus())) {
            throw new BusinessException("虚拟机已停止");
        }

        try {
            vmInstancesMapper.updateStatus(vmId, "STOPPING");
            String commandId = UUID.randomUUID().toString();

            return VmControlResponseVO.builder()
                .vmId(vmId)
                .operation("stop")
                .expectedStatus("STOPPING")
                .commandId(commandId)
                .estimatedTime(controlDTO.getTimeout())
                .startedAt(LocalDateTime.now())
                .async(true)
                .operationStatus("PENDING")
                .progress(0)
                .message("虚拟机停止命令已发送")
                .details(VmControlResponseVO.OperationDetails.builder()
                    .force(controlDTO.getForce())
                    .timeout(controlDTO.getTimeout())
                    .reason(controlDTO.getReason())
                    .build())
                .nextAction("使用状态查询接口监控停止进度")
                .monitorUrl("/api/v1/vm/" + vmId + "/status")
                .build();

        } catch (Exception e) {
            logger.error("停止虚拟机失败: vmId={}", vmId, e);
            throw new BusinessException("停止虚拟机失败: " + e.getMessage());
        }
    }

    @Override
    public VmControlResponseVO restartVm(String vmId, VmControlDTO controlDTO, String userId) {
        logger.info("重启虚拟机: vmId={}, userId={}", vmId, userId);

        // 权限检查
        if (!hasVmPermission(vmId, userId)) {
            throw new BusinessException("无权限操作该虚拟机");
        }

        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new BusinessException("虚拟机不存在: " + vmId);
        }

        try {
            vmInstancesMapper.updateStatus(vmId, "STARTING");
            String commandId = UUID.randomUUID().toString();

            return VmControlResponseVO.builder()
                .vmId(vmId)
                .operation("restart")
                .expectedStatus("STARTING")
                .commandId(commandId)
                .estimatedTime(controlDTO.getTimeout())
                .startedAt(LocalDateTime.now())
                .async(true)
                .operationStatus("PENDING")
                .progress(0)
                .message("虚拟机重启命令已发送")
                .details(VmControlResponseVO.OperationDetails.builder()
                    .force(controlDTO.getForce())
                    .timeout(controlDTO.getTimeout())
                    .reason(controlDTO.getReason())
                    .config(controlDTO.getConfig())
                    .build())
                .nextAction("使用状态查询接口监控重启进度")
                .monitorUrl("/api/v1/vm/" + vmId + "/status")
                .build();

        } catch (Exception e) {
            logger.error("重启虚拟机失败: vmId={}", vmId, e);
            throw new BusinessException("重启虚拟机失败: " + e.getMessage());
        }
    }

    // ==================== 状态查询接口实现 ====================

    @Override
    @Transactional(readOnly = true)
    public VmStatusVO getVmStatus(String vmId, String userId) {
        logger.info("查询虚拟机状态: vmId={}, userId={}", vmId, userId);

        // 1. 权限检查
        if (!hasVmPermission(vmId, userId)) {
            throw new BusinessException("无权限访问该虚拟机");
        }

        // 2. 查询虚拟机信息
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new BusinessException("虚拟机不存在: " + vmId);
        }

        try {
            // 3. 构建状态信息（这里模拟实时数据，实际应该通过WebSocket查询）
            return VmStatusVO.builder()
                .vmId(vmId)
                .status(vmInstance.getStatus())
                .connectionStatus(vmInstance.getConnectionStatus())
                .uptime(calculateUptime(vmInstance))
                .resourceUsage(VmStatusVO.RealTimeResourceUsage.builder()
                    .cpu(65.5)
                    .memory(72.3)
                    .disk(45.8)
                    .gpu(23.1)
                    .temperature(58.2)
                    .power(120.5)
                    .build())
                .network(VmStatusVO.RealTimeNetwork.builder()
                    .ipAddress(vmInstance.getIpAddress())
                    .macAddress("00:11:22:33:44:55")
                    .port(vmInstance.getPort())
                    .uploadSpeed(1024L)
                    .downloadSpeed(2048L)
                    .latency(50)
                    .packetLoss(0.1)
                    .build())
                .processes(VmStatusVO.ProcessInfo.builder()
                    .total(150)
                    .active(25)
                    .system(10)
                    .user(15)
                    .zombie(0)
                    .build())
                .lastHeartbeat(vmInstance.getLastHeartbeat())
                .wsSessionId(vmInstance.getWsSessionId())
                .healthCheck(VmStatusVO.HealthCheck.builder()
                    .overall("HEALTHY")
                    .cpu("HEALTHY")
                    .memory("WARNING")
                    .disk("HEALTHY")
                    .network("HEALTHY")
                    .services("HEALTHY")
                    .build())
                .error(null)
                .warnings(new String[]{"内存使用率偏高"})
                .checkedAt(LocalDateTime.now())
                .realTime(true)
                .refreshInterval(5)
                .build();

        } catch (Exception e) {
            logger.error("查询虚拟机状态失败: vmId={}", vmId, e);
            throw new BusinessException("查询虚拟机状态失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean hasVmPermission(String vmId, String userId) {
        // 这里简化权限检查，实际应该根据具体的权限模型实现
        // TODO: 实现基于用户和虚拟机的权限控制逻辑
        logger.debug("检查虚拟机权限: vmId={}, userId={}", vmId, userId);
        
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        
        // 暂时允许所有已认证用户访问所有虚拟机
        return true;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 转换为虚拟机列表VO
     */
    private VmListVO convertToVmListVO(VmInstance vmInstance) {
        return VmListVO.builder()
            .vmId(vmInstance.getId())
            .name(vmInstance.getName())
            .ipAddress(vmInstance.getIpAddress())
            .port(vmInstance.getPort())
            .status(vmInstance.getStatus())
            .osType(vmInstance.getOsType())
            .cpuCores(vmInstance.getCpuCores())
            .memoryMb(vmInstance.getMemoryMb())
            .diskGb(vmInstance.getDiskGb())
            .connectionStatus(vmInstance.getConnectionStatus())
            .lastHeartbeat(vmInstance.getLastHeartbeat())
            .createdAt(vmInstance.getCreatedAt())
            .updatedAt(vmInstance.getUpdatedAt())
            .resourceUsage(VmListVO.ResourceUsage.builder()
                .cpu(calculateCpuUsage())
                .memory(calculateMemoryUsage())
                .disk(calculateDiskUsage())
                .build())
            .online("CONNECTED".equals(vmInstance.getConnectionStatus()))
            .uptime(calculateUptime(vmInstance))
            .build();
    }

    /**
     * 转换为虚拟机详情VO
     */
    private VmDetailVO convertToVmDetailVO(VmInstance vmInstance) {
        try {
            return VmDetailVO.builder()
                .vmId(vmInstance.getId())
                .name(vmInstance.getName())
                .ipAddress(vmInstance.getIpAddress())
                .port(vmInstance.getPort())
                .status(vmInstance.getStatus())
                .osType(vmInstance.getOsType())
                .cpuCores(vmInstance.getCpuCores())
                .memoryMb(vmInstance.getMemoryMb())
                .diskGb(vmInstance.getDiskGb())
                .connectionStatus(vmInstance.getConnectionStatus())
                .lastHeartbeat(vmInstance.getLastHeartbeat())
                .wsSessionId(vmInstance.getWsSessionId())
                .createdAt(vmInstance.getCreatedAt())
                .updatedAt(vmInstance.getUpdatedAt())
                .systemInfo(parseJsonField(vmInstance.getSystemInfo()))
                .capabilities(parseJsonField(vmInstance.getCapabilities()))
                .networkConfig(parseJsonField(vmInstance.getNetworkConfig()))
                .metadata(parseJsonField(vmInstance.getMetadata()))
                .resourceUsage(VmDetailVO.DetailedResourceUsage.builder()
                    .cpu(65.5)
                    .memory(72.3)
                    .disk(45.8)
                    .gpu(23.1)
                    .uploadSpeed(1024L)
                    .downloadSpeed(2048L)
                    .latency(50)
                    .build())
                .network(VmDetailVO.NetworkInfo.builder()
                    .macAddress("00:11:22:33:44:55")
                    .bandwidth(1000)
                    .uploadSpeed(1024L)
                    .downloadSpeed(2048L)
                    .latency(50)
                    .build())
                .processes(VmDetailVO.ProcessInfo.builder()
                    .total(150)
                    .active(25)
                    .system(10)
                    .user(15)
                    .build())
                .uptime(calculateUptime(vmInstance))
                .online("CONNECTED".equals(vmInstance.getConnectionStatus()))
                .notes("")
                .build();
        } catch (Exception e) {
            logger.error("转换虚拟机详情失败: vmId={}", vmInstance.getId(), e);
            throw new BusinessException("数据转换失败");
        }
    }

    /**
     * 解析JSON字段
     */
    private Map<String, Object> parseJsonField(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.warn("解析JSON字段失败: {}", jsonString, e);
            return new HashMap<>();
        }
    }

    /**
     * 判断是否需要重启
     */
    private Boolean isRestartRequired(List<String> updatedFields) {
        Set<String> restartFields = Set.of("cpuCores", "memoryMb", "diskGb", "networkConfig");
        return updatedFields.stream().anyMatch(restartFields::contains);
    }

    /**
     * 计算运行时长
     */
    private Long calculateUptime(VmInstance vmInstance) {
        // 简化计算，实际应该根据状态变化历史计算
        if ("RUNNING".equals(vmInstance.getStatus()) && vmInstance.getLastHeartbeat() != null) {
            return java.time.Duration.between(vmInstance.getCreatedAt(), LocalDateTime.now()).getSeconds();
        }
        return 0L;
    }

    /**
     * 计算CPU使用率（模拟）
     */
    private Double calculateCpuUsage() {
        return 45.0 + Math.random() * 30;
    }

    /**
     * 计算内存使用率（模拟）
     */
    private Double calculateMemoryUsage() {
        return 60.0 + Math.random() * 25;
    }

    /**
     * 计算磁盘使用率（模拟）
     */
    private Double calculateDiskUsage() {
        return 30.0 + Math.random() * 40;
    }

    /**
     * 生成访问令牌
     *
     * @param vmId 虚拟机ID
     * @return JWT访问令牌
     */
    private String generateAccessToken(String vmId) {
        // 从数据库获取VM信息
        VmInstance vmInstance = vmInstancesMapper.selectByVmId(vmId);
        if (vmInstance == null) {
            throw new BusinessException("VM实例不存在: " + vmId);
        }
        return vmJwtUtil.generateAccessToken(vmId, vmInstance.getName(), vmInstance.getStatus());
    }

}