package com.zhiling.agent.application.prompt.model;

/**
 * 提示词缓存视图。
 */
public record PromptTemplateCacheView(
        String promptName,
        String redisKey,
        String content
) {
}
