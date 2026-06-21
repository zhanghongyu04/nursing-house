package com.zhiling.agent.application.prompt.model;

/**
 * 提示词启停请求。
 */
public record PromptTemplateStatusRequest(
        String remark,
        Boolean syncToRedis
) {
}
