package com.zhiling.framework.agent;

import com.zhiling.framework.llm.model.AgentHistoryMessage;

import java.util.List;

/**
 * 会话记忆公开契约。
 *
 * @author zhanghongyu
 */
public interface ConversationMemoryPort {

    void append(String conversationId, List<AgentHistoryMessage> messages);

    List<AgentHistoryMessage> get(String conversationId, int lastN);

    void clear(String conversationId);
}
