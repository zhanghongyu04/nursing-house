package com.zhiling.framework.agent;

import com.zhiling.framework.llm.service.AgentSessionService;

/**
 * Agent 会话公开契约。
 *
 * 当前先与既有 AgentSessionService 对齐，作为后续归位的稳定入口。
 *
 * @author zhanghongyu
 */
public interface AgentSessionPort extends AgentSessionService {
}
