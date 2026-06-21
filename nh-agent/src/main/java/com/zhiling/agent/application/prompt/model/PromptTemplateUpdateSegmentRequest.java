package com.zhiling.agent.application.prompt.model;

/**
 * 更新提示词片段请求。
 */
public record PromptTemplateUpdateSegmentRequest(
        String promptContent,
        String remark,
        Boolean syncToRedis
) {
}
