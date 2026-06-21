package com.zhiling.agent.application.session;

import cn.hutool.core.util.IdUtil;
import com.zhiling.agent.application.repository.AgentMessageRepository;
import com.zhiling.agent.application.repository.AgentSessionRepository;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.common.result.PageResult;
import com.zhiling.framework.agent.AgentSessionPort;
import com.zhiling.framework.agent.ConversationHistoryPort;
import com.zhiling.framework.agent.ConversationMemoryPort;
import com.zhiling.framework.llm.model.AgentSessionCreateDto;
import com.zhiling.framework.llm.model.AgentSessionUpdateDto;
import com.zhiling.framework.llm.model.AgentSessionVo;
import com.zhiling.framework.llm.service.AgentSessionService;
import com.zhiling.framework.security.SecurityHelper;
import org.springframework.security.access.AccessDeniedException;
import com.zhiling.model.entity.AgentSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agent 会话公开契约实现。
 *
 * @author zhanghongyu
 */
@Service("agentSessionPort")
@Slf4j
public class AgentSessionPortService implements AgentSessionPort, AgentSessionService {

    private final AgentSessionRepository agentSessionRepository;
    private final ConversationHistoryPort conversationHistoryPort;
    private final ConversationMemoryPort conversationMemoryPort;
    private final AgentMessageRepository agentMessageRepository;
    private final SecurityHelper securityHelper;

    public AgentSessionPortService(AgentSessionRepository agentSessionRepository,
                                   ConversationHistoryPort conversationHistoryPort,
                                   ConversationMemoryPort conversationMemoryPort,
                                   AgentMessageRepository agentMessageRepository,
                                   SecurityHelper securityHelper) {
        this.agentSessionRepository = agentSessionRepository;
        this.conversationHistoryPort = conversationHistoryPort;
        this.conversationMemoryPort = conversationMemoryPort;
        this.agentMessageRepository = agentMessageRepository;
        this.securityHelper = securityHelper;
    }

    /**
     * 方法：createSession
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentSessionVo createSession(AgentSessionCreateDto dto) {
        Long userId = securityHelper.getCurrentUserId();
        String conversationId = IdUtil.simpleUUID();
        String normalizedSessionType = normalizeSessionType(dto.getSessionType());

        AgentSession session = AgentSession.builder()
                .userId(userId)
                .conversationId(conversationId)
                .title(dto.getTitle() != null ? dto.getTitle() : "新对话")
                .sessionType(normalizedSessionType)
                .build();
        session.setStatus(0);

        agentSessionRepository.save(session);
        conversationHistoryPort.save(session.getSessionType(), conversationId);
        return convertToVo(session);
    }

    /**
     * 方法：getUserSessions
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentSessionVo> getUserSessions(String sessionType) {
        Long userId = securityHelper.getCurrentUserId();
        List<AgentSession> sessions = (sessionType != null && !sessionType.trim().isEmpty())
                ? agentSessionRepository.listByUserIdAndType(userId, sessionType)
                : agentSessionRepository.listByUserId(userId);
        return sessions.stream().map(this::convertToVo).toList();
    }

    /**
     * 方法：getUserSessionsPage
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult getUserSessionsPage(Integer page, Integer pageSize, String sessionType) {
        Long userId = securityHelper.getCurrentUserId();
        PageResult result = agentSessionRepository.pageByUserId(userId, page, pageSize, sessionType);
        @SuppressWarnings("unchecked")
        List<AgentSession> records = (List<AgentSession>) result.getRecords();
        List<AgentSessionVo> vos = records.stream().map(this::convertToVo).toList();
        return new PageResult(result.getTotal(), vos);
    }

    /**
     * 方法：getSessionByConversationId
     *
     * @author zhanghongyu
     */
    @Override
    public AgentSessionVo getSessionByConversationId(String conversationId) {
        AgentSession session = agentSessionRepository.findByConversationId(conversationId);
        if (session == null) {
            return null;
        }
        requireSessionOwnership(session);
        return convertToVo(session);
    }

    /**
     * 方法：updateSession
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateSession(AgentSessionUpdateDto dto) {
        AgentSession session = agentSessionRepository.findByConversationId(dto.getConversationId());
        if (session == null) {
            throw new ProjectException(404, "会话不存在");
        }
        requireSessionOwnership(session);
        if (dto.getTitle() != null) {
            session.setTitle(dto.getTitle());
        }
        if (dto.getStatus() != null) {
            session.setStatus(dto.getStatus());
        }
        return agentSessionRepository.updateById(session);
    }

    /**
     * 方法：deleteSession
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSession(String conversationId) {
        AgentSession session = agentSessionRepository.findByConversationId(conversationId);
        if (session == null) {
            throw new ProjectException(404, "会话不存在");
        }
        requireSessionOwnership(session);

        try {
            conversationMemoryPort.clear(conversationId);
            log.info("已清理Agent Redis中的聊天记录，conversationId={}", conversationId);
        } catch (Exception ex) {
            log.error("清理Agent聊天记录失败，conversationId={}", conversationId, ex);
        }

        agentMessageRepository.softDeleteByConversation(session.getUserId(), conversationId);
        conversationHistoryPort.remove(session.getSessionType(), conversationId);
        return agentSessionRepository.softDeleteById(session.getId());
    }

    /**
     * 方法：updateSessionStatus
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean updateSessionStatus(String conversationId, Integer status) {
        AgentSession session = agentSessionRepository.findByConversationId(conversationId);
        if (session == null) {
            throw new ProjectException(404, "会话不存在");
        }
        requireSessionOwnership(session);
        return agentSessionRepository.updateStatus(conversationId, status);
    }

    /**
     * 方法：convertToVo
     *
     * @author zhanghongyu
     */
    private AgentSessionVo convertToVo(AgentSession session) {
        return AgentSessionVo.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .conversationId(session.getConversationId())
                .title(session.getTitle())
                .sessionType(normalizeSessionType(session.getSessionType()))
                .status(session.getStatus())
                .createTime(session.getCreateTime())
                .updateTime(session.getUpdateTime())
                .build();
    }

    /**
     * 方法：normalizeSessionType
     *
     * @author zhanghongyu
     */
    private String normalizeSessionType(String sessionType) {
        if ("pdf".equalsIgnoreCase(sessionType)) {
            return "pdf";
        }
        return "chat";
    }

    /**
     * 方法：requireSessionOwnership
     *
     * @author zhanghongyu
     */
    private void requireSessionOwnership(AgentSession session) {
        Long currentUserId = securityHelper.getCurrentUserId();
        if (!session.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("无权操作该会话");
        }
    }
}
