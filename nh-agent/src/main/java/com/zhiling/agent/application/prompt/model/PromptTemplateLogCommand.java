package com.zhiling.agent.application.prompt.model;

/**
 * 提示词加载日志写入命令。
 *
 * @param promptId   提示词主键
 * @param promptName 提示词名称
 * @param version    提示词版本
 * @param oldContent 旧内容
 * @param newContent 新内容
 * @author zhanghongyu
 */
public record PromptTemplateLogCommand(
        Long promptId,
        String promptName,
        Integer version,
        String oldContent,
        String newContent,
        String operationType,
        String operator,
        String remark
) {

    public PromptTemplateLogCommand(Long promptId, String promptName, Integer version,
                                    String oldContent, String newContent) {
        this(promptId, promptName, version, oldContent, newContent,
                "LOAD_REDIS", "system", "应用启动时自动同步提示词到Redis");
    }
}

