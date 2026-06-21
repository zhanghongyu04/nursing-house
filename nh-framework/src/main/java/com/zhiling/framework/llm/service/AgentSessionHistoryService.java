package com.zhiling.framework.llm.service;

import com.zhiling.framework.llm.model.AgentHistoryMessage;

import java.util.List;

/**
 * Agent 会话与历史消息服务。
 *
 * @author zhanghongyu
 */
public interface AgentSessionHistoryService {

    void saveConversationReference(String chatId);

    void saveInterceptExchange(String chatId, String userPrompt, String assistantReply);

    List<String> getChatIds(String type);

    List<AgentHistoryMessage> getChatHistory(String type, String chatId);

    void ensureConversationAccessible(String conversationId, String requestType);
}