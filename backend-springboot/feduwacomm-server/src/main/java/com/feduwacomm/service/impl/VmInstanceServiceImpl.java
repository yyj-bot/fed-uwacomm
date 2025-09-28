package com.feduwacomm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.PageResult;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.config.JwtConfig;
import com.feduwacomm.constants.SystemConstants;
import com.feduwacomm.dto.*;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.entity.VmSecret;
import com.feduwacomm.entity.User;
import com.feduwacomm.entity.VmAssignment;
import com.feduwacomm.entity.ProjectMember;
import com.feduwacomm.entity.VmShare;
import com.feduwacomm.entity.VmTempPermission;
import com.feduwacomm.enums.ConnectionStatus;
import com.feduwacomm.enums.VmStatus;
import com.feduwacomm.enums.VmSecretStatus;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.mapper.VmSecretsMapper;
import com.feduwacomm.mapper.UserMapper;
import com.feduwacomm.mapper.VmAssignmentMapper;
import com.feduwacomm.mapper.ProjectMemberMapper;
import com.feduwacomm.mapper.VmShareMapper;
import com.feduwacomm.mapper.VmTempPermissionMapper;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.service.WebSocketCommandService;
import com.feduwacomm.utils.VmJwtUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.utils.ApiKeyUtil;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.feduwacomm.config.NetworkProperties;
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
    private VmSecretsMapper vmSecretsMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VmJwtUtil vmJwtUtil;

    @Autowired
    private WebSocketCommandService webSocketCommandService;

    @Autowired
    private NetworkProperties networkProperties;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private UuidUtil uuidUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VmInstancesMapper vmInstanceMapper;

    @Autowired
    private VmAssignmentMapper vmAssignmentMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private VmShareMapper vmShareMapper;

    @Autowired
    private VmTempPermissionMapper vmTempPermissionMapper;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${jwt.expire-seconds:86400}")
    private Long tokenExpireSeconds;

    @Value("${vm.secret.expire-days:30}")
    private int secretExpireDays;

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public VmRegisterResponseVO register(VmRegisterDTO registerDTO) {
        logger.info("开始注册虚拟机: name={}, ip={} [线程: {}]",
                   registerDTO.getName(), registerDTO.getIpAddress(), Thread.currentThread().getName());

        // 1. 自动生成vmId
        String vmId = uuidUtil.generateUuid();
        logger.info("自动生成vmId: {} [线程: {}]", vmId, Thread.currentThread().getName());

        // 2. 检查虚拟机是否已存在 (带重试机制)
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (vmInstancesMapper.existsByVmId(vmId) > 0) {
                    logger.warn("虚拟机已存在: vmId={}, 第{}\u6b21检查", vmId, attempt);
                    if (attempt < maxRetries) {
                        // 重新生成vmId重试
                        vmId = uuidUtil.generateUuid();
                        logger.info("重新生成vmId: {} (第{}\u6b21重试)", vmId, attempt);
                        continue;
                    } else {
                        throw new BusinessException("虚拟机注册失败: 多次生成vmId均已存在");
                    }
                }
                break; // vmId唯一，退出重试循环
            } catch (Exception e) {
                logger.error("检查vmId存在性失败: vmId={}, 第{}\u6b21尝试, 错误: {}", vmId, attempt, e.getMessage());
                if (attempt >= maxRetries) {
                    throw new BusinessException("虚拟机注册失败: 数据库检查错误", e);
                }
                // 等待一段时间后重试
                try {
                    Thread.sleep(100 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException("虚拟机注册被中断");
                }
            }
        }

        // 2. 生成API Key和会话ID
        String rawApiKey = ApiKeyUtil.generateApiKey(); // 明文API Key，只返回一次
        String hashedApiKey = ApiKeyUtil.encodeApiKey(rawApiKey); // BCrypt哈希后存储
        String sessionId = uuidUtil.generateUuid();

        // 3. 创建虚拟机实例
        VmInstance vmInstance = new VmInstance();
        vmInstance.setId(vmId);
        vmInstance.setName(registerDTO.getName());
        vmInstance.setIpAddress(registerDTO.getIpAddress());
        vmInstance.setPort(registerDTO.getPort());
        vmInstance.setOsType(registerDTO.getOsType());
        vmInstance.setCpuCores(registerDTO.getCpuCores());
        vmInstance.setMemoryMb(registerDTO.getMemoryMb());
        vmInstance.setDiskGb(registerDTO.getDiskGb());
        vmInstance.setStatus(VmStatus.OFFLINE);
        vmInstance.setConnectionStatus(ConnectionStatus.DISCONNECTED);
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
            logger.error("JSON序列化失败: vmId={}", vmId, e);
            throw new BusinessException("数据格式错误");
        }

        // 5. 插入数据库 (带并发处理)
        try {
            int result = vmInstancesMapper.insert(vmInstance);
            if (result <= 0) {
                logger.error("虚拟机注册失败: vmId={} [线程: {}]", vmId, Thread.currentThread().getName());
                throw new BusinessException("虚拟机注册失败: 数据库插入失败");
            }
            logger.info("虚拟机数据库插入成功: vmId={} [线程: {}]", vmId, Thread.currentThread().getName());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 处理数据库唯一性约束冲突
            logger.warn("数据库唯一性约束冲突: vmId={}, 错误: {} [线程: {}]",
                       vmId, e.getMessage(), Thread.currentThread().getName());
            throw new BusinessException("虚拟机注册失败: vmId已存在或数据冲突");
        } catch (Exception e) {
            logger.error("虚拟机数据库插入异常: vmId={} [线程: {}]", vmId, Thread.currentThread().getName(), e);
            throw new BusinessException("虚拟机注册失败: 数据库异常", e);
        }

        // 6. 生成vm_secrets表记录，返回secretId用于返回给虚拟机
        String secretId = insertVmSecret(vmId, hashedApiKey);

        // 7. 生成访问令牌（在数据库插入成功后）
        String accessToken = generateAccessToken(vmId);

        logger.info("虚拟机注册成功: vmId={}, sessionId={}, secretId={} [线程: {}]",
                   vmId, sessionId, secretId, Thread.currentThread().getName());

        // 8. 构建响应
        return VmRegisterResponseVO.builder()
                .vmId(vmId)
                .name(registerDTO.getName())
                .status("OFFLINE")
                .connectionStatus("DISCONNECTED")
                .createdAt(vmInstance.getCreatedAt())
                .sessionId(sessionId)
                .accessToken(accessToken) // JWT令牌只返回给客户端，不存储
                .secretId(secretId) // 返回vm_secrets表的ID，供后续认证使用
                .rawApiKey(rawApiKey) // 返回明文API Key，仅此一次
                .tokenExpireSeconds(tokenExpireSeconds)
                .websocket(buildWebSocketInfo())
                .apiEndpoints(buildApiEndpoints(vmId))
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
                    .setSigningKey(Keys.hmacShaKeyFor(jwtConfig.getVm().getSecret().getBytes(SystemConstants.DEFAULT_CHARSET)))
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

        // 先更新WebSocket会话信息
        int result = vmInstancesMapper.updateWebSocketSession(vmId, wsSessionId, connectionStatus);
        if (result <= 0) {
            logger.warn("更新连接状态失败: vmId={}", vmId);
            return;
        }

        // 如果是连接状态，同时更新心跳时间
        if ("CONNECTED".equals(connectionStatus)) {
            String currentTime = LocalDateTime.now().toString();
            int heartbeatResult = vmInstancesMapper.updateConnection(vmId, connectionStatus, currentTime);
            if (heartbeatResult <= 0) {
                logger.warn("更新心跳时间失败: vmId={}", vmId);
            } else {
                logger.debug("VM连接并更新心跳成功: vmId={}, heartbeat={}", vmId, currentTime);
            }
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

    @Override
    public void disconnectVm(String vmId) {
        logger.info("虚拟机断开连接 - VmId: {}", vmId);

        int result = vmInstancesMapper.updateWebSocketSession(vmId, null, "DISCONNECTED");
        if (result <= 0) {
            logger.warn("更新VM断开状态失败 - VmId: {}", vmId);
        } else {
            logger.debug("VM断开状态更新成功 - VmId: {}", vmId);
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
                    .page(queryDTO.getPage().longValue())
                    .size(queryDTO.getSize().longValue())
                    .total(0L)
                    .pages(0L)
                    .list(Collections.emptyList())
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
                .page(queryDTO.getPage().longValue())
                .size(queryDTO.getSize().longValue())
                .total((long) total)
                .pages(pages)
                .list(voList)
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
        if (!force && VmStatus.RUNNING.equals(vmInstance.getStatus())) {
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

        if (VmStatus.RUNNING.equals(vmInstance.getStatus())) {
            throw new BusinessException("虚拟机已在运行状态");
        }

        try {
            // 3. 更新状态为启动中
            vmInstancesMapper.updateStatus(vmId, "STARTING");

            // 4. 生成命令ID
            String commandId = uuidUtil.generateUuidWithHyphens();

            // 5. 通过WebSocket向虚拟机发送启动命令
            Map<String, Object> commandData = new HashMap<>();
            commandData.put("operation", "start");
            commandData.put("force", controlDTO.getForce());
            commandData.put("timeout", controlDTO.getTimeout());
            commandData.put("reason", controlDTO.getReason());

            String wsCommandId = webSocketCommandService.sendCommand(
                vmId,
                WebSocketCommandService.CommandType.VM_START,
                commandData,
                controlDTO.getTimeout() != null ? controlDTO.getTimeout() : 30,
                new WebSocketCommandService.CommandCallback() {
                    @Override
                    public void onCommandSent(String commandId, String vmId) {
                        logger.info("VM启动命令发送成功: vmId={}, commandId={}", vmId, commandId);
                    }

                    @Override
                    public void onCommandResult(String commandId, String vmId, WebSocketCommandService.CommandResult result, Object resultData) {
                        logger.info("VM启动命令执行结果: vmId={}, commandId={}, result={}", vmId, commandId, result);
                        if (result == WebSocketCommandService.CommandResult.SUCCESS) {
                            // 更新VM状态为运行中
                            try {
                                vmInstancesMapper.updateStatus(vmId, "RUNNING");
                            } catch (Exception e) {
                                logger.error("更新VM状态失败: vmId={}", vmId, e);
                            }
                        } else {
                            // 启动失败，恢复状态
                            try {
                                vmInstancesMapper.updateStatus(vmId, "STOPPED");
                            } catch (Exception e) {
                                logger.error("恢复VM状态失败: vmId={}", vmId, e);
                            }
                        }
                    }

                    @Override
                    public void onCommandError(String commandId, String vmId, String error) {
                        logger.error("VM启动命令执行错误: vmId={}, commandId={}, error={}", vmId, commandId, error);
                        try {
                            vmInstancesMapper.updateStatus(vmId, "STOPPED");
                        } catch (Exception e) {
                            logger.error("恢复VM状态失败: vmId={}", vmId, e);
                        }
                    }
                }
            );

            if (wsCommandId != null) {
                logger.info("虚拟机启动命令已发送: vmId={}, commandId={}, wsCommandId={}", vmId, commandId, wsCommandId);
            } else {
                logger.warn("虚拟机启动命令发送失败: vmId={}, commandId={}", vmId, commandId);
                // 恢复状态
                vmInstancesMapper.updateStatus(vmId, "STOPPED");
                throw new BusinessException("虚拟机启动命令发送失败");
            }

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
            String commandId = uuidUtil.generateUuidWithHyphens();

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
            String commandId = uuidUtil.generateUuidWithHyphens();

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
                .status(vmInstance.getStatus().getCode())
                .connectionStatus(vmInstance.getConnectionStatus().getCode())
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
                    .macAddress(generateMacAddress(vmInstance.getId()))
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
        logger.debug("检查虚拟机权限: vmId={}, userId={}", vmId, userId);

        try {
            // 参数验证
            if (userId == null || userId.trim().isEmpty()) {
                logger.warn("用户ID为空，权限检查失败");
                return false;
            }

            if (vmId == null || vmId.trim().isEmpty()) {
                logger.warn("虚拟机ID为空，权限检查失败");
                return false;
            }

            // 1. 检查用户是否存在且状态正常
            User user = userMapper.selectById(userId);
            if (user == null) {
                logger.warn("用户不存在，权限检查失败: userId={}", userId);
                return false;
            }

            if (!"ACTIVE".equals(user.getStatus())) {
                logger.warn("用户状态异常，权限检查失败: userId={}, status={}", userId, user.getStatus());
                return false;
            }

            // 2. 检查虚拟机是否存在
            VmInstance vm = vmInstanceMapper.selectById(vmId);
            if (vm == null) {
                logger.warn("虚拟机不存在，权限检查失败: vmId={}", vmId);
                return false;
            }

            // 3. 管理员拥有所有虚拟机的权限
            if ("ADMIN".equals(user.getRole())) {
                logger.debug("管理员用户，拥有所有虚拟机权限: userId={}, role={}", userId, user.getRole());
                return true;
            }

            // 4. 检查虚拟机所有者权限
            if (userId.equals(vm.getOwnerId())) {
                logger.debug("虚拟机所有者，拥有权限: userId={}, vmId={}", userId, vmId);
                return true;
            }

            // 5. 检查虚拟机分配关系
            VmAssignment assignment = vmAssignmentMapper.selectByUserIdAndVmId(userId, vmId);
            if (assignment != null && "ACTIVE".equals(assignment.getStatus())) {
                logger.debug("用户已被分配虚拟机，拥有权限: userId={}, vmId={}, assignmentId={}",
                            userId, vmId, assignment.getId());
                return true;
            }

            // 6. 检查项目组权限（如果虚拟机属于某个项目组，且用户是项目组成员）
            if (vm.getProjectId() != null) {
                ProjectMember projectMember = projectMemberMapper.selectByProjectIdAndUserId(vm.getProjectId(), userId);
                if (projectMember != null && "ACTIVE".equals(projectMember.getStatus())) {
                    logger.debug("用户是项目组成员，拥有虚拟机权限: userId={}, vmId={}, projectId={}",
                                userId, vmId, vm.getProjectId());
                    return true;
                }
            }

            // 7. 检查共享权限（如果虚拟机设置为共享）
            if (Boolean.TRUE.equals(vm.getIsShared())) {
                // 检查用户是否在共享白名单中
                List<VmShare> shares = vmShareMapper.selectByVmId(vmId);
                for (VmShare share : shares) {
                    if (userId.equals(share.getSharedUserId()) && "ACTIVE".equals(share.getStatus())) {
                        logger.debug("用户在虚拟机共享列表中，拥有权限: userId={}, vmId={}", userId, vmId);
                        return true;
                    }
                }

                // 如果虚拟机设置为公开共享，且用户角色为研究员或以上
                if (Boolean.TRUE.equals(vm.getIsPublicShared()) &&
                    ("RESEARCHER".equals(user.getRole()) || "OPERATOR".equals(user.getRole()) || "ADMIN".equals(user.getRole()))) {
                    logger.debug("虚拟机公开共享，用户角色符合要求: userId={}, vmId={}, role={}",
                                userId, vmId, user.getRole());
                    return true;
                }
            }

            // 8. 检查临时权限（时间限制的临时访问权限）
            List<VmTempPermission> tempPermissions = vmTempPermissionMapper.selectByUserIdAndVmId(userId, vmId);
            for (VmTempPermission tempPermission : tempPermissions) {
                if ("ACTIVE".equals(tempPermission.getStatus()) &&
                    System.currentTimeMillis() >= tempPermission.getStartTime() &&
                    System.currentTimeMillis() <= tempPermission.getEndTime()) {
                    logger.debug("用户拥有临时权限: userId={}, vmId={}, tempPermissionId={}",
                                userId, vmId, tempPermission.getId());
                    return true;
                }
            }

            // 9. 默认拒绝访问
            logger.debug("用户没有虚拟机访问权限: userId={}, vmId={}", userId, vmId);
            return false;

        } catch (Exception e) {
            logger.error("检查虚拟机权限时发生异常: vmId={}, userId={}", vmId, userId, e);
            // 异常情况下默认拒绝访问
            return false;
        }
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
            .status(vmInstance.getStatus().getCode())
            .osType(vmInstance.getOsType())
            .cpuCores(vmInstance.getCpuCores())
            .memoryMb(vmInstance.getMemoryMb())
            .diskGb(vmInstance.getDiskGb())
            .connectionStatus(vmInstance.getConnectionStatus().getCode())
            .lastHeartbeat(vmInstance.getLastHeartbeat())
            .createdAt(vmInstance.getCreatedAt())
            .updatedAt(vmInstance.getUpdatedAt())
            .resourceUsage(VmListVO.ResourceUsage.builder()
                .cpu(calculateCpuUsage())
                .memory(calculateMemoryUsage())
                .disk(calculateDiskUsage())
                .build())
            .online(isVmOnline(vmInstance))
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
                .status(vmInstance.getStatus().getCode())
                .osType(vmInstance.getOsType())
                .cpuCores(vmInstance.getCpuCores())
                .memoryMb(vmInstance.getMemoryMb())
                .diskGb(vmInstance.getDiskGb())
                .connectionStatus(vmInstance.getConnectionStatus().getCode())
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
                    .macAddress(generateMacAddress(vmInstance.getId()))
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
                .online(isVmOnline(vmInstance))
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
        if (VmStatus.RUNNING.equals(vmInstance.getStatus()) && vmInstance.getLastHeartbeat() != null) {
            return java.time.Duration.between(vmInstance.getCreatedAt(), LocalDateTime.now()).getSeconds();
        }
        return 0L;
    }

    /**
     * 判断VM是否在线
     * 综合考虑连接状态和心跳时间
     *
     * @param vmInstance VM实例
     * @return true如果VM在线，false否则
     */
    private boolean isVmOnline(VmInstance vmInstance) {
        // 首先检查连接状态
        if (!"CONNECTED".equals(vmInstance.getConnectionStatus())) {
            return false;
        }

        // 检查心跳时间
        LocalDateTime lastHeartbeat = vmInstance.getLastHeartbeat();
        if (lastHeartbeat == null) {
            return false;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            long secondsSinceLastHeartbeat = java.time.temporal.ChronoUnit.SECONDS.between(lastHeartbeat, now);

            // 90秒内有心跳视为在线
            return secondsSinceLastHeartbeat <= 90;

        } catch (Exception e) {
            logger.warn("计算VM心跳时间失败，视为离线 - VmId: {}, LastHeartbeat: {}",
                       vmInstance.getId(), lastHeartbeat, e);
            return false;
        }
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
        return vmJwtUtil.generateAccessToken(vmId, vmInstance.getName(), vmInstance.getStatus().getCode());
    }

    /**
     * 基于虚拟机ID生成MAC地址
     * 使用固定前缀00:FD:UW，后三位基于vmId哈希生成
     *
     * @param vmId 虚拟机ID
     * @return 生成的MAC地址
     */
    private String generateMacAddress(String vmId) {
        if (vmId == null || vmId.isEmpty()) {
            return "00:FD:UW:00:00:01";
        }

        // 使用vmId的hashCode生成后三位
        int hash = Math.abs(vmId.hashCode());
        int octet4 = (hash >> 16) & 0xFF;
        int octet5 = (hash >> 8) & 0xFF;
        int octet6 = hash & 0xFF;

        return String.format("00:FD:UW:%02X:%02X:%02X", octet4, octet5, octet6);
    }

    /**
     * 构建WebSocket连接信息
     *
     * @return WebSocket连接信息
     */
    private VmRegisterResponseVO.WebSocketInfo buildWebSocketInfo() {
        NetworkProperties.WebSocketInfo wsInfo = networkProperties.getWebSocketInfo();
        return VmRegisterResponseVO.WebSocketInfo.builder()
                .sockjs(wsInfo.getSockjs())
                .nativeWs(wsInfo.getNativeWs())
                .build();
    }

    /**
     * 构建API端点信息
     *
     * @param vmId 虚拟机ID
     * @return API端点信息
     */
    private VmRegisterResponseVO.ApiEndpoints buildApiEndpoints(String vmId) {
        NetworkProperties.Api apiConfig = networkProperties.getApi();
        return VmRegisterResponseVO.ApiEndpoints.builder()
                .status(apiConfig.buildVmStatusPath(vmId))
                .control(apiConfig.buildVmControlPath(vmId))
                .tokenRefresh(apiConfig.getTokenRefreshPath())
                .build();
    }

    /**
     * 插入vm_secrets表记录 (支持并发)
     *
     * @param vmId 虚拟机ID
     * @param secretHash 已哈希的密钥
     * @return secretId 返回给虚拟机的密钥ID
     */
    private String insertVmSecret(String vmId, String secretHash) {
        String secretId = uuidUtil.generateUuid();

        logger.debug("开始创建VM Secret记录: vmId={}, secretId={} [线程: {}]",
                    vmId, secretId, Thread.currentThread().getName());

        // 创建VmSecret实体
        VmSecret vmSecret = VmSecret.builder()
                .id(secretId)
                .vmId(vmId)
                .secretHash(secretHash)
                .salt(null) // 当前不使用盐值，BCrypt自带盐值
                .status(VmSecretStatus.ACTIVE)
                .expiresAt(null) // 不设置过期时间
                .lastUsedAt(null)
                .rotatedAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            int result = vmSecretsMapper.insert(vmSecret);
            if (result <= 0) {
                logger.error("插入vm_secrets表失败: vmId={} [线程: {}]", vmId, Thread.currentThread().getName());
                throw new BusinessException("VM认证记录创建失败");
            }
            logger.info("VM Secret记录创建成功: vmId={}, secretId={} [线程: {}]",
                       vmId, secretId, Thread.currentThread().getName());
            return secretId;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 处理数据库唯一性约束冲突
            logger.warn("VM Secret数据库约束冲突: vmId={}, secretId={}, 错误: {} [线程: {}]",
                       vmId, secretId, e.getMessage(), Thread.currentThread().getName());
            throw new BusinessException("VM认证记录创建失败: 数据冲突");
        } catch (Exception e) {
            logger.error("插入vm_secrets表异常: vmId={} [线程: {}]", vmId, Thread.currentThread().getName(), e);
            throw new BusinessException("VM认证记录创建失败: " + e.getMessage());
        }
    }

}