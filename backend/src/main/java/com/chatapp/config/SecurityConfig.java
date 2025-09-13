package com.chatapp.config;

import com.chatapp.service.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 安全配置类
 *
 * @author ChatApp
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public SecurityConfig() {
        // 无参构造函数，已移除JWT相关依赖
    }

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 安全过滤器链配置（已移除权限验证）
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println(
                "加载安全配置，允许匿名访问的路径: /auth/**, /contacts/**, /api/user/list-all, /api/user/avatar/**, /user/avatar/**, /api/files/**");
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 明确允许所有OPTIONS请求
                        .requestMatchers("/auth/**", "/contacts/**", "/api/user/list-all", "/api/user/avatar/**",
                                "/user/avatar/**", "/api/files/**", "/ws/**", "/api/ws/**", "/api/messages/**","/api/groups/**",
                                "/images/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            System.out.println("认证失败: " + request.getRequestURI() + ", 方法: " + request.getMethod());
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        }));

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenService(), userDetailsService());
    }

    @Bean
    public JwtTokenService jwtTokenService() {
        return new JwtTokenService();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            return org.springframework.security.core.userdetails.User.builder()
                    .username(username)
                    .password("password")
                    .roles("USER")
                    .build();
        };
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有源
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("*"));
        // 允许携带凭证
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间（1小时）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        System.out.println("CORS配置已加载：允许所有源、方法和请求头");
        System.out.println("允许的源: " + configuration.getAllowedOriginPatterns());
        System.out.println("允许的方法: " + configuration.getAllowedMethods());
        System.out.println("允许的请求头: " + configuration.getAllowedHeaders());
        return source;
    }
}