package com.zhiling.agent.domain.prompt;

import com.zhiling.framework.llm.core.service.PromptProvider;

/**
 * 从容器中读取系统提示词的 PromptProvider。
 *
 * @author zhanghongyu
 */
public class BeanPromptProvider implements PromptProvider {

    private final String systemPrompt;

    /**
     * 构造器：BeanPromptProvider
     *
     * @author zhanghongyu
     */
    public BeanPromptProvider(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    /**
     * 方法：systemPrompt
     *
     * @author zhanghongyu
     */
    @Override
    public String systemPrompt() {
        return systemPrompt;
    }
}
