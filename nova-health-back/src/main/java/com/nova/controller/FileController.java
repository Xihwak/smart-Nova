package com.nova.controller;

import cn.hutool.core.io.FileUtil;
import com.nova.constant.FileConstant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件下载控制器
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    /**
     * 下载PDF文件
     */
    @GetMapping("/download/pdf/{fileName}")
    public ResponseEntity<byte[]> downloadPDF(@PathVariable String fileName) {
        return downloadFile("pdf", fileName);
    }

    /**
     * 下载普通文件
     */
    @GetMapping("/download/file/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        return downloadFile("file", fileName);
    }

    /**
     * 下载资源文件
     */
    @GetMapping("/download/resource/{fileName}")
    public ResponseEntity<byte[]> downloadResource(@PathVariable String fileName) {
        return downloadFile("download", fileName);
    }

    /**
     * 通用文件下载方法
     */
    private ResponseEntity<byte[]> downloadFile(String subDir, String fileName) {
        try {
            // 构建文件路径
            Path filePath = Paths.get(FileConstant.FILE_SAVE_DIR, subDir, fileName);
            File file = filePath.toFile();

            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            // 读取文件内容
            byte[] fileContent = Files.readAllBytes(filePath);

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            
            // 处理文件名编码（支持中文）
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.set("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取文件下载链接
     */
    @GetMapping("/download-url/{type}/{fileName}")
    public ResponseEntity<String> getDownloadUrl(@PathVariable String type, @PathVariable String fileName) {
        String baseUrl = "/api/files/download/";
        String url = baseUrl + type + "/" + fileName;
        return ResponseEntity.ok(url);
    }
}
