package com.zhiling.agent.application.prompt.model;

/**
 * 系统支持的提示词类型。
 */
public record PromptSupportedTypeView(
        String promptName,
        String label,
        String description,
        Boolean exists,
        Integer maxVersion,
        Integer activeVersion
) {
}
