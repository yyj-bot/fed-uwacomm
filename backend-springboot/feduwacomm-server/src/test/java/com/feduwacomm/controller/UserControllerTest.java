package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.exception.UserException;
import com.feduwacomm.service.UserService;
import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;

/**
 * 用户控制器测试类
 * 测试用户自助功能接口
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
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

    @BeforeEach
    void setUp() {
        // 设置测试数据
        registerDTO = UserRegisterDTO.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .confirmPassword("password123")
            .build();

        loginDTO = UserLoginDTO.builder()
            .loginIdentifier("testuser")
            .password("password123")
            .captcha("1234")
            .captchaKey("key123")
            .rememberMe(false)
            .build();

        // 设置测试用的有效token
        validToken = "token123";
        refreshToken = "refresh123";
        
        // Mock JWT验证，返回有效的access token Claims
        Claims accessClaims = new DefaultClaims();
        accessClaims.put("userId", "user123");
        accessClaims.put("username", "testuser");
        accessClaims.put("role", "VIEWER");
        accessClaims.put("type", "access");
        
        // Mock JWT验证，返回有效的refresh token Claims
        Claims refreshClaims = new DefaultClaims();
        refreshClaims.put("userId", "user123");
        refreshClaims.put("type", "refresh");
        
        when(userJwtUtil.validateToken(validToken)).thenReturn(accessClaims);
        when(userJwtUtil.validateToken(refreshToken)).thenReturn(refreshClaims);
    }

    @Test
    void testUserRegister() throws Exception {
        UserRegisterResponseVO response = UserRegisterResponseVO.builder()
            .userId("user123")
            .username("testuser")
            .email("test@example.com")
            .role("VIEWER")
            .status("ACTIVE")
            .createdAt(LocalDateTime.now())
            .build();

        when(userService.register(any(UserRegisterDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("注册成功"))
            .andExpect(jsonPath("$.data.userId").value("user123"));
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
    void testLoginWithCaptcha() throws Exception {
        UserLoginDTO loginWithCaptcha = UserLoginDTO.builder()
            .loginIdentifier("testuser")
            .password("password123")
            .captcha("1234")
            .captchaKey("key123")
            .rememberMe(true)
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
                .content(objectMapper.writeValueAsString(loginWithCaptcha)))
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
}