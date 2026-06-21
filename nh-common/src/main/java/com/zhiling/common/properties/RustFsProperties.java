package com.zhiling.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RustFS 配置属性
 * RustFS 是一个高性能的 S3 兼容对象存储系统
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nursing-house.rustfs")
public class RustFsProperties {
    /**
     * 是否启用 RustFS
     * 默认: true
     */
    private boolean enabled = true;

    /**
     * RustFS 服务端点
     * 例如: http://localhost:9000
     */
    private String endpoint;

    /**
     * 访问密钥 ID
     * 在 RustFS 控制台创建 Access Key 后获取
     */
    private String accessKeyId;

    /**
     * 密钥访问密钥
     * 在 RustFS 控制台创建 Access Key 后获取
     */
    private String secretAccessKey;

    /**
     * 存储桶名称
     * 系统会自动创建此存储桶
     */
    private String bucketName;

    /**
     * 区域（S3 协议要求）
     * RustFS 使用标准 S3 协议，需要指定区域
     * 默认: us-east-1
     */
    private String region = "us-east-1";

    /**
     * 连接超时时间（毫秒）
     * 默认: 10000 (10秒)
     */
    private int connectionTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     * 默认: 60000 (60秒)
     */
    private int readTimeout = 60000;

    /**
     * 写入超时时间（毫秒）
     * 默认: 60000 (60秒)
     */
    private int writeTimeout = 60000;

    /**
     * 是否启用路径风格访问
     * RustFS 要求启用路径风格访问
     * 启用后 URL 格式: http://localhost:9000/bucket-name/key
     * 默认: true
     */
    private boolean pathStyleAccessEnabled = true;
}