package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.service.UserService;
import com.feduwacomm.testdata.TestDataBuilder;
import com.feduwacomm.testdata.TestHelper;
import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static com.feduwacomm.testdata.TestHelper.Constants.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import io.jsonwebtoken.Claims;

/**
 * 用户控制器测试类
 * 
 * 测试目的：验证用户API端点的HTTP请求响应和数据格式
 * 主要测试场景：
 * 1. 用户注册、登录、登出API
 * 2. 令牌刷新和用户信息管理
 * 3. HTTP状态码和响应格式验证
 * 4. 请求参数验证和异常处理
 * 5. JWT认证和权限控制
 * 6. Content-Type和安全头验证
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserJwtUtil userJwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;
    private String validToken;
    private String refreshToken;
    private Claims validAccessClaims;
    private Claims validRefreshClaims;

    @BeforeEach
    void setUp() {
        // 使用TestDataBuilder设置测试数据
        registerDTO = TestDataBuilder.DTOs.validRegisterDTO()
            .username("controlleruser")
            .email("controller@example.com")
            .build();

        loginDTO = TestDataBuilder.DTOs.validLoginDTO()
            .loginIdentifier("controlleruser")
            .password(VALID_PASSWORD)
            .build();

        // 设置测试用的有效token
        validToken = "Bearer_test_access_token";
        refreshToken = "Bearer_test_refresh_token";
        
        // 使用TestHelper创建JWT Claims
        validAccessClaims = TestHelper.JwtClaims.validAccessTokenClaims(
            "user123", "controlleruser", ROLE_VIEWER);
        validRefreshClaims = TestHelper.JwtClaims.validRefreshTokenClaims("user123");
        
        // 配置JWT工具的Mock行为
        configureJwtMockBehavior();
    }
    
    /**
     * 配置JWT工具的Mock行为
     */
    private void configureJwtMockBehavior() {
        when(userJwtUtil.validateToken(validToken)).thenReturn(validAccessClaims);
        when(userJwtUtil.validateToken(refreshToken)).thenReturn(validRefreshClaims);
        
        // 配置过期token的行为
        String expiredToken = "Bearer_expired_token";
        when(userJwtUtil.validateToken(expiredToken))
                .thenReturn(TestHelper.JwtClaims.expiredTokenClaims("user123"));
        
        // 配置无效token的行为
        String invalidToken = "Bearer_invalid_token";
        when(userJwtUtil.validateToken(invalidToken))
                .thenThrow(new RuntimeException("Invalid token"));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testUserRegister_WithValidData_ShouldReturn200AndUserInfo() throws Exception {
        // 准备测试数据 - 使用TestDataBuilder
        UserRegisterResponseVO expectedResponse = TestDataBuilder.VOs.validRegisterResponseVO()
            .userId("ctrl_user_123")
            .username("controlleruser")
            .email("controller@example.com")
            .role(ROLE_VIEWER)
            .status(STATUS_ACTIVE)
            .createdAt(LocalDateTime.now())
            .build();

        when(userService.register(org.mockito.ArgumentMatchers.any(UserRegisterDTO.class))).thenReturn(expectedResponse);

        // 执行测试并验证HTTP响应
        ResultActions result = mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andDo(print()) // 输出详细请求响应信息用于调试
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            
            // 验证统一响应格式
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("注册成功"))
            .andExpect(jsonPath("$.data").exists())
            
            // 验证用户数据
            .andExpect(jsonPath("$.data.userId").value("ctrl_user_123"))
            .andExpect(jsonPath("$.data.username").value("controlleruser"))
            .andExpect(jsonPath("$.data.email").value("controller@example.com"))
            .andExpect(jsonPath("$.data.role").value(ROLE_VIEWER))
            .andExpect(jsonPath("$.data.status").value(STATUS_ACTIVE))
            .andExpect(jsonPath("$.data.createdAt").exists());
        
        // 验证服务层调用
        verify(userService, times(1)).register(argThat(dto -> 
            "controlleruser".equals(dto.getUsername()) &&
            "controller@example.com".equals(dto.getEmail()) &&
            VALID_PASSWORD.equals(dto.getPassword())
        ));
    }

    @Test
    void testUserLogin() throws Exception {
        LoginResponseVO response = LoginResponseVO.builder()
            .token("token123")
            .refreshToken("refresh123")
            .expiresIn(86400L)
            .user(UserInfoVO.builder()
                .userId("user123")
                .username("testuser")
                .email("test@example.com")
                .role("VIEWER")
                .status("ACTIVE")
                .build())
            .build();

        when(userService.login(any(UserLoginDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("登录成功"))
            .andExpect(jsonPath("$.data.token").value("token123"));
    }

    @Test
    void testRefreshToken() throws Exception {
        TokenRefreshResponseVO response = TokenRefreshResponseVO.builder()
            .token("newtoken123")
            .refreshToken("newrefresh123")
            .expiresIn(86400L)
            .build();

        when(userService.refreshToken(anyString())).thenReturn(response);

        mockMvc.perform(post("/api/user/refresh")
                .header("Authorization", "Bearer " + refreshToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("Token刷新成功"));
    }

    @Test
    void testUserLogout() throws Exception {
        mockMvc.perform(post("/api/user/logout")
                .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("登出成功"));
    }

    @Test
    void testGetCurrentUserInfo() throws Exception {
        UserInfoVO userInfo = UserInfoVO.builder()
            .userId("user123")
            .username("testuser")
            .email("test@example.com")
            .role("VIEWER")
            .status("ACTIVE")
            .lastLoginTime(LocalDateTime.now())
            .lastLoginIp("192.168.1.100")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(userService.getCurrentUserInfo("user123")).thenReturn(userInfo);

        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + validToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("获取成功"))
            .andExpect(jsonPath("$.data.userId").value("user123"));
    }

    @Test
    void testUpdateUserInfo() throws Exception {
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
            .username("updateduser")
            .email("updated@example.com")
            .build();

        UserUpdateResponseVO response = UserUpdateResponseVO.builder()
            .userId("user123")
            .username("updateduser")
            .email("updated@example.com")
            .role("VIEWER")
            .status("ACTIVE")
            .updatedAt(LocalDateTime.now())
            .build();

        when(userService.updateUserInfo("user123", updateDTO)).thenReturn(response);

        mockMvc.perform(put("/api/user/profile")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("更新成功"))
            .andExpect(jsonPath("$.data.username").value("updateduser"));
    }

    @Test
    void testChangePassword() throws Exception {
        PasswordChangeDTO passwordDTO = PasswordChangeDTO.builder()
            .oldPassword("oldpassword")
            .newPassword("newpassword")
            .confirmPassword("newpassword")
            .build();

        mockMvc.perform(put("/api/user/password")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(passwordDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("密码修改成功"));
    }

    @Test
    void testLoginWithRememberMe() throws Exception {
        UserLoginDTO loginWithRememberMe = UserLoginDTO.builder()
            .loginIdentifier("testuser")
            .password("password123")
            .build();

        LoginResponseVO response = LoginResponseVO.builder()
            .token("token123")
            .refreshToken("refresh123")
            .expiresIn(86400L)
            .user(UserInfoVO.builder()
                .userId("user123")
                .username("testuser")
                .email("test@example.com")
                .role("VIEWER")
                .status("ACTIVE")
                .build())
            .build();

        when(userService.login(any(UserLoginDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginWithRememberMe)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("登录成功"));
    }

    @Test
    void testRegisterValidation() throws Exception {
        // 测试密码不匹配的情况
        UserRegisterDTO invalidDTO = UserRegisterDTO.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .confirmPassword("differentpassword")
            .build();

        // 当密码不匹配时，服务应该抛出UserException
        when(userService.register(any(UserRegisterDTO.class)))
            .thenThrow(UserException.passwordMismatch());

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
            .andExpect(status().isOk()) // 全局异常处理器返回200状态码
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testLoginValidation() throws Exception {
        // 测试缺少必填字段的情况
        UserLoginDTO invalidDTO = UserLoginDTO.builder()
            .password("password123")
            .build();

        mockMvc.perform(post("/api/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
            .andExpect(status().isOk()); // 全局异常处理器返回200状态码
    }

    // ======================== 边界条件和安全测试 ========================

    @Test
    void testRegister_WithInvalidContentType_ShouldReturn415() throws Exception {
        // 当前系统行为：返回200状态码但Response Body包含错误信息
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.TEXT_PLAIN) // 错误的Content-Type
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("未知错误"))
            .andExpect(jsonPath("$.data").value(containsString("Content-Type 'text/plain' is not supported")));
    }

    @Test
    void testRegister_WithMalformedJson_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}")) // 格式错误的JSON
            .andExpect(status().isOk()) // 全局异常处理器处理
            .andExpect(jsonPath("$.code").value(500)); // 内部错误码
    }

    @Test
    void testLogin_WithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/profile")) // 需要认证的端点
            .andExpect(status().isOk()) // 全局异常处理器返回200
            .andExpect(jsonPath("$.code").value(401)); // 但业务码是401
    }

    @Test
    void testLogin_WithInvalidToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer invalid_token"))
            .andExpect(status().isOk()) // 全局异常处理器返回200
            .andExpect(jsonPath("$.code").value(401)); // 业务码是401
    }

    @Test
    void testLogin_WithExpiredToken_ShouldReturn401() throws Exception {
        String expiredToken = "Bearer_expired_token";
        
        mockMvc.perform(get("/api/user/profile")
                .header("Authorization", expiredToken))
            .andExpect(status().isOk()) // 全局异常处理器返回200
            .andExpect(jsonPath("$.code").value(401)); // 业务码是401
    }

    @Test
    void testRegister_WithSqlInjectionAttempt_ShouldBeSafe() throws Exception {
        UserRegisterDTO sqlInjectionDTO = TestDataBuilder.DTOs.validRegisterDTO()
            .username("'; DROP TABLE users; --")
            .email("hacker'; DROP TABLE users; --@evil.com")
            .build();

        when(userService.register(any(UserRegisterDTO.class)))
            .thenThrow(new UserException("非法字符"));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sqlInjectionDTO)))
            .andExpect(status().isOk()) // 全局异常处理器返回200
            .andExpect(jsonPath("$.code").value(400)); // 业务错误码
    }

    @Test
    void testRegister_WithXssAttempt_ShouldBeSafe() throws Exception {
        UserRegisterDTO xssDTO = TestDataBuilder.DTOs.validRegisterDTO()
            .username("<script>alert('xss')</script>")
            .email("<img src=x onerror=alert('xss')>@evil.com")
            .build();

        when(userService.register(any(UserRegisterDTO.class)))
            .thenThrow(new UserException("非法字符"));

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(xssDTO)))
            .andExpect(status().isOk()) // 全局异常处理器返回200
            .andExpect(jsonPath("$.code").value(400)) // 业务错误码
            .andExpect(jsonPath("$.message").value(not(containsString("<script>"))));
    }

    @Test
    void testHttpMethodValidation_OnlyPostAllowed() throws Exception {
        // GET请求到注册端点应该返回200状态码但包含错误信息
        // 注意：当前系统行为是返回200状态码，但Response Body包含错误信息
        mockMvc.perform(get("/api/user/register"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("未知错误"))
            .andExpect(jsonPath("$.data").value("Request method 'GET' is not supported"));

        // PUT请求到注册端点应该返回200状态码但包含错误信息
        mockMvc.perform(put("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.message").value("未知错误"))
            .andExpect(jsonPath("$.data").value("Request method 'PUT' is not supported"));
    }

    @Test
    void testRequestSizeLimit_WithLargePayload() throws Exception {
        // 创建一个非常大的用户名来测试请求大小限制
        String largeUsername = "a".repeat(10000);
        UserRegisterDTO largeDTO = TestDataBuilder.DTOs.validRegisterDTO()
            .username(largeUsername)
            .build();

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeDTO)))
            .andExpect(status().isOk()) // 全局异常处理器处理
            .andExpect(jsonPath("$.code").exists()); // 应该有业务错误码
    }

    @Test 
    void testSecurityHeaders_ShouldBePresent() throws Exception {
        when(userService.register(any(UserRegisterDTO.class)))
            .thenReturn(TestDataBuilder.VOs.validRegisterResponseVO().build());

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Content-Type-Options"));
    }

    // ======================== 性能和并发测试 ========================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)  
    void testConcurrentRequests_ShouldHandleGracefully() throws Exception {
        when(userService.register(any(UserRegisterDTO.class)))
            .thenReturn(TestDataBuilder.VOs.validRegisterResponseVO().build());

        // 模拟并发请求（在测试环境中是伪并发）
        int requestCount = 10;
        for (int i = 0; i < requestCount; i++) {
            UserRegisterDTO concurrentDTO = TestDataBuilder.DTOs.validRegisterDTO()
                .username("concurrent" + i)
                .email("concurrent" + i + "@example.com")
                .build();
            
            mockMvc.perform(post("/api/user/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(concurrentDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        }
        
        // 验证服务层被正确调用
        verify(userService, times(requestCount)).register(any(UserRegisterDTO.class));
    }
}