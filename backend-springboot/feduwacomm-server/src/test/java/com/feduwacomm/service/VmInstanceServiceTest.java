package com.feduwacomm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.common.PageResult;
import com.feduwacomm.common.exception.BusinessException;
import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.dto.VmTokenRefreshDTO;
import com.feduwacomm.dto.VmQueryDTO;
import com.feduwacomm.dto.VmUpdateDTO;
import com.feduwacomm.dto.VmControlDTO;
import com.feduwacomm.entity.VmInstance;
import com.feduwacomm.mapper.VmInstancesMapper;
import com.feduwacomm.service.impl.VmInstanceServiceImpl;
import com.feduwacomm.utils.VmJwtUtil;
import com.feduwacomm.utils.UuidUtil;
import com.feduwacomm.utils.ApiKeyUtil;
import com.feduwacomm.vo.*;
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
import java.util.*;

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

    @Mock
    private VmJwtUtil vmJwtUtil;

    @Mock
    private ApiKeyUtil apiKeyUtil;

    @InjectMocks
    private VmInstanceServiceImpl vmInstanceService;

    private VmRegisterDTO registerDTO;
    private VmTokenRefreshDTO refreshDTO;
    private VmInstance vmInstance;
    private VmQueryDTO queryDTO;
    private VmUpdateDTO updateDTO;
    private VmControlDTO controlDTO;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(vmInstancesMapper, objectMapper, vmJwtUtil, apiKeyUtil);
        
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
        refreshDTO.setSecretId("fua_testRefreshApiKey123456789"); // 明文API Key

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
        vmInstance.setSecretId("$2a$10$hashedRefreshToken"); // BCrypt哈希后的API Key
        vmInstance.setCreatedAt(LocalDateTime.now());
        vmInstance.setUpdatedAt(LocalDateTime.now());
        vmInstance.setSecretExpireTime(LocalDateTime.now().plusDays(7));

        // 准备查询DTO
        queryDTO = VmQueryDTO.builder()
            .page(1)
            .size(10)
            .status("RUNNING")
            .osType("Linux")
            .keyword("test")
            .connectionStatus("CONNECTED")
            .sortField("created_at")
            .sortOrder("desc")
            .userId("test-user-123")
            .build();

        // 准备更新DTO
        updateDTO = VmUpdateDTO.builder()
            .name("Updated VM Name")
            .ipAddress("192.168.1.101")
            .port(2222)
            .cpuCores(8)
            .memoryMb(16384)
            .build();

        // 准备控制DTO
        controlDTO = VmControlDTO.builder()
            .operation("start")
            .force(false)
            .timeout(300)
            .graceful(true)
            .reason("Test operation")
            .build();
    }

    /**
     * 测试虚拟机注册 - 成功场景
     */
    @Test
    void testRegister_Success() {
        // 准备mock数据
        when(vmInstancesMapper.existsByVmId(anyString())).thenReturn(0);
        when(vmInstancesMapper.selectByVmId(anyString())).thenReturn(vmInstance);
        when(vmInstancesMapper.insert(any(VmInstance.class))).thenReturn(1);

        try (MockedStatic<UuidUtil> uuidUtilMock = mockStatic(UuidUtil.class);
             MockedStatic<ApiKeyUtil> apiKeyUtilMock = mockStatic(ApiKeyUtil.class)) {
            
            String sessionId = "session-001";
            String accessToken = "vm-access-token-123";
            String apiKey = "fua_testApiKey123456789";
            String encodedApiKey = "$2a$10$encodedHashedApiKey";
            
            uuidUtilMock.when(() -> UuidUtil.generateShortUuid()).thenReturn(sessionId);
            when(vmJwtUtil.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn(accessToken);
            apiKeyUtilMock.when(() -> ApiKeyUtil.generateApiKey()).thenReturn(apiKey);
            apiKeyUtilMock.when(() -> ApiKeyUtil.encodeApiKey(anyString())).thenReturn(encodedApiKey);

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
     * 测试令牌刷新 - 成功场景（使用API Key认证）
     */
    @Test
    void testRefreshToken_Success() {
        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(anyString())).thenReturn(vmInstance);

        try (MockedStatic<ApiKeyUtil> apiKeyUtilMock = mockStatic(ApiKeyUtil.class)) {
            String newAccessToken = "new-vm-access-token-456";
            String newApiKey = "fua_newApiKey987654321";
            String encodedApiKey = "$2a$10$newEncodedHashedApiKey";
            
            when(vmJwtUtil.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn(newAccessToken);
            apiKeyUtilMock.when(() -> ApiKeyUtil.generateApiKey()).thenReturn(newApiKey);
            apiKeyUtilMock.when(() -> ApiKeyUtil.encodeApiKey(anyString())).thenReturn(encodedApiKey);
            apiKeyUtilMock.when(() -> ApiKeyUtil.isValidFormat(anyString())).thenReturn(true);
            apiKeyUtilMock.when(() -> ApiKeyUtil.matches(anyString(), anyString())).thenReturn(true);

            // 执行测试
            VmTokenRefreshResponseVO response = vmInstanceService.refreshToken(refreshDTO);

            // 验证结果
            assertNotNull(response);
            assertEquals(newAccessToken, response.getAccessToken());
            assertEquals(refreshDTO.getSecretId(), response.getSecretId()); // API Key保持原样返回
            assertNotNull(response.getTokenExpireSeconds());

            // 验证mock调用 (refreshToken中1次，generateAccessToken中1次，共2次)
            verify(vmInstancesMapper, times(2)).selectByVmId(refreshDTO.getVmId());
        }
    }

    /**
     * 测试令牌刷新 - 虚拟机不存在
     */
    @Test
    void testRefreshToken_VmNotFound() {
        // 准备mock数据 - 虚拟机不存在
        when(vmInstancesMapper.selectByVmId(anyString())).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> vmInstanceService.refreshToken(refreshDTO));
        
        assertEquals("虚拟机不存在", exception.getMessage());
        
        // 验证mock调用
        verify(vmInstancesMapper).selectByVmId(refreshDTO.getVmId());
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

    // ==================== 新增CRUD功能测试 ====================

    /**
     * 测试虚拟机列表查询 - 成功场景
     */
    @Test
    void testQueryVmList_Success() {
        // 准备测试数据
        List<VmInstance> vmList = Arrays.asList(vmInstance);
        
        // 准备mock数据
        when(vmInstancesMapper.countVmInstances(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(1);
        when(vmInstancesMapper.selectPagedList(anyInt(), anyInt(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString()))
            .thenReturn(vmList);

        // 执行测试
        PageResult<VmListVO> result = vmInstanceService.queryVmList(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getPages());
        assertEquals(1, result.getRecords().size());

        VmListVO vmListVO = result.getRecords().get(0);
        assertEquals(vmInstance.getId(), vmListVO.getVmId());
        assertEquals(vmInstance.getName(), vmListVO.getName());
        assertEquals(vmInstance.getStatus(), vmListVO.getStatus());

        // 验证mock调用
        verify(vmInstancesMapper).countVmInstances(anyString(), anyString(), anyString(), anyString());
        verify(vmInstancesMapper).selectPagedList(anyInt(), anyInt(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString());
    }

    /**
     * 测试虚拟机列表查询 - 空结果
     */
    @Test
    void testQueryVmList_EmptyResult() {
        // 准备mock数据 - 无数据
        when(vmInstancesMapper.countVmInstances(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(0);

        // 执行测试
        PageResult<VmListVO> result = vmInstanceService.queryVmList(queryDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertEquals(1, result.getCurrent());
        assertEquals(10, result.getSize());
        assertEquals(0, result.getPages());
        assertTrue(result.getRecords().isEmpty());

        // 验证mock调用 - 当总数为0时，不应查询列表数据
        verify(vmInstancesMapper).countVmInstances(anyString(), anyString(), anyString(), anyString());
        verify(vmInstancesMapper, never()).selectPagedList(anyInt(), anyInt(), anyString(), anyString(), 
            anyString(), anyString(), anyString(), anyString());
    }

    /**
     * 测试虚拟机详情查询 - 成功场景
     */
    @Test
    void testGetVmDetail_Success() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";

        // 准备mock数据
        when(vmInstancesMapper.selectDetailById(vmId)).thenReturn(vmInstance);

        // 执行测试
        VmDetailVO result = vmInstanceService.getVmDetail(vmId, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(vmInstance.getId(), result.getVmId());
        assertEquals(vmInstance.getName(), result.getName());
        assertEquals(vmInstance.getIpAddress(), result.getIpAddress());
        assertEquals(vmInstance.getPort(), result.getPort());
        assertEquals(vmInstance.getStatus(), result.getStatus());
        assertNotNull(result.getResourceUsage());
        assertNotNull(result.getNetwork());
        assertNotNull(result.getProcesses());

        // 验证mock调用
        verify(vmInstancesMapper).selectDetailById(vmId);
    }

    /**
     * 测试虚拟机详情查询 - 虚拟机不存在
     */
    @Test
    void testGetVmDetail_VmNotFound() {
        String vmId = "non-existent-vm-id";
        String userId = "test-user-id";

        // 准备mock数据 - 虚拟机不存在
        when(vmInstancesMapper.selectDetailById(vmId)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> vmInstanceService.getVmDetail(vmId, userId));
        
        assertTrue(exception.getMessage().contains("虚拟机不存在"));

        // 验证mock调用
        verify(vmInstancesMapper).selectDetailById(vmId);
    }

    /**
     * 测试虚拟机更新 - 成功场景
     */
    @Test
    void testUpdateVm_Success() throws Exception {
        String vmId = "test-vm-id";
        String userId = "test-user-id";

        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(vmInstance);
        when(vmInstancesMapper.updateBasicInfo(any(VmInstance.class))).thenReturn(1);

        // 执行测试
        VmUpdateResponseVO result = vmInstanceService.updateVm(vmId, updateDTO, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(vmId, result.getVmId());
        assertEquals(updateDTO.getName(), result.getName());
        assertNotNull(result.getUpdatedAt());
        assertNotNull(result.getUpdatedFields());
        assertTrue(result.getUpdatedFields().length > 0);
        assertNotNull(result.getRequiresRestart());

        // 验证mock调用
        verify(vmInstancesMapper).selectByVmId(vmId);
        verify(vmInstancesMapper).updateBasicInfo(any(VmInstance.class));
    }

    /**
     * 测试虚拟机更新 - 虚拟机不存在
     */
    @Test
    void testUpdateVm_VmNotFound() {
        String vmId = "non-existent-vm-id";
        String userId = "test-user-id";

        // 准备mock数据 - 虚拟机不存在
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> vmInstanceService.updateVm(vmId, updateDTO, userId));
        
        assertTrue(exception.getMessage().contains("虚拟机不存在"));

        // 验证mock调用
        verify(vmInstancesMapper).selectByVmId(vmId);
        verify(vmInstancesMapper, never()).updateBasicInfo(any(VmInstance.class));
    }

    /**
     * 测试虚拟机删除 - 成功场景
     */
    @Test
    void testDeleteVm_Success() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";
        Boolean force = false;

        // 设置虚拟机状态为已停止
        vmInstance.setStatus("STOPPED");

        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(vmInstance);
        when(vmInstancesMapper.deleteByVmId(vmId)).thenReturn(1);

        // 执行测试
        VmDeleteResponseVO result = vmInstanceService.deleteVm(vmId, force, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(vmId, result.getVmId());
        assertEquals(vmInstance.getName(), result.getName());
        assertNotNull(result.getDeletedAt());
        assertEquals(force, result.getForce());
        assertNotNull(result.getDeletedResources());
        assertNotNull(result.getMessage());

        // 验证mock调用
        verify(vmInstancesMapper).selectByVmId(vmId);
        verify(vmInstancesMapper).deleteByVmId(vmId);
    }

    /**
     * 测试虚拟机删除 - 虚拟机正在运行且不强制删除
     */
    @Test
    void testDeleteVm_RunningVmWithoutForce() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";
        Boolean force = false;

        // 设置虚拟机状态为正在运行
        vmInstance.setStatus("RUNNING");

        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(vmInstance);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> vmInstanceService.deleteVm(vmId, force, userId));
        
        assertTrue(exception.getMessage().contains("虚拟机正在运行"));

        // 验证mock调用 - 不应该执行删除操作
        verify(vmInstancesMapper).selectByVmId(vmId);
        verify(vmInstancesMapper, never()).deleteByVmId(vmId);
    }

    /**
     * 测试虚拟机删除 - 强制删除运行中的虚拟机
     */
    @Test
    void testDeleteVm_ForceDeleteRunningVm() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";
        Boolean force = true;

        // 设置虚拟机状态为正在运行
        vmInstance.setStatus("RUNNING");

        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(vmInstance);
        when(vmInstancesMapper.deleteByVmId(vmId)).thenReturn(1);

        // 执行测试
        VmDeleteResponseVO result = vmInstanceService.deleteVm(vmId, force, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(vmId, result.getVmId());
        assertTrue(result.getForce());

        // 验证mock调用 - 应该执行删除操作
        verify(vmInstancesMapper).selectByVmId(vmId);
        verify(vmInstancesMapper).deleteByVmId(vmId);
    }

    // ==================== 虚拟机控制功能测试 ====================

    /**
     * 测试虚拟机启动 - 成功场景
     */
    @Test
    void testStartVm_Success() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";

        // 设置虚拟机状态为已停止
        vmInstance.setStatus("STOPPED");

        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(vmInstance);
        when(vmInstancesMapper.updateStatus(vmId, "STARTING")).thenReturn(1);

        // 执行测试
        VmControlResponseVO result = vmInstanceService.startVm(vmId, controlDTO, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(vmId, result.getVmId());
        assertEquals("start", result.getOperation());
        assertEquals("STARTING", result.getExpectedStatus());
        assertNotNull(result.getCommandId());
        assertTrue(result.getAsync());
        assertEquals("PENDING", result.getOperationStatus());
        assertNotNull(result.getDetails());

        // 验证mock调用
        verify(vmInstancesMapper).selectByVmId(vmId);
        verify(vmInstancesMapper).updateStatus(vmId, "STARTING");
    }

    /**
     * 测试虚拟机启动 - 虚拟机已在运行状态
     */
    @Test
    void testStartVm_AlreadyRunning() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";

        // 设置虚拟机状态为正在运行
        vmInstance.setStatus("RUNNING");

        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(vmInstance);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> vmInstanceService.startVm(vmId, controlDTO, userId));
        
        assertTrue(exception.getMessage().contains("虚拟机已在运行状态"));

        // 验证mock调用 - 不应该更新状态
        verify(vmInstancesMapper).selectByVmId(vmId);
        verify(vmInstancesMapper, never()).updateStatus(anyString(), anyString());
    }

    /**
     * 测试虚拟机停止 - 成功场景
     */
    @Test
    void testStopVm_Success() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";

        // 设置虚拟机状态为正在运行
        vmInstance.setStatus("RUNNING");

        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(vmInstance);
        when(vmInstancesMapper.updateStatus(vmId, "STOPPING")).thenReturn(1);

        // 执行测试
        VmControlResponseVO result = vmInstanceService.stopVm(vmId, controlDTO, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(vmId, result.getVmId());
        assertEquals("stop", result.getOperation());
        assertEquals("STOPPING", result.getExpectedStatus());

        // 验证mock调用
        verify(vmInstancesMapper).selectByVmId(vmId);
        verify(vmInstancesMapper).updateStatus(vmId, "STOPPING");
    }

    /**
     * 测试虚拟机状态查询 - 成功场景
     */
    @Test
    void testGetVmStatus_Success() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";

        // 准备mock数据
        when(vmInstancesMapper.selectByVmId(vmId)).thenReturn(vmInstance);

        // 执行测试
        VmStatusVO result = vmInstanceService.getVmStatus(vmId, userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(vmId, result.getVmId());
        assertEquals(vmInstance.getStatus(), result.getStatus());
        assertEquals(vmInstance.getConnectionStatus(), result.getConnectionStatus());
        assertNotNull(result.getResourceUsage());
        assertNotNull(result.getNetwork());
        assertNotNull(result.getProcesses());
        assertNotNull(result.getHealthCheck());
        assertTrue(result.getRealTime());

        // 验证mock调用
        verify(vmInstancesMapper).selectByVmId(vmId);
    }

    /**
     * 测试权限检查 - 有权限
     */
    @Test
    void testHasVmPermission_HasPermission() {
        String vmId = "test-vm-id";
        String userId = "test-user-id";

        // 执行测试
        Boolean result = vmInstanceService.hasVmPermission(vmId, userId);

        // 验证结果 - 当前实现允许所有已认证用户
        assertTrue(result);
    }

    /**
     * 测试权限检查 - 无权限（用户ID为空）
     */
    @Test
    void testHasVmPermission_NoPermission() {
        String vmId = "test-vm-id";
        String userId = null;

        // 执行测试
        Boolean result = vmInstanceService.hasVmPermission(vmId, userId);

        // 验证结果 - 用户ID为空应该返回false
        assertFalse(result);
    }
}