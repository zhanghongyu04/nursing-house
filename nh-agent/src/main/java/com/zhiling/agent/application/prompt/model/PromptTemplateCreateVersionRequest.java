package com.zhiling.agent.application.prompt.model;

import java.util.List;

/**
 * 新增提示词版本请求。
 */
public record PromptTemplateCreateVersionRequest(
        List<PromptTemplateSegmentEdit> segments,
        String remark,
        Boolean syncToRedis
) {
}
