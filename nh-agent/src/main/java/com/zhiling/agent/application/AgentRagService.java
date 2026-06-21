package com.zhiling.agent.application;

import com.zhiling.framework.llm.model.AgentRagSummary;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Agent RAG 预检索服务。
 *
 * @author zhanghongyu
 */
public interface AgentRagService {

    AgentRagSummary inspectKnowledgeRetrieval(String query);

    AgentRagSummary emptySummary();

    void writeRagHeaders(HttpServletResponse response, AgentRagSummary summary);
}