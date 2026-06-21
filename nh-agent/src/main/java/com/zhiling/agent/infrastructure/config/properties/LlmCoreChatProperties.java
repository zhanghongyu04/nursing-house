package com.zhiling.agent.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM 聊天配置。
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nh.llm.chat")
public class LlmCoreChatProperties {

    /**
     * 是否启用聊天能力。
     */
    private boolean enabled = true;

    /**
     * 能力接口是否暴露。
     */
    private boolean exposeApi = true;

    /**
     * 上下文拼装时加载的历史消息上限。
     */
    private int contextMessageLimit = 12;
}

