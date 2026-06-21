package com.zhiling.agent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.entity.AgentSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Agent 会话持久化 Mapper。
 *
 * @author zhanghongyu
 */
@Mapper
public interface AgentSessionPersistenceMapper extends BaseMapper<AgentSession> {

    default AgentSession selectByConversationId(String conversationId) {
        return selectOne(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getConversationId, conversationId)
                .eq(AgentSession::getStatus, 0));
    }

    default List<AgentSession> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getUserId, userId)
                .eq(AgentSession::getStatus, 0)
                .orderByDesc(AgentSession::getCreateTime));
    }

    default List<AgentSession> selectByUserIdAndType(Long userId, String sessionType) {
        return selectList(new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getUserId, userId)
                .eq(AgentSession::getSessionType, sessionType)
                .eq(AgentSession::getStatus, 0)
                .orderByDesc(AgentSession::getCreateTime));
    }

    default IPage<AgentSession> selectPageByUserId(Page<AgentSession> page, Long userId, String sessionType) {
        LambdaQueryWrapper<AgentSession> wrapper = new LambdaQueryWrapper<AgentSession>()
                .eq(AgentSession::getUserId, userId)
                .eq(AgentSession::getStatus, 0)
                .orderByDesc(AgentSession::getCreateTime);
        if (sessionType != null && !sessionType.isBlank()) {
            wrapper.eq(AgentSession::getSessionType, sessionType);
        }
        return selectPage(page, wrapper);
    }

    default Boolean softDeleteSession(Long sessionId) {
        AgentSession session = new AgentSession();
        session.setId(sessionId);
        session.setStatus(1);
        return updateById(session) > 0;
    }

    default Boolean updateStatus(String conversationId, Integer status) {
        AgentSession session = selectByConversationId(conversationId);
        if (session == null) {
            return false;
        }
        session.setStatus(status);
        return updateById(session) > 0;
    }
}