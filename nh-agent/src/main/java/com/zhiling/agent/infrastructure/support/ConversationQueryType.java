package com.zhiling.agent.infrastructure.support;

/**
 * 对话查询类型。
 *
 * @author zhanghongyu
 */
public enum ConversationQueryType {

    /**
     * 总结、回顾、梳理当前会话。
     */
    CONVERSATION_META,

    /**
     * 查询系统内部业务数据。
     */
    INTERNAL_DATA,

    /**
     * 追问或复用历史媒体内容。
     */
    MEDIA_REFERENCE,

    /**
     * 寒暄、确认、感谢等轻量对话。
     */
    SMALL_TALK,

    /**
     * 需要知识库支撑的护理/医养知识问答。
     */
    KNOWLEDGE,

    /**
     * 普通开放对话，不默认检索知识库。
     */
    GENERAL
}
