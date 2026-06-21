package com.zhiling.common.constant;

/**
 * 智能体提示词相关常量
 *
 * @author zhanghongyu
 */
public class PromptConstant {

    /**
     * Redis 提示词缓存前缀
     */
    public static final String AGENT_PROMPT_CACHE_PREFIX = "agent:prompt:";

    /**
     * 默认系统提示词名称
     */
    public static final String AGENT_CHAT_ROLE_PROMPT_NAME = "CHAT_ROLE";

    /**
     * 知识库问答检索增强提示词名称
     */
    public static final String AGENT_CHAT_KNOWLEDGE_RAG_PROMPT_NAME = "CHAT_KNOWLEDGE_RAG";

    /**
     * 多模态补充提示词名称
     */
    public static final String AGENT_CHAT_MULTIMODAL_PROMPT_NAME = "CHAT_MULTIMODAL_SUPPLEMENT";

    /**
     * 默认系统提示词 Redis Key
     */
    public static final String AGENT_CHAT_ROLE_PROMPT_REDIS_KEY =
            AGENT_PROMPT_CACHE_PREFIX + AGENT_CHAT_ROLE_PROMPT_NAME;
}
