package com.zhiling.framework.llm.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 标准化对话响应。
 *
 * @author zhanghongyu
 */
@Data
@Builder
public class LlmChatResponse {

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * 模型回复文本
     */
    private String content;

    /**
     * 能力开关快照
     */
    private Map<String, Boolean> capabilityFlags;

    /**
     * 降级原因（无降级可为空）
     */
    private String degradeReason;

    /**
     * 来源信息（RAG/工具等）
     */
    private List<String> sources;
}
