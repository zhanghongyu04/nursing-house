package com.zhiling.agent.application.prompt.model;

import java.util.List;

/**
 * 当前启用提示词视图。
 */
public record PromptTemplateActiveView(
        String promptName,
        Integer version,
        List<PromptTemplateDetail> segments,
        String mergedContent
) {
}
