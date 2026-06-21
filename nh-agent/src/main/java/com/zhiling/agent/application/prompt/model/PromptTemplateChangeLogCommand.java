package com.zhiling.agent.application.prompt.model;

/**
 * 提示词变更日志写入命令。
 */
public record PromptTemplateChangeLogCommand(
        Long promptId,
        String promptName,
        Integer promptIndex,
        Integer oldVersion,
        Integer newVersion,
        String oldContent,
        String newContent,
        String operationType,
        String operator,
        String remark
) {
}
