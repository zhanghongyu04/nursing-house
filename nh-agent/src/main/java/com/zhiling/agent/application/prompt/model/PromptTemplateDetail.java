package com.zhiling.agent.application.prompt.model;

import java.time.LocalDateTime;

/**
 * 提示词片段详情。
 */
public record PromptTemplateDetail(
        Long id,
        String promptName,
        Integer promptIndex,
        String promptContent,
        Integer version,
        Integer status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
