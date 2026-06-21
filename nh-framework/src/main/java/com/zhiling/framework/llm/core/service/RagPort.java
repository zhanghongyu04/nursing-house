package com.zhiling.framework.llm.core.service;

import com.zhiling.framework.llm.core.model.RagResult;

import java.util.Map;

/**
 * RAG 能力端口。
 *
 * @author zhanghongyu
 */
public interface RagPort {

    /**
     * 对输入问题执行检索增强。
     *
     * @param prompt 用户问题
     * @param metadata 元数据
     * @return 增强结果
     */
    RagResult augment(String prompt, Map<String, Object> metadata);
}