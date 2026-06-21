package com.zhiling.agent.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM Guard 配置。
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nh.llm.guard")
public class LlmCoreGuardProperties {

    /**
     * 是否启用输入/输出防护。
     */
    private boolean enabled = true;
}

