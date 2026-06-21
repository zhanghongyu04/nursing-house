package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.system.application.service.CommonFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/commonFile")
@Tag(name = "通用文件接口")
/**
 * 通用文件控制器
 *
 * @author zhanghongyu
 */
@Slf4j
public class CommonFileController {
    private final CommonFileService fileService;

    /**
     * 构造器：CommonFileController
     *
     * @author zhanghongyu
     */
    public CommonFileController(CommonFileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @PostMapping("/upload")
    @Operation(summary = "通用文件上传请求(单个)")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        return Result.success(fileService.upload(file));
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @PostMapping("/uploads")
    @Operation(summary = "通用文件上传请求(多个)")
    public Result<List<String>> uploadFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = fileService.upload(file);
            if (url != null) {
                urls.add(url);
            } else {
                throw new RuntimeException("文件上传失败：" + file.getOriginalFilename());
            }
        }
        return Result.success(urls);
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @PostMapping("/download")
    @Operation(summary = "通用文件下载请求")
    public Result downloadFile(@RequestParam("fileName") String fileName, HttpServletResponse response) {
        boolean success = fileService.download(fileName, response);
        if (success) {
            return Result.success();
        } else {
            return Result.fail("文件下载失败");
        }
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/delete")
    @Operation(summary = "通用文件删除请求")
    public Result<String> deleteFile(@RequestParam("fileName") String fileName) {
        boolean isDeleted = fileService.delete(fileName);
        if (isDeleted) {
            return Result.success("文件删除成功");
        } else {
            return Result.fail("文件删除失败");
        }
    }

    @GetMapping("/presigned-upload-url")
    @Operation(summary = "获取上传预签名URL")
    public Result<CommonFileService.PresignedUploadResult> generateUploadUrl(
            @RequestParam("fileName") String originalFileName) {
        return Result.success(fileService.generatePresignedUploadUrl(originalFileName));
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @GetMapping("/presigned-download-url")
    @Operation(summary = "获取下载预签名URL")
    public Result<String> generateDownloadUrl(@RequestParam("objectKey") String objectKey) {
        return Result.success(fileService.generatePresignedDownloadUrl(objectKey));
    }

    /**
     * 方法：getImage
     *
     * @author zhanghongyu
     */
    @GetMapping("/image/**")
    @Operation(summary = "获取图片（代理方式）")
    public ResponseEntity<byte[]> getImage(HttpServletRequest request) {
        try {
            String requestURI = request.getRequestURI();
            // 从 /api/v1/commonFile/image/xxx 中提取 xxx 部分
            String encodedObjectKey = requestURI.substring("/api/v1/commonFile/image/".length());
            String objectKey = decodeObjectKey(encodedObjectKey);
            log.info("代理获取图片: encodedObjectKey={}, objectKey={}", encodedObjectKey, objectKey);

            byte[] imageBytes = fileService.downloadAsBytes(objectKey);

            if (imageBytes == null) {
                log.warn("图片不存在: objectKey={}", objectKey);
                return ResponseEntity.notFound().build();
            }

            // 根据 objectKey 判断 Content-Type
            String contentType = determineContentType(objectKey);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition("inline", extractFileName(objectKey)))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400") // 缓存 24 小时
                    .body(imageBytes);

        } catch (Exception e) {
            log.error("获取图片失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据 objectKey 判断 Content-Type
     */
    private String determineContentType(String objectKey) {
        String lowerKey = objectKey.toLowerCase();
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerKey.endsWith(".png")) {
            return "image/png";
        } else if (lowerKey.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerKey.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerKey.endsWith(".bmp")) {
            return "image/bmp";
        } else if (lowerKey.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "image/jpeg"; // 默认
    }

    /**
     * 从 objectKey 中提取文件名
     */
    private String extractFileName(String objectKey) {
        int lastSlashIndex = objectKey.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < objectKey.length() - 1) {
            return objectKey.substring(lastSlashIndex + 1);
        }
        return objectKey;
    }

    /**
     * 方法：buildContentDisposition
     *
     * @author zhanghongyu
     */
    private String buildContentDisposition(String dispositionType, String fileName) {
        String safeFileName = fileName == null || fileName.isBlank() ? "file" : fileName;
        String asciiFallback = safeFileName
                .replaceAll("[\\\\\\r\\n\"]", "_")
                .replaceAll("[^\\x20-\\x7E]", "_");
        String encoded = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return String.format("%s; filename=\"%s\"; filename*=UTF-8''%s",
                dispositionType,
                asciiFallback,
                encoded);
    }

    /**
     * URL 路径中的 objectKey 会被编码（中文、空格等），
     * 下载前需要解码为存储系统中的真实对象键。
     */
    private String decodeObjectKey(String encodedObjectKey) {
        if (encodedObjectKey == null || encodedObjectKey.isEmpty()) {
            return encodedObjectKey;
        }
        return UriUtils.decode(encodedObjectKey, StandardCharsets.UTF_8);
    }
}
