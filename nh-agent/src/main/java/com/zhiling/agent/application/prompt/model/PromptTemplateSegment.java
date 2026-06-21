package com.zhiling.agent.application.prompt.model;

/**
 * 提示词片段模型。
 *
 * @param id            片段所属提示词主键
 * @param promptContent 片段内容
 * @author zhanghongyu
 */
public record PromptTemplateSegment(Long id, String promptContent) {
}

