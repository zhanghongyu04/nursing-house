package com.zhiling.system.application.service.impl;

import com.zhiling.common.properties.RustFsProperties;
import com.zhiling.system.application.port.ObjectStoragePort;
import com.zhiling.system.application.service.CommonFileService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 通用文件服务实现
 *
 * @author zhanghongyu
 */
@Service
public class CommonFileServiceImpl implements CommonFileService {

    private static final Logger log = LoggerFactory.getLogger(CommonFileServiceImpl.class);

    private final ObjectStoragePort objectStoragePort;
    private final RustFsProperties rustFsProperties;

    private static final long PRESIGNED_URL_EXPIRATION_MINUTES = 60;

    public CommonFileServiceImpl(ObjectStoragePort objectStoragePort,
                                 RustFsProperties rustFsProperties) {
        this.objectStoragePort = objectStoragePort;
        this.rustFsProperties = rustFsProperties;
    }

    /**
     * 通用文件上传请求(单个)
     * @param file 要上传的文件
     * @return 文件访问 URL
     */
    @Override
    public String upload(MultipartFile file) {
        try {
            String bucketName = rustFsProperties.getBucketName();

            // 生成对象键：日期路径 + UUID + 原始文件名
            String datePath = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String originalFilename = file.getOriginalFilename();
            String objectKey = datePath + "/" + UUID.randomUUID() + "-" + originalFilename;

            log.info("开始上传文件: originalName={}, objectKey={}, size={}",
                    originalFilename, objectKey, file.getSize());
            String fileUrl = objectStoragePort.upload(objectKey, file.getInputStream(), file.getSize(), file.getContentType());

            log.info("文件上传成功: objectKey={}, url={}", objectKey, fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通用文件下载请求
     * @param objectKey 对象键（完整路径，如：20250312/uuid-xxx.jpg）
     * @param response HttpServletResponse
     * @return 文件下载是否成功
     */
    @Override
    public Boolean download(String objectKey, HttpServletResponse response) {
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            log.info("开始下载文件: objectKey={}", objectKey);
            inputStream = objectStoragePort.download(objectKey);

            // 提取原始文件名（去掉日期路径和UUID前缀）
            String displayFileName = extractOriginalFileName(objectKey);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", buildContentDisposition("attachment", displayFileName));

            outputStream = response.getOutputStream();
            byte[] buffer = new byte[4096]; // 增大缓冲区
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            log.info("文件下载成功: objectKey={}", objectKey);
            return true;

        } catch (Exception e) {
            log.error("文件下载失败: objectKey={}", objectKey, e);
            return false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                log.warn("关闭流失败", e);
            }
        }
    }

    /**
     * 下载文件为字节数组
     * @param objectKey 对象键（完整路径）
     * @return 文件字节数组
     */
    public byte[] downloadAsBytes(String objectKey) {
        try {
            byte[] fileBytes = objectStoragePort.downloadAsBytes(objectKey);
            log.info("文件下载为字节数组成功: objectKey={}, size={}", objectKey, fileBytes.length);
            return fileBytes;

        } catch (Exception e) {
            log.error("文件下载为字节数组失败: objectKey={}", objectKey, e);
            return null;
        }
    }

    /**
     * 通过文件 URL 解析 objectKey 后下载为字节数组。
     *
     * @author zhanghongyu
     */
    @Override
    public byte[] downloadAsBytesByUrl(String fileUrl) {
        String objectKey = extractObjectKeyFromUrl(fileUrl);
        if (objectKey == null || objectKey.isBlank()) {
            log.warn("根据 URL 提取 objectKey 失败: fileUrl={}", fileUrl);
            return null;
        }
        return downloadAsBytes(objectKey);
    }

    /**
     * 通用文件删除请求
     * @param objectKey 对象键（完整路径）
     * @return 删除成功返回true，否则返回false
     */
    @Override
    public Boolean delete(String objectKey) {
        try {
            log.info("开始删除文件: objectKey={}", objectKey);
            Boolean deleted = objectStoragePort.delete(objectKey);
            if (deleted) {
                log.info("文件删除成功: objectKey={}", objectKey);
            }
            return deleted;
        } catch (Exception e) {
            log.error("文件删除失败: objectKey={}", objectKey, e);
            return false;
        }
    }

    /**
     * 生成上传预签名URL（PUT）
     * @param originalFileName 原始文件名
     * @return PresignedUploadResult 包含预签名URL和objectKey
     */
    @Override
    public CommonFileService.PresignedUploadResult generatePresignedUploadUrl(String originalFileName) {
        try {
            // 生成对象键：日期路径 + UUID + 原始文件名
            String objectKey = generateObjectKey(originalFileName);

            log.info("生成上传预签名URL: originalName={}, objectKey={}", originalFileName, objectKey);
            ObjectStoragePort.PresignedUploadInfo presignedUploadInfo =
                    objectStoragePort.generatePresignedUpload(objectKey, PRESIGNED_URL_EXPIRATION_MINUTES);

            log.info("上传预签名URL生成成功: objectKey={}", objectKey);

            return new CommonFileService.PresignedUploadResult(
                    presignedUploadInfo.presignedUrl(),
                    presignedUploadInfo.objectKey(),
                    presignedUploadInfo.fileUrl()
            );

        } catch (Exception e) {
            log.error("生成上传预签名URL失败: fileName={}", originalFileName, e);
            throw new RuntimeException("生成预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成下载预签名URL（GET）
     * @param objectKey 对象键（完整路径，如：20250312/uuid-xxx.jpg）
     * @return 预签名下载 URL
     */
    @Override
    public String generatePresignedDownloadUrl(String objectKey) {
        try {
            log.info("生成下载预签名URL: objectKey={}", objectKey);
            String presignedUrl = objectStoragePort.generatePresignedDownload(objectKey, PRESIGNED_URL_EXPIRATION_MINUTES);

            log.info("下载预签名URL生成成功: objectKey={}", objectKey);
            return presignedUrl;

        } catch (Exception e) {
            log.error("生成下载预签名URL失败: objectKey={}", objectKey, e);
            throw new RuntimeException("生成预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成对象键（包含日期路径）
     * 格式: yyyyMMdd/UUID-originalFileName
     * @param originalFileName 原始文件名
     * @return 对象键
     */
    private String generateObjectKey(String originalFileName) {
        String datePath = new SimpleDateFormat("yyyyMMdd").format(new Date());
        return datePath + "/" + UUID.randomUUID() + "-" + originalFileName;
    }

    /**
     * 从对象键中提取原始文件名
     * 例如：20250312/550e8400-e29b-41d4-a716-446655440000-photo.jpg
     *      提取出：photo.jpg
     * @param objectKey 对象键
     * @return 原始文件名
     */
    private String extractOriginalFileName(String objectKey) {
        // 对象键格式：yyyyMMdd/UUID-filename
        // 我们需要提取 UUID 后面的部分
        int lastSlashIndex = objectKey.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < objectKey.length() - 1) {
            String fileNamePart = objectKey.substring(lastSlashIndex + 1);
            if (fileNamePart.matches("^[0-9a-fA-F-]{36}-.+")) {
                return fileNamePart.substring(37);
            }
        }
        // 如果无法解析，返回原始对象键
        return objectKey;
    }

    /**
     * 构建兼容中文文件名的 Content-Disposition 响应头。
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
     * 从完整文件 URL 中提取对象存储 objectKey。
     *
     * @author zhanghongyu
     */
    private String extractObjectKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return null;
        }
        try {
            String decoded = URLDecoder.decode(fileUrl.trim(), StandardCharsets.UTF_8);
            String bucketSegment = "/" + rustFsProperties.getBucketName() + "/";
            int bucketIndex = decoded.indexOf(bucketSegment);
            if (bucketIndex < 0) {
                return null;
            }
            return decoded.substring(bucketIndex + bucketSegment.length());
        } catch (Exception ex) {
            log.warn("解析文件 URL 失败: fileUrl={}", fileUrl, ex);
            return null;
        }
    }
}
