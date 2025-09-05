package com.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 配置静态资源访问
 * 
 * @author ChatApp
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取当前工作目录的绝对路径
        String currentPath = System.getProperty("user.dir");
        String uploadPath = "file:" + currentPath + "/uploads/";
        
        // 配置头像文件的静态资源访问
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(0); // 禁用缓存，方便测试
        
        System.out.println("静态资源配置完成：/uploads/** -> " + uploadPath);
        System.out.println("当前工作目录: " + currentPath);
        
        // 检查文件是否存在
        java.io.File uploadDir = new java.io.File(currentPath + "/uploads/avatars");
        System.out.println("上传目录是否存在: " + uploadDir.exists());
        if (uploadDir.exists()) {
            System.out.println("目录中的文件: ");
            java.io.File[] files = uploadDir.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    System.out.println("  - " + file.getName() + " (" + file.length() + " bytes)");
                }
            }
        }
    }
}