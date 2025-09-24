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
        MultiValueMap<String, String> queryParams = UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        System.out.println("WebSocket握手开始: " + uri);
        System.out.println("查询参数: " + queryParams);

        String token = first(queryParams.getFirst("token"), queryParams.getFirst("Token"));
        if (token != null && !token.isBlank()) {
            attributes.put("token", token);
            System.out.println("Token提取成功: " + token.substring(0, Math.min(20, token.length())) + "...");
        } else {
            System.out.println("Token提取失败: token参数为空");
        }

        String vmId = first(queryParams.getFirst("vmId"), queryParams.getFirst("VMID"));
        if (vmId != null && !vmId.isBlank()) {
            attributes.put("vmId", vmId);
            System.out.println("VmId提取成功: " + vmId);
        } else {
            System.out.println("VmId提取失败: vmId参数为空");
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