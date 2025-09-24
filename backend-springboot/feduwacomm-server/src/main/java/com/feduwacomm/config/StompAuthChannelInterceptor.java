package com.feduwacomm.config;

import com.feduwacomm.utils.UserJwtUtil;
import com.feduwacomm.utils.VmJwtUtil;
import com.feduwacomm.utils.UuidUtil;
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
import java.util.Map;

/**
 * STOMP 认证拦截器：
 * - 拦截 CONNECT 帧，校验 JWT（优先读取 Authorization 头，其次读取 token 头）
 * - 可选读取 vmId 头，放入会话属性
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private UserJwtUtil userJwtUtil;
    
    @Autowired
    private VmJwtUtil vmJwtUtil;

    @Autowired
    private com.feduwacomm.service.VmInstanceService vmInstanceService;

    @Autowired
    private UuidUtil uuidUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            System.out.println("STOMP CONNECT命令认证开始");
            System.out.println("Native Headers: " + accessor.toNativeHeaderMap());
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

            // 如果STOMP头中没有token，尝试从WebSocket session attributes中获取
            if (!StringUtils.hasText(token) && accessor.getSessionAttributes() != null) {
                Object sessionToken = accessor.getSessionAttributes().get("token");
                if (sessionToken instanceof String) {
                    token = (String) sessionToken;
                    System.out.println("从WebSocket session attributes获取到token: " + token.substring(0, Math.min(20, token.length())) + "...");
                }
            }

            if (!StringUtils.hasText(token)) {
                System.out.println("STOMP认证失败：缺少Token");
                System.out.println("Session Attributes: " + accessor.getSessionAttributes());
                throw new MessagingException("WebSocket/STOMP认证失败：缺少Token");
            }

            Claims claims = null;
            String category = null;
            String username = null;
            String role = null;
            
            // 尝试用用户JWT验证
            try {
                claims = userJwtUtil.validateToken(token);
                category = claims.get("category", String.class);
                if ("user".equals(category)) {
                    String type = claims.get("type", String.class);
                    if (!"access".equals(type)) {
                        throw new MessagingException("WebSocket/STOMP认证失败：用户Token类型错误");
                    }
                    
                    String userId = claims.get("userId", String.class);
                    username = claims.get("username", String.class);
                    role = claims.get("role", String.class);
                    
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null) {
                        sessionAttributes.put("userId", userId);
                        sessionAttributes.put("username", username);
                        sessionAttributes.put("role", role);
                        sessionAttributes.put("category", "user");
                    }
                }
            } catch (Exception ex) {
                // 用户JWT验证失败，尝试VM JWT验证
                try {
                    claims = vmJwtUtil.validateToken(token);
                    category = claims.get("category", String.class);
                    if ("vm".equals(category)) {
                        String type = claims.get("type", String.class);
                        if (!"access".equals(type)) {
                            throw new MessagingException("WebSocket/STOMP认证失败：VM Token类型错误");
                        }
                        
                        String vmId = claims.get("vmId", String.class);
                        String vmName = claims.get("vmName", String.class);
                        String status = claims.get("status", String.class);
                        
                        Map<String, Object> vmSessionAttributes = accessor.getSessionAttributes();
                        if (vmSessionAttributes != null) {
                            vmSessionAttributes.put("vmId", vmId);
                            vmSessionAttributes.put("vmName", vmName);
                            vmSessionAttributes.put("status", status);
                            vmSessionAttributes.put("category", "vm");
                        }

                        // 直接更新VM连接状态
                        try {
                            // 使用UuidUtil生成32位的会话ID，而不是使用Spring框架的长会话ID
                            String sessionId = uuidUtil.generateUuid();
                            vmInstanceService.updateConnectionStatus(vmId, "CONNECTED", sessionId);
                            System.out.println("VM认证成功，已更新连接状态: vmId=" + vmId + ", sessionId=" + sessionId);
                        } catch (Exception e) {
                            System.err.println("更新VM连接状态失败: vmId=" + vmId + ", error=" + e.getMessage());
                        }

                        // 为VM设置默认用户信息
                        username = vmName != null ? vmName : vmId;
                        role = "VM";
                    }
                } catch (Exception vmEx) {
                    throw new MessagingException("WebSocket/STOMP认证失败：Token无效或已过期");
                }
            }
            
            if (claims == null) {
                throw new MessagingException("WebSocket/STOMP认证失败：Token验证失败");
            }

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