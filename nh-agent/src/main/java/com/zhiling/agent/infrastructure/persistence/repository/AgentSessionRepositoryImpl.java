package com.zhiling.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.agent.application.repository.AgentSessionRepository;
import com.zhiling.agent.infrastructure.persistence.mapper.AgentSessionPersistenceMapper;
import com.zhiling.common.result.PageResult;
import com.zhiling.model.entity.AgentSession;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
/**
 * AgentSessionRepositoryImpl
 *
 * @author zhanghongyu
 */
public class AgentSessionRepositoryImpl implements AgentSessionRepository {

    private final AgentSessionPersistenceMapper agentSessionMapper;

    /**
     * 构造器：AgentSessionRepositoryImpl
     *
     * @author zhanghongyu
     */
    public AgentSessionRepositoryImpl(AgentSessionPersistenceMapper agentSessionMapper) {
        this.agentSessionMapper = agentSessionMapper;
    }

    /**
     * 方法：save
     *
     * @author zhanghongyu
     */
    @Override
    public void save(AgentSession agentSession) {
        agentSessionMapper.insert(agentSession);
    }

    /**
     * 方法：listByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentSession> listByUserId(Long userId) {
        return agentSessionMapper.selectByUserId(userId);
    }

    /**
     * 方法：listByUserIdAndType
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentSession> listByUserIdAndType(Long userId, String sessionType) {
        return agentSessionMapper.selectByUserIdAndType(userId, sessionType);
    }

    /**
     * 方法：pageByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult pageByUserId(Long userId, Integer page, Integer pageSize, String sessionType) {
        Page<AgentSession> pageParam = new Page<>(page, pageSize);
        IPage<AgentSession> result = agentSessionMapper.selectPageByUserId(pageParam, userId, sessionType);
        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 方法：findByConversationId
     *
     * @author zhanghongyu
     */
    @Override
    public AgentSession findByConversationId(String conversationId) {
        return agentSessionMapper.selectByConversationId(conversationId);
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(AgentSession session) {
        return agentSessionMapper.updateById(session) > 0;
    }

    /**
     * 方法：softDeleteById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean softDeleteById(Long sessionId) {
        return agentSessionMapper.softDeleteSession(sessionId);
    }

    /**
     * 方法：updateStatus
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateStatus(String conversationId, Integer status) {
        return agentSessionMapper.updateStatus(conversationId, status);
    }
}