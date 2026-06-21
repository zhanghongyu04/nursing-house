package com.zhiling.framework.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 分库与命名空间配置。
 */
@Data
@ConfigurationProperties(prefix = "nh.redis")
public class NhRedisProperties {

    /**
     * 认证 Token、验证码等（生产建议 database=0）。
     */
    private int authDatabase = 0;

    /**
     * Agent 会话记忆、提示词缓存等（生产建议 database=1）。
     */
    private int agentDatabase = 1;
}
