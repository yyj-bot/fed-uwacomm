package com.feduwacomm.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key工具类
 * 用于生成和验证VM的API Key
 *
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class ApiKeyUtil {

    private static BCryptPasswordEncoder encoder;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public void setBCryptPasswordEncoder(BCryptPasswordEncoder encoder) {
        ApiKeyUtil.encoder = encoder;
    }
    
    // API Key长度（字节）
    private static final int API_KEY_LENGTH = 48; // 生成64字符的Base64字符串
    
    // API Key前缀，便于识别
    private static final String API_KEY_PREFIX = "fua_";

    /**
     * 生成安全的API Key
     * 生成格式：fua_<64位Base64字符串>
     *
     * @return 明文API Key
     */
    public static String generateApiKey() {
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String base64Key = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return API_KEY_PREFIX + base64Key;
    }

    /**
     * 对API Key进行BCrypt加密
     *
     * @param rawApiKey 明文API Key
     * @return BCrypt加密后的哈希值
     */
    public static String encodeApiKey(String rawApiKey) {
        return encoder.encode(rawApiKey);
    }

    /**
     * 验证API Key
     *
     * @param rawApiKey 明文API Key
     * @param encodedApiKey BCrypt加密后的API Key
     * @return 验证是否通过
     */
    public static boolean matches(String rawApiKey, String encodedApiKey) {
        return encoder.matches(rawApiKey, encodedApiKey);
    }

    /**
     * 验证API Key格式是否正确
     *
     * @param apiKey API Key
     * @return 格式是否正确
     */
    public static boolean isValidFormat(String apiKey) {
        if (apiKey == null || !apiKey.startsWith(API_KEY_PREFIX)) {
            return false;
        }
        
        // 移除前缀后应该是64字符的Base64字符串
        String keyPart = apiKey.substring(API_KEY_PREFIX.length());
        return keyPart.length() == 64 && keyPart.matches("^[A-Za-z0-9_-]+$");
    }
}