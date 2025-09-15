package com.feduwacomm.utils;

import com.feduwacomm.exception.UserException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordUtil 测试类
 */
@SpringBootTest
@ActiveProfiles("test")
public class PasswordUtilTest {

    @Test
    void testValidPasswordFormat_Success() {
        // 测试有效密码
        assertDoesNotThrow(() -> PasswordUtil.validatePasswordFormat("password123"));
        assertDoesNotThrow(() -> PasswordUtil.validatePasswordFormat("abc123def"));
        assertDoesNotThrow(() -> PasswordUtil.validatePasswordFormat("Test123"));
    }

    @Test
    void testValidPasswordFormat_TooShort() {
        // 测试密码长度不足
        UserException exception = assertThrows(UserException.class, () -> 
            PasswordUtil.validatePasswordFormat("12345"));
        assertEquals("密码格式不正确", exception.getMessage());
    }

    @Test
    void testValidPasswordFormat_TooLong() {
        // 测试密码长度过长
        UserException exception = assertThrows(UserException.class, () -> 
            PasswordUtil.validatePasswordFormat("a".repeat(21) + "123"));
        assertEquals("密码格式不正确", exception.getMessage());
    }

    @Test
    void testValidPasswordFormat_MissingLetter() {
        // 测试缺少字母
        UserException exception = assertThrows(UserException.class, () -> 
            PasswordUtil.validatePasswordFormat("123456"));
        assertEquals("密码格式不正确", exception.getMessage());
    }

    @Test
    void testValidPasswordFormat_MissingDigit() {
        // 测试缺少数字
        UserException exception = assertThrows(UserException.class, () -> 
            PasswordUtil.validatePasswordFormat("abcdef"));
        assertEquals("密码格式不正确", exception.getMessage());
    }

    @Test
    void testValidPasswordFormat_NullPassword() {
        // 测试空密码
        UserException exception = assertThrows(UserException.class, () -> 
            PasswordUtil.validatePasswordFormat(null));
        assertEquals("密码格式不正确", exception.getMessage());
    }

    @Test
    void testValidPasswordFormat_EmptyPassword() {
        // 测试空字符串密码
        UserException exception = assertThrows(UserException.class, () -> 
            PasswordUtil.validatePasswordFormat(""));
        assertEquals("密码格式不正确", exception.getMessage());
    }

    @Test
    void testValidPasswordFormat_WithSpecialChars() {
        // 测试包含特殊字符的有效密码
        assertDoesNotThrow(() -> PasswordUtil.validatePasswordFormat("password123!"));
        assertDoesNotThrow(() -> PasswordUtil.validatePasswordFormat("test@123"));
        assertDoesNotThrow(() -> PasswordUtil.validatePasswordFormat("Pass#123"));
    }

    @Test
    void testIsValidPassword_BackwardCompatibility() {
        // 测试向后兼容性
        assertTrue(PasswordUtil.isValidPassword("password123"));
        assertTrue(PasswordUtil.isValidPassword("abc123def"));
        assertFalse(PasswordUtil.isValidPassword("123456"));
        assertFalse(PasswordUtil.isValidPassword("abcdef"));
        assertFalse(PasswordUtil.isValidPassword("12345"));
        assertFalse(PasswordUtil.isValidPassword("a".repeat(21) + "123"));
        assertFalse(PasswordUtil.isValidPassword(null));
        assertFalse(PasswordUtil.isValidPassword(""));
    }
}