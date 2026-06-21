package com.zhiling.agent.application;

import com.zhiling.framework.llm.model.AgentHistoryUserPayload;
import com.zhiling.framework.llm.model.AgentAttachment;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Agent 对话会话编排服务。
 *
 * @author zhanghongyu
 */
public interface AgentConversationService {

    String attachHistoryContext(String conversationId, String currentPrompt, int retrieveSize);

    Flux<String> persistStreamingConversation(Flux<String> stream, String conversationId, String storagePrompt);

    Flux<String> persistStreamingConversation(Flux<String> stream, String conversationId, String storagePrompt, boolean syncModelMemory);

    void persistConversationExchange(String conversationId, String storagePrompt, String assistantReply, String messageType, boolean syncModelMemory);

    AgentHistoryUserPayload parseHistoryUserPayload(String content);

    List<AgentAttachment> findRecentMediaAttachments(String conversationId, int retrieveSize);
}
