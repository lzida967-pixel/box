package com.chatapp.controller;

import com.chatapp.entity.ImageFile;
import com.chatapp.service.ImageFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/images")
public class ImageController {

    @Autowired
    private ImageFileService imageFileService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(error(400, "文件为空"));
            }
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            byte[] data = file.getBytes();
            long size = file.getSize();
            String originalName = file.getOriginalFilename();

            ImageFile saved = imageFileService.save(contentType, data, size, originalName);

            Map<String, Object> resp = ok("上传成功", Map.of(
                    "id", saved.getId(),
                    "contentType", saved.getContentType(),
                    "size", saved.getSize(),
                    "originalName", saved.getOriginalName()
            ));
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(error(500, "上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> fetch(@PathVariable("id") Long id) {
        ImageFile img = imageFileService.get(id);
        if (img == null || img.getData() == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType mt;
        try {
            mt = MediaType.parseMediaType(img.getContentType());
        } catch (Exception e) {
            mt = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .contentType(mt)
                .body(img.getData());
    }

    @GetMapping("/by-name")
    public ResponseEntity<Map<String, Object>> findByName(@RequestParam("name") String name) {
        ImageFile img = imageFileService.findByOriginalName(name);
        if (img == null) {
            return ResponseEntity.ok(error(404, "未找到图片: " + name));
        }
        return ResponseEntity.ok(ok("查询成功", Map.of("id", img.getId())));
    }

    private Map<String, Object> ok(String message, Object data) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", 200);
        m.put("message", message);
        m.put("data", data);
        return m;
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("message", message);
        m.put("data", null);
        return m;
    }
}