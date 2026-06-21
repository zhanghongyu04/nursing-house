package com.zhiling.agent.application.repository;

import com.zhiling.model.entity.AgentMessage;

import java.util.List;

/**
 * Agent 会话消息明细仓储。
 *
 * @author zhanghongyu
 */
public interface AgentMessageRepository {

    void appendMessages(Long userId, String conversationId, String messageType, List<AgentMessage> messages);

    List<AgentMessage> listByConversation(Long userId, String conversationId);

    List<AgentMessage> listRecent(Long userId, String conversationId, int limit);

    boolean softDeleteByConversation(Long userId, String conversationId);
}
