package com.zhiling.agent.infrastructure.persistence.repository;

import com.zhiling.agent.application.port.AgentContextMemoryRecoveryPort;
import com.zhiling.agent.application.repository.AgentMessageRepository;
import com.zhiling.framework.llm.core.model.MemoryMessage;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 基于 MySQL 会话消息明细的模型上下文恢复实现。
 *
 * @author zhanghongyu
 */
@Repository
public class MysqlAgentContextMemoryRecoveryPort implements AgentContextMemoryRecoveryPort {

    private final AgentMessageRepository agentMessageRepository;

    public MysqlAgentContextMemoryRecoveryPort(AgentMessageRepository agentMessageRepository) {
        this.agentMessageRepository = agentMessageRepository;
    }

    @Override
    public List<MemoryMessage> recover(Long userId, String conversationId, int limit) {
        if (userId == null || conversationId == null || conversationId.isBlank() || limit <= 0) {
            return List.of();
        }
        return agentMessageRepository.listRecent(userId, conversationId, limit).stream()
                .filter(message -> message.getRole() != null && message.getContent() != null
                        && !message.getContent().isBlank())
                .map(message -> MemoryMessage.builder()
                        .role(message.getRole())
                        .content(message.getContent())
                        .build())
                .toList();
    }
}
