package com.chatapp.config;

import com.chatapp.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * JWT认证过滤器
 * 用于验证JWT令牌并设置认证信息
 * 
 * @author ChatApp
 * @since 1.0.0
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                // 添加调试信息
                System.out.println("JWT过滤器 - 处理token: " + token.substring(0, Math.min(20, token.length())) + "...");
                System.out.println("JWT过滤器 - 请求URL: " + request.getRequestURI());

                if (jwtTokenService.validateToken(token)) {
                    String username = jwtTokenService.getUsernameFromToken(token);

                    System.out.println("JWT过滤器 - 提取到用户名: " + username);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        // 从token中提取用户ID并设置到请求头
                        try {
                            Long userId = jwtTokenService.getUserIdFromToken(token);
                            if (userId != null) {
                                // 创建包装器来添加请求头
                                RequestWrapper requestWrapper = new RequestWrapper(request);
                                requestWrapper.addHeader("X-User-Id", userId.toString());
                                request = requestWrapper;
                                System.out.println("JWT过滤器 - 设置用户ID: " + userId);
                            }
                        } catch (Exception e) {
                            System.err.println("JWT过滤器 - 提取用户ID失败: " + e.getMessage());
                        }
                        
                        System.out.println("JWT过滤器 - 认证成功，用户: " + username);
                    }
                } else {
                    System.out.println("JWT过滤器 - Token验证失败");
                }
            } else {
                System.out.println("JWT过滤器 - 未找到Authorization头或格式不正确");
            }
        } catch (Exception e) {
            System.err.println("JWT认证失败: " + e.getMessage());
            logger.error("JWT认证失败: " + e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 包装HttpServletRequest以添加X-User-Id请求头
     */
    private static class RequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders;

        public RequestWrapper(HttpServletRequest request) {
            super(request);
            this.customHeaders = new HashMap<>();
        }

        public void addHeader(String name, String value) {
            this.customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = customHeaders.get(name);
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> set = new HashSet<>(customHeaders.keySet());
            Enumeration<String> e = super.getHeaderNames();
            while (e.hasMoreElements()) {
                set.add(e.nextElement());
            }
            return Collections.enumeration(set);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String headerValue = customHeaders.get(name);
            if (headerValue != null) {
                return Collections.enumeration(Arrays.asList(headerValue));
            }
            return super.getHeaders(name);
        }
    }
}