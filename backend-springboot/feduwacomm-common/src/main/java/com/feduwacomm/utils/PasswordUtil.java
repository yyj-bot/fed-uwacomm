package com.feduwacomm.utils;

import com.feduwacomm.exception.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

/**
 * 密码加密工具类
 */
@Component
public class PasswordUtil {

    private static BCryptPasswordEncoder encoder;

    @Autowired
    public void setBCryptPasswordEncoder(BCryptPasswordEncoder encoder) {
        PasswordUtil.encoder = encoder;
    }

    /**
     * 加密密码
     */
    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }

    // 密码规则常量
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 20;
    
    // 正则表达式模式
    private static final Pattern LETTER_PATTERN = Pattern.compile(".*[a-zA-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*[0-9].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    private static final Pattern INVALID_CHAR_PATTERN = Pattern.compile(".*[^a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?\\s].*");

    /**
     * 验证密码强度（简单版本，保持向后兼容）
     */
    public static boolean isValidPassword(String password) {
        try {
            validatePasswordFormat(password);
            return true;
        } catch (UserException e) {
            return false;
        }
    }

    /**
     * 详细的密码格式验证，抛出具体的异常
     */
    public static void validatePasswordFormat(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw UserException.passwordTooWeak();
        }

        // 检查长度
        if (password.length() < MIN_LENGTH) {
            throw UserException.passwordTooShort(MIN_LENGTH);
        }
        if (password.length() > MAX_LENGTH) {
            throw UserException.passwordTooLong(MAX_LENGTH);
        }

        // 检查是否包含不允许的字符
        if (INVALID_CHAR_PATTERN.matcher(password).matches()) {
            throw UserException.passwordInvalidCharacters();
        }

        // 检查必须包含的字符类型
        boolean hasLetter = LETTER_PATTERN.matcher(password).matches();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).matches();

        if (!hasLetter) {
            throw UserException.passwordMissingLetter();
        }
        if (!hasDigit) {
            throw UserException.passwordMissingDigit();
        }

        // 基本验证通过
    }

    /**
     * 强密码验证（可选的更严格验证）
     */
    public static void validateStrongPassword(String password) {
        // 先进行基本验证
        validatePasswordFormat(password);

        // 附加的强密码检查
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).matches();
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).matches();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).matches();

        // 这些是建议性的，不强制要求，但可以根据需要启用
        if (!hasUppercase) {
            // 可以抛出 UserException.passwordMissingUppercase() 
            // 但当前保持宽松策略，仅做基本验证
        }
        if (!hasLowercase) {
            // 可以抛出 UserException.passwordMissingLowercase()
        }
        if (!hasSpecialChar) {
            // 可以抛出 UserException.passwordMissingSpecialChar()
        }
    }
}