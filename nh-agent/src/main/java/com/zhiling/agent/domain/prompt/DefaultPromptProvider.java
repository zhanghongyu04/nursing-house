package com.zhiling.agent.domain.prompt;

import com.zhiling.framework.llm.core.service.PromptProvider;
import com.zhiling.agent.infrastructure.config.properties.LlmCorePromptProperties;

/**
 * 默认 Prompt 实现。
 *
 * @author zhanghongyu
 */
public class DefaultPromptProvider implements PromptProvider {

    private final LlmCorePromptProperties properties;

    /**
     * 构造器：DefaultPromptProvider
     *
     * @author zhanghongyu
     */
    public DefaultPromptProvider(LlmCorePromptProperties properties) {
        this.properties = properties;
    }

    /**
     * 方法：systemPrompt
     *
     * @author zhanghongyu
     */
    @Override
    public String systemPrompt() {
        return properties.getSystem();
    }
}
