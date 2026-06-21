package com.zhiling.framework.llm.core.service;

import com.zhiling.framework.llm.core.model.ToolResult;

import java.util.Map;

/**
 * 工具路由能力端口。
 *
 * @author zhanghongyu
 */
public interface ToolPort {

    /**
     * 在模型调用前对问题做工具增强。
     *
     * @param prompt 用户问题
     * @param metadata 元数据
     * @return 增强结果
     */
    ToolResult beforeChat(String prompt, Map<String, Object> metadata);
}