package com.zhiling.agent.application.prompt.model;

/**
 * 提示词预览结果。
 */
public record PromptTemplatePreviewView(
        String promptName,
        Integer version,
        String mergedContent
) {
}
