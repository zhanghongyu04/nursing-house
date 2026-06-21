package com.zhiling.common.utils;

import com.zhiling.common.properties.RustFsProperties;

/**
 * 文件工具类
 *
 * @author zhanghongyu
 */
public class FileUtil {
    private final RustFsProperties rustFsProperties;

    /**
     * 构造器：FileUtil
     *
     * @author zhanghongyu
     */
    public FileUtil(RustFsProperties rustFsProperties) {
        this.rustFsProperties = rustFsProperties;
    }

    /**
     * 从文件 URL 中提取文件名
     * 支持格式: http://localhost:9000/bucket-name/path/to/file.jpg
     *
     * @param url 文件访问 URL
     * @return 文件名（包括路径），如果无法提取则返回 null
     */
    public String extractFileNameFromUrl(String url) {
        try {
            String bucketName = rustFsProperties.getBucketName(); // nursing-home

            // 找到 bucketName 在 url 中的位置
            int idx = url.indexOf(bucketName);
            if (idx == -1) {
                // 没找到 bucketName，可能是无效的 URL
                return null;
            }

            // 文件名从 bucketName 后面开始截取
            // 格式: http://localhost:9000/nursing-home/20250312/uuid-file.jpg
            // 我们需要获取: 20250312/uuid-file.jpg
            int pathStartIdx = idx + bucketName.length() + 1; // 跳过 "/"

            if (pathStartIdx >= url.length()) {
                return null;
            }

            return url.substring(pathStartIdx);

        } catch (Exception e) {
            // 提取失败
            return null;
        }
    }

    /**
     * 从文件 URL 中提取对象键（Object Key）
     * 对象键是 S3 协议中的术语，相当于文件名（包含路径）
     *
     * @param url 文件访问 URL
     * @return 对象键，如果无法提取则返回 null
     */
    public String extractObjectKeyFromUrl(String url) {
        // 与 extractFileNameFromUrl 方法相同，S3 中称为 Object Key
        return extractFileNameFromUrl(url);
    }

    /**
     * 验证 URL 是否为有效的 RustFS 文件 URL
     *
     * @param url 要验证的 URL
     * @return true 如果 URL 包含有效的端点和存储桶，否则 false
     */
    public boolean isValidRustFsUrl(String url) {
        try {
            String endpoint = rustFsProperties.getEndpoint();
            String bucketName = rustFsProperties.getBucketName();

            return url != null
                    && url.contains(endpoint)
                    && url.contains(bucketName);

        } catch (Exception e) {
            return false;
        }
    }
}