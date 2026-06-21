package com.zhiling.agent.interfaces.framework;

import com.zhiling.framework.llm.model.AgentAttachment;
import com.zhiling.framework.llm.model.AgentHistoryUserPayload;
import com.zhiling.agent.application.AgentContentPolicyService;
import com.zhiling.agent.application.AgentConversationService;
import com.zhiling.agent.application.repository.AgentMessageRepository;
import com.zhiling.framework.llm.core.model.MemoryMessage;
import com.zhiling.framework.llm.core.service.MemoryPort;
import com.zhiling.agent.infrastructure.memory.redis.RedisChatMemory;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.entity.AgentMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent 对话会话编排适配器。
 *
 * @author zhanghongyu
 */
@Component
@Slf4j
public class AgentConversationServiceAdapter implements AgentConversationService {

    private static final String DOCUMENT_CONTEXT_START = "=== 上传的文档内容 ===";
    private static final String MEDIA_CONTEXT_START = "=== 上传的媒体文件 ===";
    private static final String USER_QUESTION_MARKER = "用户问题：";
    private static final String META_SUMMARY_INSTRUCTION = "如果当前用户问题是在要求总结、回顾或梳理本次对话，请只总结下方历史对话中用户和助手实际讨论过的内容，不要引入新的知识库主题或重新回答某个护理知识点。";
    private static final Pattern DOCUMENT_FILE_PATTERN = Pattern.compile("【文件：([^】\\n]+)】");
    private static final Pattern ATTACHMENT_META_PATTERN = Pattern.compile("【附件：([^|\\n]+)\\|([^|\\n]+)\\|([^】\\n]+)】");

    private final RedisChatMemory chatMemory;
    private final AgentContentPolicyService agentContentPolicyService;
    private final SecurityHelper securityHelper;
    private final AgentMessageRepository agentMessageRepository;
    private final MemoryPort memoryPort;

    public AgentConversationServiceAdapter(RedisChatMemory chatMemory,
                                           AgentContentPolicyService agentContentPolicyService,
                                           SecurityHelper securityHelper,
                                           AgentMessageRepository agentMessageRepository,
                                           MemoryPort memoryPort) {
        this.chatMemory = chatMemory;
        this.agentContentPolicyService = agentContentPolicyService;
        this.securityHelper = securityHelper;
        this.agentMessageRepository = agentMessageRepository;
        this.memoryPort = memoryPort;
    }

    /**
     * 方法：attachHistoryContext
     *
     * @author zhanghongyu
     */
    @Override
    public String attachHistoryContext(String conversationId, String currentPrompt, int retrieveSize) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return currentPrompt;
        }
        List<Message> history = chatMemory.get(conversationId, retrieveSize);
        if (history == null || history.isEmpty()) {
            return currentPrompt;
        }

        StringBuilder historyText = new StringBuilder();
        for (Message message : history) {
            if (message == null || message.getText() == null || message.getText().trim().isEmpty()) {
                continue;
            }
            if (message.getMessageType() == org.springframework.ai.chat.messages.MessageType.USER) {
                String messageText = parseHistoryUserPayload(message.getText()).getDisplayContent();
                historyText.append("用户：").append(messageText).append("\n");
            } else if (message.getMessageType() == org.springframework.ai.chat.messages.MessageType.ASSISTANT) {
                historyText.append("助手：").append(message.getText()).append("\n");
            }
        }
        if (historyText.length() == 0) {
            return currentPrompt;
        }

        return "以下是历史对话，请保持上下文连续性并基于历史信息回答。\n"
                + META_SUMMARY_INSTRUCTION
                + "\n"
                + historyText
                + "\n当前用户问题：\n"
                + currentPrompt;
    }

    /**
     * 方法：persistStreamingConversation
     *
     * @author zhanghongyu
     */
    @Override
    public Flux<String> persistStreamingConversation(Flux<String> stream, String conversationId, String storagePrompt) {
        return persistStreamingConversation(stream, conversationId, storagePrompt, false);
    }

    @Override
    public Flux<String> persistStreamingConversation(Flux<String> stream, String conversationId, String storagePrompt, boolean syncModelMemory) {
        String safeStoragePrompt = storagePrompt == null ? "" : storagePrompt;
        Long currentUserId = securityHelper.getCurrentUserId();
        return stream
                .collectList()
                .flatMapMany(parts -> {
                    String rawReply = String.join("", parts);
                    String safeReply = agentContentPolicyService.applyOutputPolicy(safeStoragePrompt, rawReply);
                    persistConversationExchange(conversationId, safeStoragePrompt, safeReply, "chat", syncModelMemory, currentUserId);
                    return Flux.just(safeReply);
                })
                .doOnError(e -> log.error("保存流式会话记忆失败，conversationId={}", conversationId, e));
    }

    @Override
    public void persistConversationExchange(String conversationId, String storagePrompt, String assistantReply,
                                            String messageType, boolean syncModelMemory) {
        Long currentUserId = securityHelper.getCurrentUserId();
        persistConversationExchange(conversationId, storagePrompt, assistantReply, messageType, syncModelMemory, currentUserId);
    }

    private void persistConversationExchange(String conversationId, String storagePrompt, String assistantReply,
                                             String messageType, boolean syncModelMemory, Long currentUserId) {
        if (conversationId == null || conversationId.trim().isEmpty()
                || assistantReply == null || assistantReply.isBlank()) {
            return;
        }
        String safeStoragePrompt = storagePrompt == null ? "" : storagePrompt;
        String safeMessageType = messageType == null || messageType.isBlank() ? "chat" : messageType;
        chatMemory.add(conversationId, currentUserId, List.of(
                new UserMessage(safeStoragePrompt),
                new AssistantMessage(assistantReply)
        ));
        agentMessageRepository.appendMessages(currentUserId, conversationId, safeMessageType, List.of(
                buildMessage("user", safeStoragePrompt, safeMessageType),
                buildMessage("assistant", assistantReply, safeMessageType)
        ));
        if (syncModelMemory) {
            appendModelMemory(conversationId, "user", safeStoragePrompt, currentUserId);
            appendModelMemory(conversationId, "assistant", assistantReply, currentUserId);
        }
    }

    private void appendModelMemory(String conversationId, String role, String content, Long currentUserId) {
        if (content == null || content.isBlank()) {
            return;
        }
        memoryPort.append(conversationId, MemoryMessage.builder()
                .role(role)
                .content(content)
                .build(), currentUserId);
    }

    private AgentMessage buildMessage(String role, String content, String messageType) {
        return AgentMessage.builder()
                .role(role)
                .content(content)
                .messageType(messageType)
                .build();
    }

    /**
     * 方法：parseHistoryUserPayload
     *
     * @author zhanghongyu
     */
    @Override
    public AgentHistoryUserPayload parseHistoryUserPayload(String content) {
        if (content == null || content.isBlank()) {
            return new AgentHistoryUserPayload(content, List.of());
        }

        LinkedHashMap<String, AgentAttachment> attachmentMap = new LinkedHashMap<>();
        Matcher metaMatcher = ATTACHMENT_META_PATTERN.matcher(content);
        while (metaMatcher.find()) {
            String fileName = metaMatcher.group(1);
            String fileType = metaMatcher.group(2);
            String fileUrl = metaMatcher.group(3);
            if (fileName == null || fileName.isBlank()) {
                continue;
            }
            String normalized = fileName.trim();
            String safeType = (fileType == null || fileType.isBlank())
                    ? getFileExtension(normalized).toUpperCase(Locale.ROOT)
                    : fileType.trim().toUpperCase(Locale.ROOT);
            String safeUrl = fileUrl == null ? null : fileUrl.trim();
            attachmentMap.put(normalized, new AgentAttachment(normalized, safeType, safeUrl));
        }

        Matcher matcher = DOCUMENT_FILE_PATTERN.matcher(content);
        while (matcher.find()) {
            String fileName = matcher.group(1);
            if (fileName != null && !fileName.isBlank()) {
                String normalized = fileName.trim();
                attachmentMap.putIfAbsent(
                        normalized,
                        new AgentAttachment(normalized, getFileExtension(normalized).toUpperCase(Locale.ROOT), null)
                );
            }
        }

        String question = "";
        int questionIndex = content.indexOf(USER_QUESTION_MARKER);
        if (questionIndex >= 0) {
            question = content.substring(questionIndex + USER_QUESTION_MARKER.length()).trim();
        }

        String displayContent = question;
        if (displayContent == null || displayContent.isBlank()) {
            if (content.contains(DOCUMENT_CONTEXT_START)) {
                displayContent = "请帮我分析上传的文档内容";
            } else if (content.contains(MEDIA_CONTEXT_START)) {
                displayContent = "请帮我分析上传的媒体内容";
            } else {
                displayContent = content;
            }
        }
        return new AgentHistoryUserPayload(displayContent, new ArrayList<>(attachmentMap.values()));
    }

    /**
     * 方法：findRecentMediaAttachments
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentAttachment> findRecentMediaAttachments(String conversationId, int retrieveSize) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return List.of();
        }
        List<Message> history = chatMemory.get(conversationId, retrieveSize);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            Message message = history.get(i);
            if (!(message instanceof UserMessage) || message.getText() == null || message.getText().isBlank()) {
                continue;
            }
            List<AgentAttachment> attachments = parseHistoryUserPayload(message.getText()).getAttachments();
            if (attachments == null || attachments.isEmpty()) {
                continue;
            }
            List<AgentAttachment> mediaAttachments = attachments.stream()
                    .filter(this::isMediaAttachment)
                    .toList();
            if (!mediaAttachments.isEmpty()) {
                return mediaAttachments;
            }
        }
        return List.of();
    }

    /**
     * 方法：isMediaAttachment
     *
     * @author zhanghongyu
     */
    private boolean isMediaAttachment(AgentAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        String fileType = attachment.getFileType();
        if (fileType == null || fileType.isBlank()) {
            return false;
        }
        String normalized = fileType.trim().toLowerCase(Locale.ROOT);
        return List.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "mp3", "wav", "ogg", "mp4", "webm", "mov")
                .contains(normalized);
    }

    /**
     * 方法：getFileExtension
     *
     * @author zhanghongyu
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}

