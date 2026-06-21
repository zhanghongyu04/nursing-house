package com.zhiling.system.application.service;


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件服务接口
 * 提供文件上传、下载、删除、预签名 URL 生成等功能
 *
 * @author zhanghongyu
 */
public interface CommonFileService {

    /**
     * 通用文件上传请求(单个)
     * @param file 要上传的文件
     * @return 文件访问 URL
     */
    String upload(MultipartFile file);

    /**
     * 通用文件下载请求
     *
     * @param objectKey 对象键（完整路径，如：20250312/uuid-xxx.jpg）
     * @param response HttpServletResponse
     * @return 下载是否成功
     */
    Boolean download(String objectKey, HttpServletResponse response);

    /**
     * 通用文件删除请求
     * @param objectKey 对象键（完整路径）
     * @return 删除是否成功
     */
    Boolean delete(String objectKey);

    /**
     * 生成上传预签名URL（PUT）
     * 注意：返回的 PresignedUploadResult 包含预签名 URL 和实际的 objectKey
     * 前端需要保存 objectKey，后续下载时需要使用
     *
     * @param originalFileName 原始文件名
     * @return PresignedUploadResult 包含预签名URL和objectKey
     */
    PresignedUploadResult generatePresignedUploadUrl(String originalFileName);

    /**
     * 生成下载预签名URL（GET）
     *
     * @param objectKey 对象键（完整路径，如：20250312/uuid-xxx.jpg）
     * @return 预签名下载 URL
     */
    String generatePresignedDownloadUrl(String objectKey);

    /**
     * 下载文件为字节数组
     * 用于图片代理等场景
     *
     * @param objectKey 对象键（完整路径）
     * @return 文件字节数组，如果文件不存在返回 null
     */
    byte[] downloadAsBytes(String objectKey);

    /**
     * 根据文件访问 URL 下载文件为字节数组。
     *
     * @param fileUrl 文件访问 URL
     * @return 文件字节数组，如果文件不存在返回 null
     */
    byte[] downloadAsBytesByUrl(String fileUrl);

    /**
     * 预签名上传结果
     * 包含预签名 URL 和实际的对象键
 * @author zhanghongyu
 */
    record PresignedUploadResult(
        String presignedUrl,      // 预签名上传 URL
        String objectKey,         // 实际的对象键（带日期路径和UUID）
        String fileUrl           // 文件访问 URL
    ) {}
}

