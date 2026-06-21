package com.zhiling.framework.llm.core.model;

import lombok.Data;

import java.util.Map;

/**
 * 标准化对话请求。
 *
 * @author zhanghongyu
 */
@Data
public class LlmChatRequest {

    /**
     * 用户输入
     */
    private String prompt;

    /**
     * 会话ID（可选）
     */
    private String chatId;

    /**
     * 调用方透传元数据（可选）
     */
    private Map<String, Object> metadata;

    /**
     * 当前用户ID（可选）。
     *
     * 用于在异步/流式线程中显式透传安全上下文，避免依赖线程本地状态。
     */
    private Long userId;
}
