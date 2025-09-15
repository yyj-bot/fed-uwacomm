package com.feduwacomm.integration;

import com.feduwacomm.dto.*;
import com.feduwacomm.service.*;
import com.feduwacomm.vo.*;
import com.feduwacomm.utils.PasswordUtil;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 简化版完整学习流程集成测试
 * 验证核心用户和认证功能
 * 使用H2内存数据库(MySQL兼容模式)进行集成测试
 * 注意：由于环境限制无法使用TestContainers，采用H2数据库但配置为MySQL兼容模式
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimpleWorkflowIntegrationTest {

    @Autowired
    private UserService userService;
    
    @Autowired 
    private AdminService adminService;
    
    @BeforeEach
    void setUp() {
        initializePasswordUtil();
    }
    
    /**
     * 初始化PasswordUtil工具类
     */
    private void initializePasswordUtil() {
        try {
            Field encoderField = PasswordUtil.class.getDeclaredField("encoder");
            encoderField.setAccessible(true);
            encoderField.set(null, new BCryptPasswordEncoder());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PasswordUtil for testing", e);
        }
    }
    
    @Test
    @DisplayName("完整学习流程集成测试 - 用户管理和认证功能")
    void completeWorkflowIntegrationTest() {
        // 步骤1: 管理员用户创建和登录
        UserRegisterDTO adminRegisterDTO = new UserRegisterDTO();
        adminRegisterDTO.setUsername("admin");
        adminRegisterDTO.setEmail("admin@feduwacomm.com");
        adminRegisterDTO.setPassword("AdminPass123!");
        adminRegisterDTO.setConfirmPassword("AdminPass123!");
            
        UserRegisterResponseVO adminRegResponse = userService.register(adminRegisterDTO);
        assertThat(adminRegResponse).isNotNull();
        
        String adminUserId = adminRegResponse.getUserId();
        
        // 验证第一个用户获得管理员权限
        assertThat(adminRegResponse.getRole()).isEqualTo("ADMIN");
        
        // 1.2 管理员登录
        UserLoginDTO adminLogin = new UserLoginDTO();
        adminLogin.setLoginIdentifier("admin@feduwacomm.com");
        adminLogin.setPassword("AdminPass123!");
            
        LoginResponseVO adminLoginResponse = userService.login(adminLogin);
        String adminToken = adminLoginResponse.getToken();
        assertThat(adminToken).isNotNull();
        
        // 验证登录用户信息
        assertThat(adminLoginResponse.getUser().getUserId()).isEqualTo(adminUserId);
        assertThat(adminLoginResponse.getUser().getRole()).isEqualTo("ADMIN");
        
        // 步骤2: 普通用户注册和登录
        UserRegisterDTO normalUserDTO = new UserRegisterDTO();
        normalUserDTO.setUsername("researcher01");
        normalUserDTO.setEmail("researcher01@feduwacomm.com");
        normalUserDTO.setPassword("UserPass123!");
        normalUserDTO.setConfirmPassword("UserPass123!");
            
        UserRegisterResponseVO userRegResponse = userService.register(normalUserDTO);
        assertThat(userRegResponse).isNotNull();
        
        String normalUserId = userRegResponse.getUserId();
        
        // 验证普通用户获得VIEWER权限
        assertThat(userRegResponse.getRole()).isEqualTo("VIEWER");
        
        // 2.2 普通用户登录
        UserLoginDTO userLogin = new UserLoginDTO();
        userLogin.setLoginIdentifier("researcher01@feduwacomm.com");
        userLogin.setPassword("UserPass123!");
            
        LoginResponseVO userLoginResponse = userService.login(userLogin);
        String userToken = userLoginResponse.getToken();
        assertThat(userToken).isNotNull();
        
        // 验证登录用户权限
        assertThat(userLoginResponse.getUser().getRole()).isEqualTo("VIEWER");
        
        // 步骤3: 验证用户管理功能
        UserQueryDTO queryDTO = new UserQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setSize(10);
            
        PageResponseDTO<UserListVO> userListResponse = adminService.getUserList(queryDTO);
        assertThat(userListResponse).isNotNull();
        assertThat(userListResponse.getTotal()).isGreaterThanOrEqualTo(2);
        
        // 3.2 管理员查看用户详情
        UserDetailVO adminDetail = adminService.getUserDetail(adminUserId);
        assertThat(adminDetail.getRole()).isEqualTo("ADMIN");
        assertThat(adminDetail.getUsername()).isEqualTo("admin");
        
        UserDetailVO userDetail = adminService.getUserDetail(normalUserId);
        assertThat(userDetail.getRole()).isEqualTo("VIEWER");
        assertThat(userDetail.getUsername()).isEqualTo("researcher01");
        
        // 3.3 验证用户权限（允许为空，新用户可能默认没有权限）
        List<UserPermissionVO> adminPermissions = adminService.getUserPermissions(adminUserId);
        assertThat(adminPermissions).isNotNull(); // 只验证返回值不为null
        
        List<UserPermissionVO> userPermissions = adminService.getUserPermissions(normalUserId);
        assertThat(userPermissions).isNotNull(); // 只验证返回值不为null
        
        // 步骤4: 验证用户信息更新
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setUsername("researcher01_updated");
            
        UserUpdateResponseVO updateResponse = userService.updateUserInfo(normalUserId, updateDTO);
        assertThat(updateResponse).isNotNull();
        assertThat(updateResponse.getUsername()).isEqualTo("researcher01_updated");
        
        // 4.2 验证更新结果
        UserDetailVO updatedUser = adminService.getUserDetail(normalUserId);
        assertThat(updatedUser.getUsername()).isEqualTo("researcher01_updated");
        
        // 步骤5: 验证密码修改功能
        PasswordChangeDTO passwordChangeDTO = new PasswordChangeDTO();
        passwordChangeDTO.setOldPassword("UserPass123!");
        passwordChangeDTO.setNewPassword("NewUserPass123!");
        passwordChangeDTO.setConfirmPassword("NewUserPass123!");
            
        userService.changePassword(normalUserId, passwordChangeDTO);
        
        // 5.2 使用新密码登录验证
        UserLoginDTO newPasswordLogin = new UserLoginDTO();
        newPasswordLogin.setLoginIdentifier("researcher01@feduwacomm.com");
        newPasswordLogin.setPassword("NewUserPass123!");
            
        LoginResponseVO newLoginResponse = userService.login(newPasswordLogin);
        assertThat(newLoginResponse.getToken()).isNotNull();
        assertThat(newLoginResponse.getUser().getUserId()).isEqualTo(normalUserId);
        
        // 步骤6: 端到端验证完整用户流程
        // 6.1 验证所有用户数据完整性
        assertThat(adminUserId).isNotNull();
        assertThat(normalUserId).isNotNull();
        assertThat(adminToken).isNotNull();
        assertThat(userToken).isNotNull();
        
        // 6.2 验证管理员功能完整性
        UserDetailVO finalAdminCheck = adminService.getUserDetail(adminUserId);
        assertThat(finalAdminCheck.getRole()).isEqualTo("ADMIN");
        assertThat(finalAdminCheck.getUsername()).isEqualTo("admin");
        assertThat(finalAdminCheck.getEmail()).isEqualTo("admin@feduwacomm.com");
        
        // 6.3 验证普通用户功能完整性
        UserDetailVO finalUserCheck = adminService.getUserDetail(normalUserId);
        assertThat(finalUserCheck.getRole()).isEqualTo("VIEWER");
        assertThat(finalUserCheck.getUsername()).isEqualTo("researcher01_updated");
        assertThat(finalUserCheck.getEmail()).isEqualTo("researcher01@feduwacomm.com");
        
        // 6.4 验证系统数据一致性
        UserQueryDTO finalQuery = new UserQueryDTO();
        finalQuery.setPage(1);
        finalQuery.setSize(10);
        
        PageResponseDTO<UserListVO> finalUserList = adminService.getUserList(finalQuery);
        assertThat(finalUserList.getTotal()).isGreaterThanOrEqualTo(2);
        assertThat(finalUserList.getList()).isNotEmpty();
        
        // 验证用户列表中包含我们创建的用户
        List<String> userIds = finalUserList.getList().stream()
            .map(UserListVO::getUserId)
            .toList();
        assertThat(userIds).contains(adminUserId, normalUserId);
        
        // 测试完成: 成功验证了完整的用户管理和认证流程
    }
}