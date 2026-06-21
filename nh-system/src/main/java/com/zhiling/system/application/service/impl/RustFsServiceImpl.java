package com.zhiling.system.application.service.impl;

import com.zhiling.common.properties.RustFsProperties;
import com.zhiling.system.application.service.RustFsService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/**
 * RustFS 文件服务实现类
 * 提供基于 AWS S3 SDK 的 RustFS 对象存储操作
 *
 * @since 1.0.1
 *
 * @author zhanghongyu
 */
@Service
public class RustFsServiceImpl implements RustFsService {

    private static final Logger log = LoggerFactory.getLogger(RustFsServiceImpl.class);
    private static final String RUSTFS_BANNER = """
         _____           _   ______ _____
        |  __ \\         | | |  ____/ ____|
        | |__) |   _ ___| |_| |__ | (___
        |  _  / | | / __| __|  __| \\___ \\
        | | \\ \\ |_| \\__ \\ |_| |    ____) |
        |_|  \\_\\__,_|___/\\__|_|   |_____/

                     RustFS Object Storage
        """;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RustFsProperties rustFsProperties;
    private final String bucketName;
    private final String endpoint;

    /**
     * 注入 RustFS 所需客户端与配置，并缓存核心桶信息。
     *
     * @author zhanghongyu
     */
    public RustFsServiceImpl(S3Client s3Client, S3Presigner s3Presigner, RustFsProperties rustFsProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.rustFsProperties = rustFsProperties;
        this.bucketName = rustFsProperties.getBucketName();
        this.endpoint = rustFsProperties.getEndpoint();
    }

    /**
     * 服务启动后打印 RustFS 标识并确保目标存储桶可用。
     *
     * @author zhanghongyu
     */
    @PostConstruct
    public void init() {
        log.info("\n{}", RUSTFS_BANNER);
        createBucketIfNotExists();
        log.info("RustFS 服务初始化完成: endpoint={}, bucket={}", endpoint, bucketName);
    }

    /**
     * 上传 MultipartFile 到对象存储并返回可访问 URL。
     *
     * @author zhanghongyu
     */
    @Override
    public String uploadFile(String objectKey, MultipartFile file) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            log.info("文件上传成功: bucket={}, key={}", bucketName, objectKey);
            return getFileUrl(objectKey);
        } catch (Exception e) {
            log.error("文件上传失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传字节数组到对象存储并返回可访问 URL。
     *
     * @author zhanghongyu
     */
    @Override
    public String uploadFile(String objectKey, byte[] fileBytes, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .contentLength((long) fileBytes.length)
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            log.info("文件上传成功: bucket={}, key={}", bucketName, objectKey);
            return getFileUrl(objectKey);
        } catch (Exception e) {
            log.error("文件上传失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从输入流上传文件内容到对象存储并返回可访问 URL。
     *
     * @author zhanghongyu
     */
    @Override
    public String uploadFile(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));

            log.info("文件上传成功: bucket={}, key={}", bucketName, objectKey);
            return getFileUrl(objectKey);
        } catch (Exception e) {
            log.error("文件上传失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从对象存储下载文件流，调用方负责消费与关闭流。
     *
     * @author zhanghongyu
     */
    @Override
    public InputStream downloadFile(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

            return s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            log.error("文件下载失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除单个对象。
     *
     * @author zhanghongyu
     */
    @Override
    public void deleteFile(String objectKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.info("文件删除成功: bucket={}, key={}", bucketName, objectKey);
        } catch (Exception e) {
            log.error("文件删除失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量删除对象键集合。
     *
     * @author zhanghongyu
     */
    @Override
    public void deleteFiles(List<String> objectKeys) {
        try {
            List<ObjectIdentifier> objects = objectKeys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();

            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(Delete.builder().objects(objects).build())
                .build();

            s3Client.deleteObjects(deleteObjectsRequest);

            log.info("批量删除成功: bucket={}, count={}", bucketName, objectKeys.size());
        } catch (Exception e) {
            log.error("批量删除失败: bucket={}, count={}", bucketName, objectKeys.size(), e);
            throw new RuntimeException("批量删除失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据 endpoint、bucket 与 objectKey 组装文件访问 URL。
     *
     * @author zhanghongyu
     */
    @Override
    public String getFileUrl(String objectKey) {
        // 使用配置中的 endpoint 构建 URL
        // 确保 endpoint 结尾没有斜杠，objectKey 开头没有斜杠
        String cleanEndpoint = endpoint.replaceAll("/+$", "");
        String cleanObjectKey = objectKey.replaceFirst("^/+", "");
        return String.format("%s/%s/%s", cleanEndpoint, bucketName, cleanObjectKey);
    }

    /**
     * 通过 HeadObject 检测对象是否存在。
     *
     * @author zhanghongyu
     */
    @Override
    public boolean fileExists(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据可选前缀列出对象列表。
     *
     * @author zhanghongyu
     */
    @Override
    public List<S3Object> listObjects(String prefix) {
        try {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                .bucket(bucketName);

            if (prefix != null && !prefix.isEmpty()) {
                builder.prefix(prefix);
            }

            ListObjectsV2Request listObjectsV2Request = builder.build();
            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsV2Request);

            log.info("列出对象成功: bucket={}, count={}", bucketName, response.contents().size());
            return response.contents();
        } catch (Exception e) {
            log.error("列出对象失败: bucket={}", bucketName, e);
            throw new RuntimeException("列出对象失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成指定有效期的预签名上传 URL（PUT）。
     *
     * @author zhanghongyu
     */
    @Override
    public String generatePresignedUploadUrl(String objectKey, long expirationMinutes) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build();

            String presignedUrl = s3Presigner.presignPutObject(presignRequest).url().toString();

            log.info("生成预签名上传URL成功: bucket={}, key={}, expiration={}min",
                bucketName, objectKey, expirationMinutes);
            return presignedUrl;
        } catch (Exception e) {
            log.error("生成预签名上传URL失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("生成预签名上传URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成指定有效期的预签名下载 URL（GET）。
     *
     * @author zhanghongyu
     */
    @Override
    public String generatePresignedDownloadUrl(String objectKey, long expirationMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

            String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();

            log.info("生成预签名下载URL成功: bucket={}, key={}, expiration={}min",
                bucketName, objectKey, expirationMinutes);
            return presignedUrl;
        } catch (Exception e) {
            log.error("生成预签名下载URL失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("生成预签名下载URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 若桶不存在则自动创建，保证后续上传/下载链路可执行。
     *
     * @author zhanghongyu
     */
    @Override
    public void createBucketIfNotExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();

            s3Client.headBucket(headBucketRequest);
            log.info("存储桶已存在: {}", bucketName);
        } catch (Exception e) {
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

                s3Client.createBucket(createBucketRequest);
                log.info("存储桶创建成功: {}", bucketName);
            } catch (Exception ex) {
                log.error("创建存储桶失败: {}", bucketName, ex);
                throw new RuntimeException("创建存储桶失败: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 查询对象元数据（大小、内容类型、更新时间等）。
     *
     * @author zhanghongyu
     */
    @Override
    public HeadObjectResponse getFileMetadata(String objectKey) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

            return s3Client.headObject(headObjectRequest);
        } catch (Exception e) {
            log.error("获取文件元数据失败: bucket={}, key={}", bucketName, objectKey, e);
            throw new RuntimeException("获取文件元数据失败: " + e.getMessage(), e);
        }
    }
}
