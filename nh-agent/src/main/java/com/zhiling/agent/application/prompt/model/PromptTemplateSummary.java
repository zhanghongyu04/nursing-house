package com.zhiling.agent.application.prompt.model;

import java.time.LocalDateTime;

/**
 * 提示词版本摘要。
 */
public record PromptTemplateSummary(
        String promptName,
        Integer version,
        Integer status,
        Long segmentCount,
        LocalDateTime updateTime
) {
}
