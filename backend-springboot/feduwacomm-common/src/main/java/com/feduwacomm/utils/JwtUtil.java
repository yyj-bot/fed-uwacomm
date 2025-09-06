package com.feduwacomm.utils;

import com.feduwacomm.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtConfig jwtConfig;
    
    private SecretKey key;

    /**
     * 初始化密钥
     */
    private SecretKey getKey() {
        if (key == null) {
            this.key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes());
        }
        return key;
    }

    /**
     * 生成访问Token
     */
    public String generateToken(String userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", "access");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration() * 1000))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成刷新Token
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshExpiration() * 1000))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证Token
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token已过期");
        } catch (JwtException e) {
            throw new RuntimeException("Token无效");
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("userId", String.class);
    }

    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 从Token中获取用户角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("role", String.class);
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 获取Token过期时间
     */
    public long getTokenExpireTime() {
        return jwtConfig.getExpiration(); // 返回秒数
    }

    /**
     * 静态方法：创建Token（用于VM认证）
     */
    public static String createToken(Map<String, Object> claims) {
        // 使用默认密钥和过期时间
        String defaultSecret = "feduwacomm_jwt_secret_key_2024_default_256_bit_length_for_security";
        SecretKey defaultKey = Keys.hmacShaKeyFor(defaultSecret.getBytes());
        long defaultExpiration = 86400; // 24小时，单位：秒

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + defaultExpiration * 1000))
                .signWith(defaultKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 静态方法：验证Token（用于VM认证）
     */
    public static boolean validateTokenStatic(String token) {
        try {
            String defaultSecret = "feduwacomm_jwt_secret_key_2024_default_256_bit_length_for_security";
            SecretKey defaultKey = Keys.hmacShaKeyFor(defaultSecret.getBytes());
            
            Jwts.parserBuilder()
                    .setSigningKey(defaultKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}