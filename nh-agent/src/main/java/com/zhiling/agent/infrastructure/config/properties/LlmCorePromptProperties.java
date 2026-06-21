package com.zhiling.agent.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM Prompt 配置。
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nh.llm.prompt")
public class LlmCorePromptProperties {

    /**
     * 默认系统提示词。
     */
    private String system = "你是一个养老院管理系统的专业助手，请基于事实回答。";
}

