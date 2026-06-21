package com.zhiling.framework.llm.core.model;

import lombok.Builder;
import lombok.Data;

/**
 * 工具路由增强结果。
 *
 * @author zhanghongyu
 */
@Data
@Builder
public class ToolResult {

    /**
     * 工具增强后的提问文本。
     */
    private String prompt;
}