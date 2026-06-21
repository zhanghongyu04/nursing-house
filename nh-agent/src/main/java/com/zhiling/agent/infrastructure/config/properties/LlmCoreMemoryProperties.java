package com.zhiling.agent.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM Memory 配置。
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nh.llm.memory")
public class LlmCoreMemoryProperties {

    /**
     * 是否启用记忆能力。
     */
    private boolean enabled = true;

    /**
     * 提供者：in-memory / redis / noop
     */
    private String provider = "in-memory";

    /**
     * Redis key 前缀。
     */
    private String redisKeyPrefix = "llm:memory:";
}

