package com.zhiling.framework.llm.core.service;

/**
 * 系统提示词提供端口。
 *
 * @author zhanghongyu
 */
public interface PromptProvider {

    /**
     * 返回当前生效系统提示词。
     */
    String systemPrompt();
}
