package com.zhiling.agent.application.prompt.model;

import java.time.LocalDateTime;

/**
 * 提示词日志视图。
 */
public record PromptTemplateLogView(
        Long id,
        Long promptId,
        String promptName,
        Integer promptIndex,
        Integer oldVersion,
        Integer newVersion,
        String oldContent,
        String newContent,
        String operationType,
        String operator,
        String remark,
        LocalDateTime createTime
) {
}
