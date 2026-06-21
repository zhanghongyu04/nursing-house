package com.zhiling.system.application.service;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.List;

/**
 * RustFS 文件服务接口
 * 提供基于 AWS S3 SDK 的 RustFS 对象存储操作
 *
 * @since 1.0.1
 *
 * @author zhanghongyu
 */
public interface RustFsService {

    /**
     * 上传文件（MultipartFile）
     *
     * @param objectKey 对象键（文件路径）
     * @param file      要上传的文件
     * @return 文件访问 URL
     */
    String uploadFile(String objectKey, MultipartFile file);

    /**
     * 上传文件（字节数组）
     *
     * @param objectKey  对象键（文件路径）
     * @param fileBytes  文件字节数组
     * @param contentType 内容类型（MIME 类型）
     * @return 文件访问 URL
     */
    String uploadFile(String objectKey, byte[] fileBytes, String contentType);

    /**
     * 上传文件（输入流）
     *
     * @param objectKey 对象键（文件路径）
     * @param inputStream 文件输入流
     * @param contentLength 文件大小
     * @param contentType 内容类型（MIME 类型）
     * @return 文件访问 URL
     */
    String uploadFile(String objectKey, InputStream inputStream, long contentLength, String contentType);

    /**
     * 下载文件
     *
     * @param objectKey 对象键（文件路径）
     * @return 文件输入流
     */
    InputStream downloadFile(String objectKey);

    /**
     * 删除文件
     *
     * @param objectKey 对象键（文件路径）
     */
    void deleteFile(String objectKey);

    /**
     * 批量删除文件
     *
     * @param objectKeys 对象键列表
     */
    void deleteFiles(List<String> objectKeys);

    /**
     * 获取文件 URL
     *
     * @param objectKey 对象键（文件路径）
     * @return 文件访问 URL
     */
    String getFileUrl(String objectKey);

    /**
     * 检查文件是否存在
     *
     * @param objectKey 对象键（文件路径）
     * @return true 如果文件存在，否则 false
     */
    boolean fileExists(String objectKey);

    /**
     * 列出桶中的对象
     *
     * @param prefix 前缀过滤（可选）
     * @return 对象列表
     */
    List<S3Object> listObjects(String prefix);

    /**
     * 生成预签名上传 URL（PUT）
     *
     * @param objectKey 对象键（文件路径）
     * @param expirationMinutes 过期时间（分钟）
     * @return 预签名 URL
     */
    String generatePresignedUploadUrl(String objectKey, long expirationMinutes);

    /**
     * 生成预签名下载 URL（GET）
     *
     * @param objectKey 对象键（文件路径）
     * @param expirationMinutes 过期时间（分钟）
     * @return 预签名 URL
     */
    String generatePresignedDownloadUrl(String objectKey, long expirationMinutes);

    /**
     * 创建存储桶（如果不存在）
     */
    void createBucketIfNotExists();

    /**
     * 获取文件元数据
     *
     * @param objectKey 对象键（文件路径）
     * @return 文件元数据
     */
    HeadObjectResponse getFileMetadata(String objectKey);
}

