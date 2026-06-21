package com.zhiling.framework.llm.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.framework.llm.model.AgentSessionCreateDto;
import com.zhiling.framework.llm.model.AgentSessionUpdateDto;
import com.zhiling.framework.llm.model.AgentSessionVo;

import java.util.List;

/**
 * Agent 会话管理服务。
 *
 * @author zhanghongyu
 */
public interface AgentSessionService {

    AgentSessionVo createSession(AgentSessionCreateDto dto);

    List<AgentSessionVo> getUserSessions(String sessionType);

    PageResult getUserSessionsPage(Integer page, Integer pageSize, String sessionType);

    AgentSessionVo getSessionByConversationId(String conversationId);

    Boolean updateSession(AgentSessionUpdateDto dto);

    Boolean deleteSession(String conversationId);

    Boolean updateSessionStatus(String conversationId, Integer status);
}

