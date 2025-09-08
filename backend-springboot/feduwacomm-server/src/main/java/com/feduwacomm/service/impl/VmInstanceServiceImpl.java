package com.feduwacomm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.dto.VmTokenRefreshDTO;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.VmJwtUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.utils.ApiKeyUtil;
import com.feduwacomm.vo.VmRegisterResponseVO;
import com.feduwacomm.vo.VmTokenRefreshResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

        // 2. 生成访问令牌和API Key
        String accessToken = generateAccessToken(registerDTO.getVmId());
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

        logger.info("虚拟机注册成功: vmId={}, sessionId={}", registerDTO.getVmId(), sessionId);

        // 6. 构建响应
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