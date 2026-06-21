package com.zhiling.system.config;

import com.zhiling.common.properties.RustFsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;

/**
 * RustFS 配置类
 * 使用 AWS S3 SDK 连接 RustFS（S3 兼容的对象存储）
 *
 * 只负责注册底层 S3 客户端 Bean，业务服务由 {@code RustFsServiceImpl} 自行注册。
 *
 * @author zhanghongyu
 */
@Configuration
@EnableConfigurationProperties(RustFsProperties.class)
@ConditionalOnProperty(prefix = "nursing-house.rustfs", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RustFsConfig {

    private final RustFsProperties rustFsProperties;

    /**
     * 构造器：RustFsConfig
     *
     * @author zhanghongyu
     */
    public RustFsConfig(RustFsProperties rustFsProperties) {
        this.rustFsProperties = rustFsProperties;
    }

    /**
     * 创建 S3Client Bean 用于连接 RustFS
     *
     * RustFS 完全兼容 S3 API，因此使用标准的 AWS S3 SDK
     * 需要注意的配置：
     * 1. 使用 path-style 访问模式（RustFS 要求）
     * 2. 自定义 endpoint（指向 RustFS 服务器）
     * 3. 使用自定义凭证（RustFS Access Key）
     */
    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            rustFsProperties.getAccessKeyId(),
            rustFsProperties.getSecretAccessKey()
        );

        S3Configuration s3Config = S3Configuration.builder()
            .pathStyleAccessEnabled(rustFsProperties.isPathStyleAccessEnabled())
            .build();

        UrlConnectionHttpClient.Builder httpClientBuilder = UrlConnectionHttpClient.builder()
            .connectionTimeout(Duration.ofMillis(rustFsProperties.getConnectionTimeout()))
            .socketTimeout(Duration.ofMillis(rustFsProperties.getReadTimeout()));

        return S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(rustFsProperties.getRegion()))
            .endpointOverride(URI.create(rustFsProperties.getEndpoint()))
            .serviceConfiguration(s3Config)
            .httpClientBuilder(httpClientBuilder)
            .build();
    }

    /**
     * 创建 S3Presigner Bean 用于生成预签名 URL
     *
     * 预签名 URL 允许前端直接与 RustFS 交互，减轻服务器负担
     */
    @Bean
    public S3Presigner s3Presigner() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            rustFsProperties.getAccessKeyId(),
            rustFsProperties.getSecretAccessKey()
        );

        S3Configuration s3Config = S3Configuration.builder()
            .pathStyleAccessEnabled(rustFsProperties.isPathStyleAccessEnabled())
            .build();

        return S3Presigner.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(rustFsProperties.getRegion()))
            .endpointOverride(URI.create(rustFsProperties.getEndpoint()))
            .serviceConfiguration(s3Config)
            .build();
    }
}