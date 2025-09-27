package com.feduwacomm.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
public class WebSocketHandshakeAuthInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        URI uri = request.getURI();

        System.out.println("WebSocket握手开始: " + uri);
        System.out.println("请求头部: " + request.getHeaders());

        // 从Authorization头部提取token
        String authHeader = request.getHeaders().getFirst("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // 移除"Bearer "前缀
        }

        if (token != null && !token.isBlank()) {
            attributes.put("token", token);
            System.out.println("Token提取成功: " + token.substring(0, Math.min(20, token.length())) + "...");
        } else {
            System.out.println("Token提取失败: Authorization头部为空或格式不正确");
        }

        // 从X-VM-ID头部提取vmId
        String vmId = request.getHeaders().getFirst("X-VM-ID");
        if (vmId != null && !vmId.isBlank()) {
            attributes.put("vmId", vmId);
            System.out.println("VmId提取成功: " + vmId);
        } else {
            System.out.println("VmId提取失败: X-VM-ID头部为空");
        }

        System.out.println("握手前置检查通过");
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }

    private String first(String a, String b) {
        return (a != null && !a.isBlank()) ? a : ((b != null && !b.isBlank()) ? b : null);
    }
}