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
 * VM JWT工具类
 * 专门处理虚拟机认证相关的JWT操作
 * 
 * @author FedUWAComm Team
 * @version 1.0.0
 */
@Component
public class VmJwtUtil {

    @Autowired
    private JwtConfig jwtConfig;
    
    private SecretKey key;

    /**
     * 初始化VM JWT密钥
     */
    private SecretKey getKey() {
        if (key == null) {
            this.key = Keys.hmacShaKeyFor(jwtConfig.getVm().getSecret().getBytes());
        }
        return key;
    }

    /**
     * 生成VM访问Token
     */
    public String generateAccessToken(String vmId, String vmName, String status) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("vmId", vmId);
        claims.put("vmName", vmName);
        claims.put("status", status);
        claims.put("type", "access");
        claims.put("category", "vm");

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getVm().getExpiration() * 1000))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * 验证VM Token
     */
    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // 验证token类别
            String category = claims.get("category", String.class);
            if (!"vm".equals(category)) {
                throw new JwtException("Token类别错误，期望vm类别");
            }
            
            return claims;
        } catch (ExpiredJwtException e) {
            throw new JwtException("VM Token已过期");
        } catch (UnsupportedJwtException e) {
            throw new JwtException("不支持的VM Token格式");
        } catch (MalformedJwtException e) {
            throw new JwtException("VM Token格式错误");
        } catch (SecurityException e) {
            throw new JwtException("VM Token签名验证失败");
        } catch (IllegalArgumentException e) {
            throw new JwtException("VM Token参数错误");
        }
    }

    /**
     * 从Token中提取VM ID
     */
    public String getVmIdFromToken(String token) {
        Claims claims = validateToken(token);
        return claims.get("vmId", String.class);
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