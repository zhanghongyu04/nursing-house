package com.zhiling.agent.application.prompt;

import cn.hutool.core.util.StrUtil;
import com.zhiling.agent.application.prompt.model.PromptTemplateSegment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提示词片段合并器。
 */
@Component
public class PromptTemplateMerger {

    /**
     * 按同步 Redis 的相同规则合并片段。
     */
    public String merge(List<PromptTemplateSegment> segments) {
        StringBuilder merged = new StringBuilder();
        if (segments == null || segments.isEmpty()) {
            return "";
        }
        for (PromptTemplateSegment segment : segments) {
            if (segment == null || StrUtil.isBlank(segment.promptContent())) {
                continue;
            }
            if (merged.length() > 0) {
                merged.append("\n\n");
            }
            merged.append(segment.promptContent().trim());
        }
        return merged.toString();
    }
}
