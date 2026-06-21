package com.zhiling.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Qdrant 向量数据库配置属性
 *
 * @since 1.0.1
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nursing-house.qdrant")
public class QdrantProperties {

    /**
     * 是否启用 Qdrant
     */
    private Boolean enabled = true;

    /**
     * Qdrant 服务器地址
     */
    private String host = "localhost";

    /**
     * Qdrant gRPC 端口（默认 6334）
     */
    private Integer port = 6334;

    /**
     * 集合名称
     */
    private String collectionName = "nursing-home-docs";

    /**
     * 向量维度（text-embedding-v3 为 1024）
     */
    private Integer vectorSize = 1024;

    /**
     * 是否初始化 Schema（自动创建集合）
     */
    private Boolean initializeSchema = true;

    /**
     * API Key（可选）
     */
    private String apiKey = "";

    /**
     * 是否使用 HTTPS
     */
    private Boolean useHttps = false;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectionTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 30000;

}
