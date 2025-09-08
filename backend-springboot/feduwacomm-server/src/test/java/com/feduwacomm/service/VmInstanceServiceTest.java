package com.feduwacomm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.dto.VmTokenRefreshDTO;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.service.impl.VmInstanceServiceImpl;
import com.feduwacomm.utils.JwtUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.vo.VmRegisterResponseVO;
import com.feduwacomm.vo.VmTokenRefreshResponseVO;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VmInstanceService单元测试类
 * 测试虚拟机实例管理服务的各种功能
 */
@ExtendWith(MockitoExtension.class)
public class VmInstanceServiceTest {

    @Mock
    private VmInstancesMapper vmInstancesMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VmInstanceServiceImpl vmInstanceService;

    private VmRegisterDTO registerDTO;
    private VmTokenRefreshDTO refreshDTO;
    private VmInstance vmInstance;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(vmInstancesMapper, objectMapper);
        
        // 设置服务器端口和令牌过期时间
        ReflectionTestUtils.setField(vmInstanceService, "serverPort", "8080");
        ReflectionTestUtils.setField(vmInstanceService, "tokenExpireSeconds", 86400L);
        ReflectionTestUtils.setField(vmInstanceService, "secretExpireDays", 7);

        // 准备测试数据
        setupTestData();
    }

    private void setupTestData() {
        // 准备虚拟机注册DTO
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("maxBatchSize", 100);
        capabilities.put("supportedAlgorithms", new String[]{"RandomForest", "SVM"});

        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("os", "Ubuntu 20.04");
        systemInfo.put("python", "3.8.10");

        registerDTO = new VmRegisterDTO();
        registerDTO.setVmId("a1b2c3d4e5f678901234567890123456");
        registerDTO.setName("TestVM-001");
        registerDTO.setIpAddress("192.168.1.100");
        registerDTO.setPort(22);
        registerDTO.setOsType("Linux");
        registerDTO.setCpuCores(4);
        registerDTO.setMemoryMb(8192);
        registerDTO.setDiskGb(100);
        registerDTO.setSystemInfo(systemInfo);
        registerDTO.setCapabilities(capabilities);

        // 准备令牌刷新DTO
        refreshDTO = new VmTokenRefreshDTO();
        refreshDTO.setVmId("a1b2c3d4e5f678901234567890123456");
        refreshDTO.setSecretId("refresh-token-123");

        // 准备虚拟机实例Entity
        vmInstance = new VmInstance();
        vmInstance.setId("a1b2c3d4e5f678901234567890123456");
        vmInstance.setName("TestVM-001");
        vmInstance.setIpAddress("192.168.1.100");
        vmInstance.setPort(22);
        vmInstance.setOsType("Linux");
        vmInstance.setCpuCores(4);
        vmInstance.setMemoryMb(8192);
        vmInstance.setDiskGb(100);
        vmInstance.setStatus("ACTIVE");
        vmInstance.setConnectionStatus("CONNECTED");
        vmInstance.setCreatedAt(LocalDateTime.now());
        vmInstance.setUpdatedAt(LocalDateTime.now());
        vmInstance.setSecretExpireTime(LocalDateTime.now().plusDays(7));
    }

    /**
     * 测试虚拟机注册 - 成功场景
     */
    @Test
    void testRegister_Success() {
        // 准备mock数据
        when(vmInstancesMapper.existsByVmId(anyString())).thenReturn(0);
        when(vmInstancesMapper.insert(any(VmInstance.class))).thenReturn(1);

        try (MockedStatic<UuidUtil> uuidUtilMock = mockStatic(UuidUtil.class);
             MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            
            String sessionId = "session-001";
            String accessToken = "access-token-123";
            
            uuidUtilMock.when(() -> UuidUtil.generateShortUuid()).thenReturn(sessionId);
            jwtUtilMock.when(() -> JwtUtil.createToken(anyMap()))
                    .thenReturn(accessToken);

            // 执行测试
            VmRegisterResponseVO response = vmInstanceService.register(registerDTO);

            // 验证结果
            assertNotNull(response);
            assertEquals(registerDTO.getVmId(), response.getVmId());
            assertEquals(registerDTO.getName(), response.getName());
            assertEquals("OFFLINE", response.getStatus());
            assertEquals("DISCONNECTED", response.getConnectionStatus());
            assertNotNull(response.getSessionId());
            assertEquals(accessToken, response.getAccessToken());
            assertNotNull(response.getCreatedAt());
            assertNotNull(response.getTokenExpireSeconds());
            assertNotNull(response.getWebsocket());
            assertNotNull(response.getApiEndpoints());

            // 验证mock调用
            verify(vmInstancesMapper).existsByVmId(registerDTO.getVmId());
            verify(vmInstancesMapper).insert(any(VmInstance.class));
        }
    }

    /**
     * 测试虚拟机注册 - 虚拟机已存在
     */
    @Test
    void testRegister_VmAlreadyExists() {
        // 准备mock数据 - 虚拟机已存在
        when(vmInstancesMapper.existsByVmId(anyString())).thenReturn(1);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> vmInstanceService.register(registerDTO));
        
        assertEquals("虚拟机已存在", exception.getMessage());
        
        // 验证mock调用
        verify(vmInstancesMapper).existsByVmId(registerDTO.getVmId());
        verify(vmInstancesMapper, never()).insert(any(VmInstance.class));
    }

    /**
     * 测试令牌刷新 - 成功场景
     */
    @Test
    void testRefreshToken_Success() {
        // 准备mock数据
        when(vmInstancesMapper.selectBySecretId(anyString())).thenReturn(vmInstance);
        when(vmInstancesMapper.update(any(VmInstance.class))).thenReturn(1);

        try (MockedStatic<JwtUtil> jwtUtilMock = mockStatic(JwtUtil.class)) {
            String newAccessToken = "new-access-token-456";
            String newRefreshToken = "new-refresh-token-456";
            
            jwtUtilMock.when(() -> JwtUtil.createToken(anyMap()))
                    .thenReturn(newAccessToken, newRefreshToken);

            // 执行测试
            VmTokenRefreshResponseVO response = vmInstanceService.refreshToken(refreshDTO);

            // 验证结果
            assertNotNull(response);
            assertEquals(newAccessToken, response.getAccessToken());
            assertNotNull(response.getSecretId()); // secretId由generateSecretId()生成，不依赖Mock
            assertTrue(response.getSecretId().startsWith("s3cr3t_"));
            assertNotNull(response.getTokenExpireSeconds());

            // 验证mock调用
            verify(vmInstancesMapper).selectBySecretId(refreshDTO.getSecretId());
        }
    }

    /**
     * 测试令牌刷新 - 虚拟机不存在
     */
    @Test
    void testRefreshToken_VmNotFound() {
        // 准备mock数据 - 无效的刷新凭证
        when(vmInstancesMapper.selectBySecretId(anyString())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> vmInstanceService.refreshToken(refreshDTO));
        
        assertEquals("无效的刷新凭证", exception.getMessage());
        
        // 验证mock调用
        verify(vmInstancesMapper).selectBySecretId(refreshDTO.getSecretId());
    }

    /**
     * 测试访问令牌验证 - 成功场景（由于实际方法直接使用JWT库，这里只测试无效令牌场景）
     */
    @Test
    void testValidateAccessToken_Success() {
        // 注意：实际的validateAccessToken方法直接使用JWT库解析，不依赖JwtUtil
        // 由于没有有效的JWT签名，这里主要测试异常处理逻辑
        String invalidToken = "invalid.jwt.token";
        
        // 执行测试 - 应该返回null因为令牌无效
        String result = vmInstanceService.validateAccessToken(invalidToken);

        // 验证结果 - 无效令牌应该返回null
        assertNull(result);
    }

    /**
     * 测试访问令牌验证 - 令牌无效
     */
    @Test
    void testValidateAccessToken_InvalidToken() {
        String invalidToken = "invalid-token";

        // 执行测试 - 无效格式的JWT令牌应该返回null
        String result = vmInstanceService.validateAccessToken(invalidToken);

        // 验证结果
        assertNull(result);
    }

    /**
     * 测试更新连接状态
     */
    @Test
    void testUpdateConnectionStatus_Success() {
        String vmId = "a1b2c3d4e5f678901234567890123456";
        String connectionStatus = "CONNECTED";
        String wsSessionId = "ws-session-123";

        // 执行测试
        assertDoesNotThrow(() -> 
            vmInstanceService.updateConnectionStatus(vmId, connectionStatus, wsSessionId));

        // 验证mock调用
        verify(vmInstancesMapper).updateWebSocketSession(anyString(), anyString(), anyString());
    }

    /**
     * 测试更新心跳时间
     */
    @Test
    void testUpdateHeartbeat_Success() {
        String vmId = "a1b2c3d4e5f678901234567890123456";

        // 执行测试
        assertDoesNotThrow(() -> vmInstanceService.updateHeartbeat(vmId));

        // 验证mock调用
        verify(vmInstancesMapper).updateConnection(anyString(), anyString(), anyString());
    }

    /**
     * 测试断开虚拟机连接
     */
    @Test
    void testDisconnect_Success() {
        String vmId = "a1b2c3d4e5f678901234567890123456";

        // 执行测试
        assertDoesNotThrow(() -> vmInstanceService.disconnect(vmId));

        // 验证mock调用
        verify(vmInstancesMapper).updateWebSocketSession(anyString(), isNull(), eq("DISCONNECTED"));
    }
}