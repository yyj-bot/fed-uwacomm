package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.VmRegisterDTO;
import com.feduwacomm.dto.VmTokenRefreshDTO;
import com.feduwacomm.service.VmInstanceService;
import com.feduwacomm.utils.IpUtil;
import com.feduwacomm.vo.VmRegisterResponseVO;
import com.feduwacomm.vo.VmTokenRefreshResponseVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 虚拟机实例控制器测试类
 * 测试VM注册、Token刷新、心跳等功能
 */
@SpringBootTest
@AutoConfigureMockMvc
public class VmInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VmInstanceService vmInstanceService;

    @Autowired
    private ObjectMapper objectMapper;

    private VmRegisterDTO validRegisterDTO;
    private VmTokenRefreshDTO validTokenRefreshDTO;
    private VmRegisterResponseVO registerResponse;
    private VmTokenRefreshResponseVO tokenRefreshResponse;

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(vmInstanceService);

        // 准备测试数据
        setupTestData();
    }

    private void setupTestData() {
        // 准备注册DTO
        validRegisterDTO = new VmRegisterDTO();
        validRegisterDTO.setVmId("12345678901234567890123456789012");
        validRegisterDTO.setName("TestVM");
        validRegisterDTO.setIpAddress("192.168.1.100");
        validRegisterDTO.setPort(8080);
        validRegisterDTO.setOsType("Ubuntu 20.04");
        validRegisterDTO.setCpuCores(4);
        validRegisterDTO.setMemoryMb(8192);
        validRegisterDTO.setDiskGb(100);

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("supportedAlgorithms", new String[]{"fedavg", "fedprox"});
        capabilities.put("maxBatchSize", 1000);
        capabilities.put("gpuMemoryMb", 8192);
        validRegisterDTO.setCapabilities(capabilities);

        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("pythonVersion", "3.8.10");
        systemInfo.put("cudaVersion", "11.8");
        validRegisterDTO.setSystemInfo(systemInfo);

        // 准备Token刷新DTO
        validTokenRefreshDTO = new VmTokenRefreshDTO();
        validTokenRefreshDTO.setVmId("12345678901234567890123456789012");
        validTokenRefreshDTO.setSecretId("test-secret-id-12345");

        // 准备响应数据
        registerResponse = VmRegisterResponseVO.builder()
                .vmId("12345678901234567890123456789012")
                .name("TestVM")
                .status("ACTIVE")
                .connectionStatus("CONNECTED")
                .createdAt(LocalDateTime.now())
                .sessionId("session-12345")
                .accessToken("access-token-12345")
                .secretId("secret-id-12345")
                .tokenExpireSeconds(3600L)
                .websocket(VmRegisterResponseVO.WebSocketInfo.builder()
                        .sockjs("ws://localhost:8080/sockjs-vm")
                        .nativeWs("ws://localhost:8080/vm-websocket")
                        .build())
                .apiEndpoints(VmRegisterResponseVO.ApiEndpoints.builder()
                        .status("/api/v1/vm/status")
                        .control("/api/v1/vm/control")
                        .tokenRefresh("/api/v1/vm/token/refresh")
                        .build())
                .build();

        tokenRefreshResponse = VmTokenRefreshResponseVO.builder()
                .accessToken("new-access-token-12345")
                .tokenExpireSeconds(3600L)
                .secretId("new-secret-id-12345")
                .build();
    }

    /**
     * 测试VM注册 - 成功场景
     */
    @Test
    void testVmRegister_Success() throws Exception {
        // Mock服务层方法
        when(vmInstanceService.register(any(VmRegisterDTO.class)))
                .thenReturn(registerResponse);

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(() -> IpUtil.getClientIpAddress(any()))
                    .thenReturn("127.0.0.1");

            mockMvc.perform(post("/api/v1/vm/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("虚拟机注册成功"))
                    .andExpect(jsonPath("$.data.vmId").value("12345678901234567890123456789012"))
                    .andExpect(jsonPath("$.data.name").value("TestVM"))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.data.connectionStatus").value("CONNECTED"))
                    .andExpect(jsonPath("$.data.sessionId").value("session-12345"))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token-12345"))
                    .andExpect(jsonPath("$.data.secretId").value("secret-id-12345"))
                    .andExpect(jsonPath("$.data.tokenExpireSeconds").value(3600))
                    .andExpect(jsonPath("$.data.websocket.sockjs").value("ws://localhost:8080/sockjs-vm"))
                    .andExpect(jsonPath("$.data.websocket.native").value("ws://localhost:8080/vm-websocket"))
                    .andExpect(jsonPath("$.data.apiEndpoints.status").value("/api/v1/vm/status"))
                    .andExpect(jsonPath("$.data.apiEndpoints.control").value("/api/v1/vm/control"))
                    .andExpect(jsonPath("$.data.apiEndpoints.tokenRefresh").value("/api/v1/vm/token/refresh"));

            // 验证服务调用
            verify(vmInstanceService).register(any(VmRegisterDTO.class));
        }
    }

    /**
     * 测试VM注册 - 服务异常
     */
    @Test
    void testVmRegister_ServiceException() throws Exception {
        // Mock服务层抛出异常
        when(vmInstanceService.register(any(VmRegisterDTO.class)))
                .thenThrow(new RuntimeException("VM ID已存在"));

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(() -> IpUtil.getClientIpAddress(any()))
                    .thenReturn("127.0.0.1");

            mockMvc.perform(post("/api/v1/vm/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));

            // 验证服务调用
            verify(vmInstanceService).register(any(VmRegisterDTO.class));
        }
    }

    /**
     * 测试VM注册 - 请求参数验证失败
     */
    @Test
    void testVmRegister_ValidationFailed() throws Exception {
        // 创建无效的注册DTO
        VmRegisterDTO invalidDTO = new VmRegisterDTO();
        invalidDTO.setVmId("invalid"); // 无效的VM ID格式
        invalidDTO.setName("");        // 空名称
        invalidDTO.setIpAddress("invalid-ip"); // 无效IP地址
        invalidDTO.setPort(-1);        // 无效端口
        invalidDTO.setCpuCores(0);     // 无效CPU核心数
        invalidDTO.setMemoryMb(100);   // 内存太小
        invalidDTO.setDiskGb(5);       // 磁盘太小

        mockMvc.perform(post("/api/v1/vm/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        // 服务不应该被调用
        verify(vmInstanceService, never()).register(any(VmRegisterDTO.class));
    }

    /**
     * 测试Token刷新 - 成功场景
     */
    @Test
    void testTokenRefresh_Success() throws Exception {
        // Mock服务层方法
        when(vmInstanceService.refreshToken(any(VmTokenRefreshDTO.class)))
                .thenReturn(tokenRefreshResponse);

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(() -> IpUtil.getClientIpAddress(any()))
                    .thenReturn("127.0.0.1");

            mockMvc.perform(post("/api/v1/vm/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validTokenRefreshDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("刷新成功"))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token-12345"))
                    .andExpect(jsonPath("$.data.tokenExpireSeconds").value(3600))
                    .andExpect(jsonPath("$.data.secretId").value("new-secret-id-12345"));

            // 验证服务调用
            verify(vmInstanceService).refreshToken(any(VmTokenRefreshDTO.class));
        }
    }

    /**
     * 测试Token刷新 - 服务异常
     */
    @Test
    void testTokenRefresh_ServiceException() throws Exception {
        // Mock服务层抛出异常
        when(vmInstanceService.refreshToken(any(VmTokenRefreshDTO.class)))
                .thenThrow(new RuntimeException("无效的刷新凭证"));

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(() -> IpUtil.getClientIpAddress(any()))
                    .thenReturn("127.0.0.1");

            mockMvc.perform(post("/api/v1/vm/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validTokenRefreshDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));

            // 验证服务调用
            verify(vmInstanceService).refreshToken(any(VmTokenRefreshDTO.class));
        }
    }

    /**
     * 测试Token刷新 - 验证失败
     */
    @Test
    void testTokenRefresh_ValidationFailed() throws Exception {
        // 创建无效的刷新DTO
        VmTokenRefreshDTO invalidDTO = new VmTokenRefreshDTO();
        invalidDTO.setVmId("invalid"); // 无效的VM ID格式
        invalidDTO.setSecretId("");    // 空的secret ID

        mockMvc.perform(post("/api/v1/vm/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        // 服务不应该被调用
        verify(vmInstanceService, never()).refreshToken(any(VmTokenRefreshDTO.class));
    }

    /**
     * 测试心跳接口 - 成功场景
     */
    @Test
    void testHeartbeat_Success() throws Exception {
        String vmId = "12345678901234567890123456789012";
        
        // Mock服务层方法
        doNothing().when(vmInstanceService).updateHeartbeat(vmId);

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(() -> IpUtil.getClientIpAddress(any()))
                    .thenReturn("127.0.0.1");

            mockMvc.perform(post("/api/v1/vm/{vmId}/heartbeat", vmId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("心跳更新成功"));

            // 验证服务调用
            verify(vmInstanceService).updateHeartbeat(vmId);
        }
    }

    /**
     * 测试心跳接口 - 服务异常
     */
    @Test
    void testHeartbeat_ServiceException() throws Exception {
        String vmId = "12345678901234567890123456789012";
        
        // Mock服务层抛出异常
        doThrow(new RuntimeException("VM不存在"))
                .when(vmInstanceService).updateHeartbeat(vmId);

        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(() -> IpUtil.getClientIpAddress(any()))
                    .thenReturn("127.0.0.1");

            mockMvc.perform(post("/api/v1/vm/{vmId}/heartbeat", vmId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500));

            // 验证服务调用
            verify(vmInstanceService).updateHeartbeat(vmId);
        }
    }

    /**
     * 测试健康检查接口
     */
    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/api/v1/vm/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("虚拟机服务运行正常"))
                .andExpect(jsonPath("$.data").value("VM_SERVICE_UP"));
    }

    /**
     * 测试不支持的HTTP方法
     */
    @Test
    void testUnsupportedHttpMethods() throws Exception {
        // 注册端点只支持POST
        mockMvc.perform(get("/api/v1/vm/register"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        mockMvc.perform(put("/api/v1/vm/register"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        // Token刷新端点只支持POST
        mockMvc.perform(get("/api/v1/vm/token/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        mockMvc.perform(delete("/api/v1/vm/token/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        // 心跳端点只支持POST
        mockMvc.perform(get("/api/v1/vm/test-vm-id/heartbeat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        // 健康检查只支持GET
        mockMvc.perform(post("/api/v1/vm/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 测试CORS头信息
     */
    @Test
    void testCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/vm/health")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "*"));
    }

    /**
     * 测试请求头处理
     */
    @Test
    void testRequestHeaders() throws Exception {
        try (MockedStatic<IpUtil> ipUtilMock = mockStatic(IpUtil.class)) {
            ipUtilMock.when(() -> IpUtil.getClientIpAddress(any()))
                    .thenReturn("192.168.1.100");

            mockMvc.perform(post("/api/v1/vm/test-vm-id/heartbeat")
                            .header("User-Agent", "FedUWAComm-VM/1.0")
                            .header("X-Forwarded-For", "192.168.1.100"))
                    .andExpect(status().isOk());

            // 验证IP获取方法被调用：LoggingInterceptor.preHandle + Controller.heartbeat + LoggingInterceptor.afterCompletion = 3次
            ipUtilMock.verify(() -> IpUtil.getClientIpAddress(any()), times(3));
        }
    }

    /**
     * 测试JSON格式错误处理
     */
    @Test
    void testInvalidJsonFormat() throws Exception {
        String invalidJson = "{invalid-json}";

        mockMvc.perform(post("/api/v1/vm/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500)); // 全局异常处理器返回500
    }

    /**
     * 测试缺少Content-Type头
     */
    @Test
    void testMissingContentType() throws Exception {
        // 缺少Content-Type会被全局异常处理器处理，返回200状态码
        mockMvc.perform(post("/api/v1/vm/register")
                        .content(objectMapper.writeValueAsString(validRegisterDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500)); // 全局异常处理器返回错误码500
    }
}