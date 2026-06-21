package com.zhiling.system.infrastructure.ezviz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
/**
 * 萤石开放平台配置属性
 *
 * @author zhanghongyu
 */
@ConfigurationProperties(prefix = "nursing-house.ezviz")
public class EzvizProperties {
    /**
     * 萤石开放平台 appKey。
     */
    private String appKey;

    /**
     * 萤石开放平台 appSecret。
     */
    private String appSecret;

    /**
     * 开放平台域名。
     */
    private String baseUrl = "https://open.ys7.com";

    /**
     * token 提前刷新秒数。
     */
    private long tokenRefreshBeforeExpireSeconds = 300;

    /**
     * HTTP 连接超时毫秒。
     */
    private int connectTimeoutMs = 5000;

    /**
     * HTTP 读取超时毫秒。
     */
    private int readTimeoutMs = 10000;

    /**
     * 方法：isConfigured
     *
     * @author zhanghongyu
     */
    public boolean isConfigured() {
        return appKey != null && !appKey.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }
}