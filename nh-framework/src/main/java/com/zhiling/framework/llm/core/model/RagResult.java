package com.zhiling.framework.llm.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * RAG 增强结果。
 *
 * @author zhanghongyu
 */
@Data
@Builder
public class RagResult {

    /**
     * 增强后的提问文本。
     */
    private String prompt;

    /**
     * 检索来源。
     */
    private List<String> sources;
}