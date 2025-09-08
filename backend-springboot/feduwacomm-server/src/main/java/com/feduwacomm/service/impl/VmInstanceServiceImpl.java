package com.feduwacomm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.dto.VmTokenRefreshDTO;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.JwtUtil;
import com.feduwacomm.utils.UuidUtil;
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

        // 2. 生成访问令牌和刷新凭证
        String accessToken = generateAccessToken(registerDTO.getVmId());
        String secretId = generateSecretId();
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
        // JWT令牌不存储在数据库中
        vmInstance.setSecretId(secretId);
        vmInstance.setSecretExpireTime(LocalDateTime.now().plusDays(secretExpireDays));
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
                .secretId(secretId)
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

        // 1. 验证刷新凭证
        VmInstance vmInstance = vmInstancesMapper.selectBySecretId(refreshDTO.getSecretId());
        if (vmInstance == null || !vmInstance.getId().equals(refreshDTO.getVmId())) {
            logger.warn("无效的刷新凭证: vmId={}", refreshDTO.getVmId());
            throw new BusinessException("无效的刷新凭证");
        }

        // 2. 检查凭证是否过期
        if (vmInstance.getSecretExpireTime().isBefore(LocalDateTime.now())) {
            logger.warn("刷新凭证已过期: vmId={}", refreshDTO.getVmId());
            throw new BusinessException("刷新凭证已过期");
        }

        // 3. 生成新的访问令牌
        String newAccessToken = generateAccessToken(refreshDTO.getVmId());
        LocalDateTime newTokenExpireTime = LocalDateTime.now().plusSeconds(tokenExpireSeconds);

        // 4. 生成新的刷新凭证（凭证旋转）
        String newSecretId = generateSecretId();
        LocalDateTime newSecretExpireTime = LocalDateTime.now().plusDays(secretExpireDays);

        // 5. 更新数据库（只更新secretId，不存储JWT）
        vmInstance.setSecretId(newSecretId);
        vmInstance.setSecretExpireTime(newSecretExpireTime);
        vmInstance.setUpdatedAt(LocalDateTime.now());

        int result = vmInstancesMapper.update(vmInstance);
        if (result <= 0) {
            logger.error("Token刷新失败: vmId={}", refreshDTO.getVmId());
            throw new BusinessException("Token刷新失败");
        }

        logger.info("Token刷新成功: vmId={}", refreshDTO.getVmId());

        return VmTokenRefreshResponseVO.builder()
                .accessToken(newAccessToken)
                .tokenExpireSeconds(tokenExpireSeconds)
                .secretId(newSecretId)
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
        Map<String, Object> claims = new HashMap<>();
        claims.put("vmId", vmId);
        claims.put("type", "vm_access");
        return JwtUtil.createToken(claims);
    }

    /**
     * 生成刷新凭证
     *
     * @return 刷新凭证
     */
    private String generateSecretId() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return "s3cr3t_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}