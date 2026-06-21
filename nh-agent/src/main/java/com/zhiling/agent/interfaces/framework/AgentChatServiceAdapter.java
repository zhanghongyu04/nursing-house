package com.zhiling.agent.interfaces.framework;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.zhiling.framework.llm.model.AgentRagSummary;
import com.zhiling.agent.application.AgentChatService;
import com.zhiling.agent.application.AgentContentPolicyService;
import com.zhiling.agent.application.AgentConversationService;
import com.zhiling.agent.application.AgentInternalDataQueryService;
import com.zhiling.agent.application.AgentRagService;
import com.zhiling.agent.infrastructure.support.ConversationQueryClassifier;
import com.zhiling.agent.infrastructure.support.ConversationQueryType;
import com.zhiling.framework.llm.core.model.LlmChatRequest;
import com.zhiling.framework.llm.core.service.LlmService;
import com.zhiling.framework.security.SecurityHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Agent 聊天编排适配器。
 *
 * @author zhanghongyu
 */
@Component
@Slf4j
public class AgentChatServiceAdapter implements AgentChatService {

    private static final int MULTIMODAL_MAX_RETRIES = 2;
    private static final Duration MULTIMODAL_RETRY_BACKOFF = Duration.ofMillis(800);

    private final LlmService llmService;
    private final ChatClient chatSenceMultimodalClient;
    private final AgentConversationService agentConversationService;
    private final AgentRagService agentRagService;
    private final AgentInternalDataQueryService agentInternalDataQueryService;
    private final AgentContentPolicyService agentContentPolicyService;
    private final SecurityHelper securityHelper;

    public AgentChatServiceAdapter(LlmService llmService,
                                   @Qualifier("chatSenceMultimodalClient") ChatClient chatSenceMultimodalClient,
                                   AgentConversationService agentConversationService,
                                   AgentRagService agentRagService,
                                   AgentInternalDataQueryService agentInternalDataQueryService,
                                   AgentContentPolicyService agentContentPolicyService,
                                   SecurityHelper securityHelper) {
        this.llmService = llmService;
        this.chatSenceMultimodalClient = chatSenceMultimodalClient;
        this.agentConversationService = agentConversationService;
        this.agentRagService = agentRagService;
        this.agentInternalDataQueryService = agentInternalDataQueryService;
        this.agentContentPolicyService = agentContentPolicyService;
        this.securityHelper = securityHelper;
    }

    /**
     * 方法：textChat
     *
     * @author zhanghongyu
     */
    @Override
    public Flux<String> textChat(String prompt, String chatId, HttpServletResponse response) {
        String finalPrompt = (prompt == null || prompt.trim().isEmpty()) ? "你好" : prompt;
        Long currentUserId = securityHelper.getCurrentUserId();
        ConversationQueryType queryType = ConversationQueryClassifier.classify(finalPrompt);
        log.info("文本对话类型判定: chatId={}, queryType={}, promptPreview={}",
                chatId, queryType, previewPrompt(finalPrompt));

        Optional<String> directAnswer = agentInternalDataQueryService.tryHandle(finalPrompt);
        if (directAnswer.isPresent()) {
            agentRagService.writeRagHeaders(response, agentRagService.emptySummary());
            String safeAnswer = agentContentPolicyService.applyOutputPolicy(finalPrompt, directAnswer.get());
            agentConversationService.persistConversationExchange(chatId, finalPrompt, safeAnswer, "direct", true);
            return Flux.just(safeAnswer);
        }

        AgentRagSummary retrievalSummary = queryType == ConversationQueryType.KNOWLEDGE
                ? agentRagService.inspectKnowledgeRetrieval(finalPrompt)
                : agentRagService.emptySummary();
        agentRagService.writeRagHeaders(response, retrievalSummary);

        LlmChatRequest request = new LlmChatRequest();
        request.setChatId(chatId);
        request.setUserId(currentUserId);
        // 文本链路统一由 LlmService 内部记忆拼装上下文，避免重复叠加历史对 RAG 查询造成干扰。
        request.setPrompt(finalPrompt);
        Flux<String> stream = llmService.stream(request);
        return agentConversationService.persistStreamingConversation(stream, chatId, finalPrompt);
    }

    @Override
    public Flux<String> multiModalChat(String modelPrompt,
                                       String storagePrompt,
                                       String chatId,
                                       List<MultipartFile> mediaFiles,
                                       HttpServletResponse response) {
        log.info("开始执行多模态对话: chatId={}, mediaFileCount={}, fileNames={}, modelPromptPreview={}, storagePromptPreview={}",
                chatId,
                mediaFiles == null ? 0 : mediaFiles.size(),
                mediaFiles == null ? List.of() : mediaFiles.stream().map(MultipartFile::getOriginalFilename).toList(),
                previewPrompt(modelPrompt),
                previewPrompt(storagePrompt));
        List<Media> medias = mediaFiles.stream()
                .map(file -> new Media(
                        MimeType.valueOf(Objects.requireNonNull(file.getContentType())),
                        file.getResource()
                ))
                .toList();

        if (medias.isEmpty()) {
            log.warn("多模态文件列表为空，回退文本链路: chatId={}", chatId);
            return textChat(modelPrompt, chatId, response);
        }

        String promptWithHistory = agentConversationService.attachHistoryContext(chatId, modelPrompt, 16);
        UserMessage userMessage = UserMessage.builder()
                .text(promptWithHistory)
                .media(medias)
                .metadata(buildMultimodalMetadata(mediaFiles))
                .build();
        Prompt promptRequest = new Prompt(List.of(userMessage), DashScopeChatOptions.builder()
                .multiModel(true)
                .build());

        Flux<String> stream = chatSenceMultimodalClient.prompt(promptRequest)
                .stream()
                .content()
                .retryWhen(Retry.backoff(MULTIMODAL_MAX_RETRIES, MULTIMODAL_RETRY_BACKOFF)
                        .filter(this::isRetryableNetworkError)
                        .doBeforeRetry(signal ->
                                log.warn("多模态请求失败，准备重试: attempt={} reason={}",
                                        signal.totalRetries() + 1,
                                        signal.failure() == null ? "unknown" : signal.failure().getMessage())))
                .onErrorResume(ex -> {
                    log.error("多模态请求最终失败，chatId={}", chatId, ex);
                    return Flux.just("多模态服务当前网络不稳定，请稍后重试，或先使用文字描述图片内容。");
                });

        return agentConversationService.persistStreamingConversation(stream, chatId, storagePrompt, true);
    }

    /**
     * 方法：isRetryableNetworkError
     *
     * @author zhanghongyu
     */
    private boolean isRetryableNetworkError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof WebClientRequestException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("connection reset by peer")
                        || lower.contains("connection reset")
                        || lower.contains("read timed out")
                        || lower.contains("premature")
                        || lower.contains("broken pipe")
                        || lower.contains("timeout")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 方法：buildMultimodalMetadata
     *
     * @author zhanghongyu
     */
    private Map<String, Object> buildMultimodalMetadata(List<MultipartFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return Map.of();
        }
        MessageFormat messageFormat = resolveMessageFormat(mediaFiles);
        if (messageFormat == null || MessageFormat.IMAGE.equals(messageFormat)) {
            return Map.of();
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(DashScopeApiConstants.MESSAGE_FORMAT, messageFormat);
        return metadata;
    }

    /**
     * 方法：resolveMessageFormat
     *
     * @author zhanghongyu
     */
    private MessageFormat resolveMessageFormat(List<MultipartFile> mediaFiles) {
        MessageFormat resolved = null;
        for (MultipartFile mediaFile : mediaFiles) {
            if (mediaFile == null) {
                continue;
            }
            String contentType = mediaFile.getContentType();
            if (contentType == null || contentType.isBlank()) {
                continue;
            }
            MessageFormat current = null;
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("video/")) {
                current = MessageFormat.VIDEO;
            } else if (normalized.startsWith("audio/")) {
                current = MessageFormat.AUDIO;
            } else if (normalized.startsWith("image/")) {
                current = MessageFormat.IMAGE;
            }
            if (current == null) {
                continue;
            }
            if (resolved == null) {
                resolved = current;
                continue;
            }
            if (!resolved.equals(current)) {
                log.warn("检测到混合媒体类型，多模态 metadata 不设置 MESSAGE_FORMAT: contentTypes={}",
                        mediaFiles.stream().filter(Objects::nonNull).map(MultipartFile::getContentType).toList());
                return null;
            }
        }
        return resolved;
    }

    /**
     * 方法：previewPrompt
     *
     * @author zhanghongyu
     */
    private String previewPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        String normalized = prompt.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80) + "...";
    }

}
