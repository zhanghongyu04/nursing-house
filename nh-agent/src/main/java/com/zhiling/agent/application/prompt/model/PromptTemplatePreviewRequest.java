package com.zhiling.agent.application.prompt.model;

import java.util.List;

/**
 * 提示词预览请求。
 */
public record PromptTemplatePreviewRequest(
        String promptName,
        Integer version,
        List<String> segments
) {
}
