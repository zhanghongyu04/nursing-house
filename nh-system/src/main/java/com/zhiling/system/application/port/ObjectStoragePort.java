package com.zhiling.system.application.port;

import java.io.InputStream;

/**
 * 对象存储抽象。
 *
 * @author zhanghongyu
 */
public interface ObjectStoragePort {

    record StoredObject(String objectKey, byte[] bytes) {
    }

    record PresignedUploadInfo(String presignedUrl, String objectKey, String fileUrl) {
    }

    void ensureBucketReady();

    String upload(String objectKey, InputStream inputStream, long contentLength, String contentType);

    InputStream download(String objectKey);

    byte[] downloadAsBytes(String objectKey);

    boolean delete(String objectKey);

    PresignedUploadInfo generatePresignedUpload(String objectKey, long expirationMinutes);

    String generatePresignedDownload(String objectKey, long expirationMinutes);

    String getFileUrl(String objectKey);
}