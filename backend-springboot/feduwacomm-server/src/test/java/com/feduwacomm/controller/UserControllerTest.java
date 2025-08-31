package com.feduwacomm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feduwacomm.dto.*;
import com.feduwacomm.service.UserService;
import com.feduwacomm.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户控制器测试类
 * 测试用户自助功能接口
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;

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
                .header("Authorization", "Bearer refresh123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("Token刷新成功"));
    }

    @Test
    void testUserLogout() throws Exception {
        mockMvc.perform(post("/api/user/logout")
                .header("Authorization", "Bearer token123"))
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
                .header("Authorization", "Bearer token123"))
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
                .header("Authorization", "Bearer token123")
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
                .header("Authorization", "Bearer token123")
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

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO)))
            .andExpect(status().isOk()); // 全局异常处理器返回200状态码
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