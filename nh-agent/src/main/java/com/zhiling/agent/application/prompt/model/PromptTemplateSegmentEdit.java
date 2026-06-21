package com.zhiling.agent.application.prompt.model;

/**
 * 提示词片段编辑项。
 */
public record PromptTemplateSegmentEdit(
        Integer promptIndex,
        String promptContent
) {
}
