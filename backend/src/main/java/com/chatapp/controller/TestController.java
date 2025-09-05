package com.chatapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于验证服务是否正常运行
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "服务正常运行");
        response.put("data", Map.of(
            "status", "OK",
            "timestamp", System.currentTimeMillis()
        ));
        return response;
    }

    /**
     * 欢迎接口
     */
    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "欢迎使用聊天应用后端服务");
        response.put("data", Map.of(
            "service", "ChatApp Backend",
            "version", "1.0.0",
            "status", "running"
        ));
        return response;
    }

    /**
     * 数据库连接测试接口
     */
    @GetMapping("/db")
    public Map<String, Object> dbTest() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "数据库连接正常");
        response.put("data", Map.of(
            "database", "MySQL",
            "status", "connected",
            "timestamp", System.currentTimeMillis()
        ));
        return response;
    }
}