package com.zhiling.system.infrastructure.storage;

import com.zhiling.common.properties.RustFsProperties;
import com.zhiling.system.application.port.ObjectStoragePort;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * RustFS 对象存储适配器。
 *
 * @author zhanghongyu
 */
@Component
public class RustFsObjectStorageAdapter implements ObjectStoragePort {

    private static final Logger log = LoggerFactory.getLogger(RustFsObjectStorageAdapter.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RustFsProperties rustFsProperties;

    public RustFsObjectStorageAdapter(S3Client s3Client,
                                      S3Presigner s3Presigner,
                                      RustFsProperties rustFsProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.rustFsProperties = rustFsProperties;
    }

    /**
     * 方法：ensureBucketReady
     *
     * @author zhanghongyu
     */
    @PostConstruct
    @Override
    public void ensureBucketReady() {
        String bucketName = rustFsProperties.getBucketName();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.info("存储桶已存在: {}", bucketName);
        } catch (Exception e) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("存储桶创建成功: {}", bucketName);
            } catch (Exception ex) {
                log.error("创建存储桶失败: {}", bucketName, ex);
                throw new RuntimeException("创建存储桶失败: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * 方法：upload
     *
     * @author zhanghongyu
     */
    @Override
    public String upload(String objectKey, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            return getFileUrl(objectKey);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 方法：download
     *
     * @author zhanghongyu
     */
    @Override
    public InputStream download(String objectKey) {
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .key(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 方法：downloadAsBytes
     *
     * @author zhanghongyu
     */
    @Override
    public byte[] downloadAsBytes(String objectKey) {
        try (InputStream inputStream = download(objectKey)) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 方法：delete
     *
     * @author zhanghongyu
     */
    @Override
    public boolean delete(String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .key(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            log.error("文件删除失败: objectKey={}", objectKey, e);
            return false;
        }
    }

    /**
     * 方法：generatePresignedUpload
     *
     * @author zhanghongyu
     */
    @Override
    public PresignedUploadInfo generatePresignedUpload(String objectKey, long expirationMinutes) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .key(objectKey)
                    .build();
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .putObjectRequest(putObjectRequest)
                    .build();
            return new PresignedUploadInfo(
                    s3Presigner.presignPutObject(presignRequest).url().toString(),
                    objectKey,
                    getFileUrl(objectKey)
            );
        } catch (Exception e) {
            throw new RuntimeException("生成预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 方法：generatePresignedDownload
     *
     * @author zhanghongyu
     */
    @Override
    public String generatePresignedDownload(String objectKey, long expirationMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(rustFsProperties.getBucketName())
                    .key(objectKey)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();
            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            throw new RuntimeException("生成预签名URL失败: " + e.getMessage(), e);
        }
    }

    /**
     * 方法：getFileUrl
     *
     * @author zhanghongyu
     */
    @Override
    public String getFileUrl(String objectKey) {
        String endpoint = rustFsProperties.getEndpoint();
        String bucketName = rustFsProperties.getBucketName();
        String cleanEndpoint = endpoint.replaceAll("/+$", "");
        String cleanObjectKey = objectKey.replaceFirst("^/+", "");
        return String.format("%s/%s/%s", cleanEndpoint, bucketName, cleanObjectKey);
    }
}