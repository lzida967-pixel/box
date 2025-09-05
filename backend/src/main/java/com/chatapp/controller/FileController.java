package com.chatapp.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件访问控制器
 * 用于调试静态资源访问问题
 */
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileController {

    /**
     * 直接访问头像文件
     */
    @GetMapping("/avatar/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        try {
            String currentPath = System.getProperty("user.dir");
            Path filePath = Paths.get(currentPath, "uploads", "avatars", filename);
            
            System.out.println("请求访问文件: " + filename);
            System.out.println("文件完整路径: " + filePath.toAbsolutePath());
            System.out.println("文件是否存在: " + Files.exists(filePath));
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            
            // 获取文件的MIME类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            System.err.println("访问文件失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 列出所有头像文件
     */
    @GetMapping("/avatars")
    public ResponseEntity<?> listAvatars() {
        try {
            String currentPath = System.getProperty("user.dir");
            File avatarDir = new File(currentPath + "/uploads/avatars");
            
            System.out.println("头像目录: " + avatarDir.getAbsolutePath());
            System.out.println("目录是否存在: " + avatarDir.exists());
            
            if (!avatarDir.exists()) {
                return ResponseEntity.ok("头像目录不存在: " + avatarDir.getAbsolutePath());
            }
            
            File[] files = avatarDir.listFiles();
            if (files == null || files.length == 0) {
                return ResponseEntity.ok("头像目录为空");
            }
            
            StringBuilder result = new StringBuilder();
            result.append("头像目录: ").append(avatarDir.getAbsolutePath()).append("\n");
            result.append("文件列表:\n");
            
            for (File file : files) {
                result.append("- ").append(file.getName())
                      .append(" (").append(file.length()).append(" bytes)")
                      .append(" [").append(file.canRead() ? "可读" : "不可读").append("]\n");
            }
            
            return ResponseEntity.ok(result.toString());
            
        } catch (Exception e) {
            System.err.println("列出文件失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("错误: " + e.getMessage());
        }
    }
}