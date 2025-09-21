package com.feduwacomm.utils;

import com.feduwacomm.config.JwtConfig;
import com.feduwacomm.constants.SystemConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户JWT工具类
 * 专门处理用户认证相关的JWT操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class UserJwtUtil {

    @Autowired
    private JwtConfig jwtConfig;
    
    private SecretKey key;

    /**
     * 初始化用户JWT密钥
     */
    private SecretKey getKey() {
        if (key == null) {
            this.key = Keys.hmacShaKeyFor(jwtConfig.getUser().getSecret().getBytes(SystemConstants.DEFAULT_CHARSET));
        }
        return key;
    }

    /**
     * 生成用户访问Token
     */
    public String generateAccessToken(String userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        claims.put("type", "access");
        claims.put("category", "user");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getUser().getExpiration() * 1000))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成用户刷新Token
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        claims.put("category", "user");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getUser().getRefreshExpiration() * 1000))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证用户Token
     */
    public Claims validateToken(String token) {
        try {
            // 额外清理token，移除可能存在的空白字符和Bearer前缀
            String cleanToken = token.trim().replaceAll("\\s+", "");
            if (cleanToken.startsWith("Bearer")) {
                cleanToken = cleanToken.substring(6); // 移除"Bearer"前缀
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(cleanToken)
                    .getBody();
            
            // 验证token类别
            String category = claims.get("category", String.class);
            if (!"user".equals(category)) {
                throw new JwtException("Token类别错误，期望user类别");
            }
            
            return claims;
        } catch (ExpiredJwtException e) {
            throw new JwtException("用户Token已过期");
        } catch (UnsupportedJwtException e) {
            throw new JwtException("不支持的用户Token格式");
        } catch (MalformedJwtException e) {
            throw new JwtException("用户Token格式错误");
        } catch (SecurityException e) {
            throw new JwtException("用户Token签名验证失败");
        } catch (IllegalArgumentException e) {
            throw new JwtException("用户Token参数错误");
        }
    }

    /**
     * 从Token中提取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("userId", String.class);
    }

    /**
     * 检查Token是否即将过期（1小时内）
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = validateToken(token);
            Date expiration = claims.getExpiration();
            long timeUntilExpiration = expiration.getTime() - System.currentTimeMillis();
            return timeUntilExpiration < 3600000; // 1小时 = 3600000毫秒
        } catch (Exception e) {
            return true; // 如果Token无效，视为即将过期
        }
    }
}