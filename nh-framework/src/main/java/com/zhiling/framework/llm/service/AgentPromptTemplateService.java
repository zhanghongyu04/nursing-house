package com.zhiling.framework.llm.service;

/**
 * Agent 提示词配置服务。
 *
 * @author zhanghongyu
 */
public interface AgentPromptTemplateService {

    /**
     * 从数据库加载指定提示词并同步到 Redis，返回最终提示词内容。
     */
    String loadPromptToRedis(String promptName);
}