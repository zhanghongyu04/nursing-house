package com.zhiling.agent.infrastructure.persistence.repository;

import com.zhiling.agent.application.repository.AgentMessageRepository;
import com.zhiling.agent.infrastructure.persistence.mapper.AgentMessagePersistenceMapper;
import com.zhiling.model.entity.AgentMessage;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Agent 会话消息明细仓储实现。
 *
 * @author zhanghongyu
 */
@Repository
public class AgentMessageRepositoryImpl implements AgentMessageRepository {

    private final AgentMessagePersistenceMapper agentMessageMapper;

    /**
     * 构造器：AgentMessageRepositoryImpl
     *
     * @author zhanghongyu
     */
    public AgentMessageRepositoryImpl(AgentMessagePersistenceMapper agentMessageMapper) {
        this.agentMessageMapper = agentMessageMapper;
    }

    /**
     * 方法：saveBatch
     *
     * @author zhanghongyu
     */
    @Override
    public void appendMessages(Long userId, String conversationId, String messageType, List<AgentMessage> messages) {
        if (conversationId == null || conversationId.isBlank() || messages == null || messages.isEmpty()) {
            return;
        }
        int seqNo = agentMessageMapper.selectMaxSeqNo(userId, conversationId) + 1;
        for (AgentMessage message : messages) {
            message.setUserId(userId);
            message.setConversationId(conversationId);
            message.setMessageType(messageType);
            message.setSeqNo(seqNo++);
            message.setStatus(0);
            agentMessageMapper.insert(message);
        }
    }

    /**
     * 方法：listByConversationIdAndUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentMessage> listByConversation(Long userId, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        return agentMessageMapper.selectByConversation(userId, conversationId);
    }

    /**
     * 方法：listRecent
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentMessage> listRecent(Long userId, String conversationId, int limit) {
        if (conversationId == null || conversationId.isBlank() || limit <= 0) {
            return List.of();
        }
        return agentMessageMapper.selectRecent(userId, conversationId, limit);
    }

    /**
     * 方法：softDeleteByConversation
     *
     * @author zhanghongyu
     */
    @Override
    public boolean softDeleteByConversation(Long userId, String conversationId) {
        return agentMessageMapper.softDeleteByConversation(userId, conversationId) > 0;
    }
}
