package com.chatapp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CORS 过滤器
 * 处理跨域请求，特别是预检请求
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Component
@Order(1) // 确保这个过滤器在其他过滤器之前执行
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");
        System.out.println(
                "CORS过滤器 - 请求方法: " + request.getMethod() + ", URI: " + request.getRequestURI() + ", Origin: " + origin);

        // 设置CORS头
        response.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        response.setHeader("Access-Control-Allow-Headers",
                "Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control, Pragma");
        response.setHeader("Access-Control-Expose-Headers",
                "Authorization, Content-Disposition, Access-Control-Allow-Origin");
        response.setHeader("Access-Control-Max-Age", "3600");

        // 如果是OPTIONS预检请求，直接返回200
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            System.out.println("处理OPTIONS预检请求，直接返回200");
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 继续处理其他请求
        chain.doFilter(req, res);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        System.out.println("CORS过滤器已初始化");
    }

    @Override
    public void destroy() {
        System.out.println("CORS过滤器已销毁");
    }
}