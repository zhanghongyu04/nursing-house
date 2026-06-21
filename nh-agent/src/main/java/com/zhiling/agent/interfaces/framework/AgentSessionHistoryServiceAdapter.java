package com.zhiling.agent.interfaces.framework;

import com.zhiling.common.exception.ProjectException;
import com.zhiling.framework.llm.model.AgentAttachment;
import com.zhiling.framework.llm.model.AgentHistoryMessage;
import com.zhiling.framework.llm.model.AgentHistoryUserPayload;
import com.zhiling.agent.application.AgentConversationService;
import com.zhiling.agent.application.repository.AgentMessageRepository;
import com.zhiling.framework.llm.service.AgentSessionService;
import com.zhiling.framework.llm.service.AgentSessionHistoryService;
import com.zhiling.framework.llm.model.AgentSessionVo;
import com.zhiling.agent.infrastructure.memory.redis.RedisChatMemory;
import com.zhiling.framework.agent.ConversationHistoryPort;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.entity.AgentMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 会话与历史消息适配器。
 *
 * @author zhanghongyu
 */
@Component
public class AgentSessionHistoryServiceAdapter implements AgentSessionHistoryService {

    private final ConversationHistoryPort conversationHistoryPort;
    private final RedisChatMemory chatMemory;
    private final AgentSessionService agentSessionService;
    private final AgentConversationService agentConversationService;
    private final SecurityHelper securityHelper;
    private final AgentMessageRepository agentMessageRepository;

    public AgentSessionHistoryServiceAdapter(ConversationHistoryPort conversationHistoryPort,
                                             RedisChatMemory chatMemory,
                                             AgentSessionService agentSessionService,
                                             AgentConversationService agentConversationService,
                                             SecurityHelper securityHelper,
                                             AgentMessageRepository agentMessageRepository) {
        this.conversationHistoryPort = conversationHistoryPort;
        this.chatMemory = chatMemory;
        this.agentSessionService = agentSessionService;
        this.agentConversationService = agentConversationService;
        this.securityHelper = securityHelper;
        this.agentMessageRepository = agentMessageRepository;
    }

    /**
     * 方法：saveConversationReference
     *
     * @author zhanghongyu
     */
    @Override
    public void saveConversationReference(String chatId) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return;
        }
        conversationHistoryPort.save("chat", chatId);
    }

    /**
     * 方法：saveInterceptExchange
     *
     * @author zhanghongyu
     */
    @Override
    public void saveInterceptExchange(String chatId, String userPrompt, String assistantReply) {
        if (chatId == null || chatId.trim().isEmpty()) {
            return;
        }
        Long currentUserId = securityHelper.getCurrentUserId();
        chatMemory.add(chatId, currentUserId, List.of(new UserMessage(userPrompt), new AssistantMessage(assistantReply)));
        agentMessageRepository.appendMessages(currentUserId, chatId, "intercept", List.of(
                buildMessage("user", userPrompt),
                buildMessage("assistant", assistantReply)
        ));
    }

    /**
     * 方法：getChatIds
     *
     * @author zhanghongyu
     */
    @Override
    public List<String> getChatIds(String type) {
        return agentSessionService.getUserSessions(type).stream()
                .map(AgentSessionVo::getConversationId)
                .toList();
    }

    /**
     * 方法：getChatHistory
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentHistoryMessage> getChatHistory(String type, String chatId) {
        ensureConversationAccessible(chatId, type);
        Long currentUserId = securityHelper.getCurrentUserId();
        List<Message> messages = chatMemory.get(chatId, Integer.MAX_VALUE);
        if (messages == null || messages.isEmpty()) {
            List<AgentMessage> persistedMessages = agentMessageRepository.listByConversation(currentUserId, chatId);
            if (persistedMessages.isEmpty()) {
                return List.of();
            }
            chatMemory.add(chatId, currentUserId, persistedMessages.stream()
                    .map(this::toSpringMessage)
                    .toList());
            return persistedMessages.stream()
                    .map(this::toHistoryMessage)
                    .toList();
        }
        return messages.stream().map(message -> {
            String role = message.getMessageType().name().toLowerCase();
            String content = message.getText();
            List<AgentAttachment> attachments = List.of();
            if ("user".equals(role)) {
                AgentHistoryUserPayload payload = agentConversationService.parseHistoryUserPayload(content);
                content = payload.getDisplayContent();
                attachments = payload.getAttachments();
            }
            return new AgentHistoryMessage(role, content, attachments);
        }).toList();
    }

    private AgentMessage buildMessage(String role, String content) {
        return AgentMessage.builder()
                .role(role)
                .content(content)
                .build();
    }

    private Message toSpringMessage(AgentMessage message) {
        if ("assistant".equalsIgnoreCase(message.getRole())) {
            return new AssistantMessage(message.getContent());
        }
        return new UserMessage(message.getContent());
    }

    private AgentHistoryMessage toHistoryMessage(AgentMessage message) {
        String role = message.getRole() == null ? "user" : message.getRole().toLowerCase();
        String content = message.getContent();
        List<AgentAttachment> attachments = List.of();
        if ("user".equals(role)) {
            AgentHistoryUserPayload payload = agentConversationService.parseHistoryUserPayload(content);
            content = payload.getDisplayContent();
            attachments = payload.getAttachments();
        }
        return new AgentHistoryMessage(role, content, attachments);
    }

    /**
     * 方法：ensureConversationAccessible
     *
     * @author zhanghongyu
     */
    @Override
    public void ensureConversationAccessible(String conversationId, String requestType) {
        AgentSessionVo session = agentSessionService.getSessionByConversationId(conversationId);
        if (session == null) {
            throw new ProjectException(404, "会话不存在");
        }
        if (requestType != null && !requestType.trim().isEmpty()) {
            String normalizedType = "pdf".equalsIgnoreCase(requestType) ? "pdf" : "chat";
            if (!normalizedType.equalsIgnoreCase(session.getSessionType())) {
                throw new ProjectException(403, "无权访问该类型会话记录");
            }
        }
    }
}



