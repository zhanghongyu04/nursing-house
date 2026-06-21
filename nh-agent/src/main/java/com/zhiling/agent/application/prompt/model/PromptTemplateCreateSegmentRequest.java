package com.zhiling.agent.application.prompt.model;

/**
 * 新增提示词片段请求。
 */
public record PromptTemplateCreateSegmentRequest(
        Integer promptIndex,
        String promptContent,
        String remark,
        Boolean syncToRedis
) {
}
