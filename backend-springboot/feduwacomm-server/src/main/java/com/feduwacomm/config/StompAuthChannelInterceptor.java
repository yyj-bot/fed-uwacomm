package com.feduwacomm.config;

import com.feduwacomm.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * STOMP 认证拦截器：
 * - 拦截 CONNECT 帧，校验 JWT（优先读取 Authorization 头，其次读取 token 头）
 * - 可选读取 vmId 头，放入会话属性
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = firstNonEmpty(
                    accessor.getFirstNativeHeader("Authorization"),
                    accessor.getFirstNativeHeader("authorization"));

            String token = null;
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            }
            if (!StringUtils.hasText(token)) {
                token = firstNonEmpty(
                        accessor.getFirstNativeHeader("token"),
                        accessor.getFirstNativeHeader("Token"));
            }

            if (!StringUtils.hasText(token)) {
                throw new MessagingException("WebSocket/STOMP认证失败：缺少Token");
            }

            Claims claims;
            try {
                claims = jwtUtil.validateToken(token);
            } catch (Exception ex) {
                throw new MessagingException("WebSocket/STOMP认证失败：Token无效或已过期");
            }

            String type = claims.get("type", String.class);
            if (!"access".equals(type)) {
                throw new MessagingException("WebSocket/STOMP认证失败：Token类型错误");
            }

            String userId = claims.get("userId", String.class);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            accessor.getSessionAttributes().put("userId", userId);
            accessor.getSessionAttributes().put("username", username);
            accessor.getSessionAttributes().put("role", role);

            // 设置 Principal 以支持点对点消息
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (StringUtils.hasText(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            Principal principal = new UsernamePasswordAuthenticationToken(username, null, authorities);
            accessor.setUser(principal);

            // 可选 vmId 透传
            String vmId = firstNonEmpty(accessor.getFirstNativeHeader("vmId"), accessor.getFirstNativeHeader("VMID"));
            if (StringUtils.hasText(vmId)) {
                accessor.getSessionAttributes().put("vmId", vmId);
            }
        }
        return message;
    }

    @Nullable
    private String firstNonEmpty(String a, String b) {
        if (StringUtils.hasText(a))
            return a;
        return StringUtils.hasText(b) ? b : null;
    }
}