package com.zhiling.agent.application.repository;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.entity.AgentSession;

import java.util.List;

/**

 * AgentSessionRepository

 *

 * @author zhanghongyu

 */

public interface AgentSessionRepository {

    void save(AgentSession agentSession);

    List<AgentSession> listByUserId(Long userId);

    List<AgentSession> listByUserIdAndType(Long userId, String sessionType);

    PageResult pageByUserId(Long userId, Integer page, Integer pageSize, String sessionType);

    AgentSession findByConversationId(String conversationId);

    boolean updateById(AgentSession session);

    boolean softDeleteById(Long sessionId);

    boolean updateStatus(String conversationId, Integer status);
}

