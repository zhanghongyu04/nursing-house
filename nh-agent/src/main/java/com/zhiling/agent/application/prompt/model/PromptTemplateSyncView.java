package com.zhiling.agent.application.prompt.model;

/**
 * 提示词同步结果。
 */
public record PromptTemplateSyncView(
        String promptName,
        String redisKey,
        String content
) {
}
