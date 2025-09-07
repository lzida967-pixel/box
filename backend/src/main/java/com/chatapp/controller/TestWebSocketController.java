package com.chatapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket测试控制器
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/test/websocket")
public class TestWebSocketController {

    /**
     * 测试WebSocket连接状态
     */
    @GetMapping("/status")
    public Map<String, Object> getWebSocketStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "WebSocket服务正常运行");
        response.put("data", Map.of(
            "endpoint", "/ws/chat",
            "protocol", "websocket",
            "status", "active"
        ));
        return response;
    }

    /**
     * 获取WebSocket连接示例
     */
    @GetMapping("/example")
    public Map<String, Object> getWebSocketExample() {
        Map<String, Object> example = new HashMap<>();
        example.put("code", 200);
        example.put("message", "WebSocket连接示例");
        example.put("data", Map.of(
            "connection_url", "ws://localhost:8080/ws/chat",
            "message_types", new String[]{"private", "group", "typing", "read_receipt"},
            "private_message_example", Map.of(
                "type", "private",
                "toUserId", 123,
                "content", "Hello, World!",
                "messageType", 1
            ),
            "group_message_example", Map.of(
                "type", "group",
                "groupId", 456,
                "content", "Hello, Group!",
                "messageType", 1
            ),
            "typing_example", Map.of(
                "type", "typing",
                "toUserId", 123,
                "isTyping", true
            ),
            "read_receipt_example", Map.of(
                "type", "read_receipt",
                "messageId", 789
            )
        ));
        return example;
    }
}