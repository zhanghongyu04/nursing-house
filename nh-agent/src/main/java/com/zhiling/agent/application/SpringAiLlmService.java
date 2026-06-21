package com.zhiling.agent.application;

import com.zhiling.framework.llm.core.service.LlmService;
import com.zhiling.framework.llm.core.model.LlmCapabilities;
import com.zhiling.framework.llm.core.model.LlmChatRequest;
import com.zhiling.framework.llm.core.model.LlmChatResponse;
import com.zhiling.framework.llm.core.model.MemoryMessage;
import com.zhiling.framework.llm.core.model.RagResult;
import com.zhiling.framework.llm.core.model.ToolResult;
import com.zhiling.framework.llm.core.service.GuardPort;
import com.zhiling.framework.llm.core.service.MemoryPort;
import com.zhiling.framework.llm.core.service.PromptProvider;
import com.zhiling.framework.llm.core.service.RagPort;
import com.zhiling.framework.llm.core.service.ToolPort;
import com.zhiling.framework.security.CurrentUserContextHolder;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于 Spring AI ChatClient 的统一门面实现。
 *
 * @author zhanghongyu
 */
public class SpringAiLlmService implements LlmService {

    private static final String MEMORY_CONTEXT_PREFIX = "以下是历史对话上下文，请结合上下文回答。";
    private static final String META_SUMMARY_INSTRUCTION = "如果当前用户问题是在要求总结、回顾或梳理本次对话，请只总结下方历史对话中用户和助手实际讨论过的内容，不要引入新的知识库主题或重新回答某个护理知识点。";
    private static final String LEGACY_HISTORY_CONTEXT_PREFIX = "以下是历史对话，请保持上下文连续性并基于历史信息回答。";
    private static final String MEMORY_QUESTION_MARKER = "[当前用户问题]";
    private static final String LEGACY_QUESTION_MARKER = "当前用户问题：";

    private final ChatClient chatClient;
    private final PromptProvider promptProvider;
    private final GuardPort guardPort;
    private final MemoryPort memoryPort;
    private final RagPort ragPort;
    private final ToolPort toolPort;
    private final Map<String, Boolean> flags;
    private final int contextLimit;

    public SpringAiLlmService(ChatClient chatClient,
                             PromptProvider promptProvider,
                             GuardPort guardPort,
                             MemoryPort memoryPort,
                             RagPort ragPort,
                             ToolPort toolPort,
                             Map<String, Boolean> capabilityFlags,
                             int contextLimit) {
        this.chatClient = chatClient;
        this.promptProvider = promptProvider;
        this.guardPort = guardPort;
        this.memoryPort = memoryPort;
        this.ragPort = ragPort;
        this.toolPort = toolPort;
        this.flags = normalizeFlags(capabilityFlags);
        this.contextLimit = Math.max(contextLimit, 0);
    }

    /**
     * 方法：chat
     *
     * @author zhanghongyu
     */
    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        String prompt = request == null ? "" : String.valueOf(request.getPrompt() == null ? "" : request.getPrompt());
        String chatId = request == null ? null : request.getChatId();
        Long userId = resolveUserId(request);
        Map<String, Object> metadata = request == null || request.getMetadata() == null ? Map.of() : request.getMetadata();
        // 统一执行输入拦截、记忆拼装、工具增强、RAG 增强，再进入模型调用。
        var intercepted = guardPort.interceptInput(prompt);
        if (intercepted.isPresent()) {
            return build(traceId, intercepted.get(), null, List.of());
        }
        String userPrompt = composePromptWithMemory(chatId, prompt, userId);
        ToolResult toolResult = toolPort.beforeChat(userPrompt, metadata);
        String toolPrompt = toolResult == null || toolResult.getPrompt() == null ? userPrompt : toolResult.getPrompt();
        RagResult ragResult = ragPort.augment(toolPrompt, metadata);
        String finalPrompt = ragResult == null || ragResult.getPrompt() == null ? toolPrompt : ragResult.getPrompt();
        List<String> sources = ragResult == null || ragResult.getSources() == null ? List.of() : ragResult.getSources();
        String raw = chatClient.prompt()
                .system(promptProvider.systemPrompt())
                .user(finalPrompt)
                .call()
                .content();
        String sanitized = guardPort.sanitizeOutput(prompt, raw);
        appendMemory(chatId, "user", prompt, userId);
        appendMemory(chatId, "assistant", sanitized, userId);
        return build(traceId, sanitized, null, sources);
    }

    /**
     * 方法：stream
     *
     * @author zhanghongyu
     */
    @Override
    public Flux<String> stream(LlmChatRequest request) {
        String prompt = request == null ? "" : String.valueOf(request.getPrompt() == null ? "" : request.getPrompt());
        String chatId = request == null ? null : request.getChatId();
        Long userId = resolveUserId(request);
        Map<String, Object> metadata = request == null || request.getMetadata() == null ? Map.of() : request.getMetadata();
        var intercepted = guardPort.interceptInput(prompt);
        if (intercepted.isPresent()) {
            return Flux.just(intercepted.get());
        }
        String userPrompt = composePromptWithMemory(chatId, prompt, userId);
        ToolResult toolResult = toolPort.beforeChat(userPrompt, metadata);
        String toolPrompt = toolResult == null || toolResult.getPrompt() == null ? userPrompt : toolResult.getPrompt();
        RagResult ragResult = ragPort.augment(toolPrompt, metadata);
        String finalPrompt = ragResult == null || ragResult.getPrompt() == null ? toolPrompt : ragResult.getPrompt();
        StringBuilder buffer = new StringBuilder();
        return chatClient.prompt()
                .system(promptProvider.systemPrompt())
                .user(finalPrompt)
                .stream()
                .content()
                .map(chunk -> {
                    // 流式阶段逐片做输出净化，并累计完整回答用于结束后写入记忆。
                    String sanitized = guardPort.sanitizeOutput(prompt, chunk);
                    buffer.append(sanitized);
                    return sanitized;
                })
                .doOnComplete(() -> {
                    appendMemory(chatId, "user", prompt, userId);
                    appendMemory(chatId, "assistant", buffer.toString(), userId);
                });
    }

    /**
     * 方法：capabilities
     *
     * @author zhanghongyu
     */
    @Override
    public LlmCapabilities capabilities() {
        return LlmCapabilities.builder()
                .enabled(true)
                .capabilityFlags(flags)
                .build();
    }

    /**
     * 方法：build
     *
     * @author zhanghongyu
     */
    private LlmChatResponse build(String traceId, String content, String degradeReason, List<String> sources) {
        return LlmChatResponse.builder()
                .traceId(traceId)
                .content(content)
                .capabilityFlags(flags)
                .degradeReason(degradeReason)
                .sources(sources == null ? List.of() : sources)
                .build();
    }

    /**
     * 方法：defaultFlags
     *
     * @author zhanghongyu
     */
    public static Map<String, Boolean> defaultFlags() {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("llm.chat", true);
        flags.put("llm.prompt", false);
        flags.put("llm.guard", false);
        flags.put("llm.rag", false);
        flags.put("llm.tool", false);
        flags.put("llm.memory", false);
        return flags;
    }

    /**
     * 方法：normalizeFlags
     *
     * @author zhanghongyu
     */
    private static Map<String, Boolean> normalizeFlags(Map<String, Boolean> snapshot) {
        Map<String, Boolean> defaults = defaultFlags();
        if (snapshot == null || snapshot.isEmpty()) {
            return defaults;
        }
        defaults.putAll(snapshot);
        return defaults;
    }

    /**
     * 方法：composePromptWithMemory
     *
     * @author zhanghongyu
     */
    private String composePromptWithMemory(String chatId, String prompt, Long userId) {
        if (chatId == null || chatId.isBlank() || contextLimit <= 0) {
            return prompt;
        }
        if (looksLikePromptWithHistory(prompt)) {
            // 已经带历史上下文的 prompt 不再二次包装，避免上下文重复叠加。
            return prompt;
        }
        List<MemoryMessage> history = memoryPort.load(chatId, contextLimit, userId);
        if (history.isEmpty()) {
            return prompt;
        }
        String context = history.stream()
                .filter(message -> message != null && message.getContent() != null)
                .map(message -> "[" + safeRole(message.getRole()) + "] " + message.getContent())
                .collect(Collectors.joining("\n"));
        if (context.isBlank()) {
            return prompt;
        }
        // 统一使用显式分隔符包装历史，降低模型把历史摘要误当当前问题的概率。
        return "以下是历史对话上下文，请结合上下文回答。\n"
                + META_SUMMARY_INSTRUCTION
                + "\n"
                + context
                + "\n[当前用户问题] "
                + prompt;
    }

    /**
     * 方法：looksLikePromptWithHistory
     *
     * @author zhanghongyu
     */
    private boolean looksLikePromptWithHistory(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }
        return prompt.startsWith(MEMORY_CONTEXT_PREFIX)
                || prompt.startsWith(LEGACY_HISTORY_CONTEXT_PREFIX)
                || prompt.contains(MEMORY_QUESTION_MARKER)
                || prompt.contains(LEGACY_QUESTION_MARKER);
    }

    /**
     * 方法：safeRole
     *
     * @author zhanghongyu
     */
    private String safeRole(String role) {
        if (role == null || role.isBlank()) {
            return "unknown";
        }
        return role;
    }

    /**
     * 方法：appendMemory
     *
     * @author zhanghongyu
     */
    private void appendMemory(String chatId, String role, String content, Long userId) {
        if (chatId == null || chatId.isBlank() || content == null || content.isBlank()) {
            return;
        }
        memoryPort.append(chatId, MemoryMessage.builder().role(role).content(content).build(), userId);
    }

    /**
     * 方法：resolveUserId
     *
     * @author zhanghongyu
     */
    private Long resolveUserId(LlmChatRequest request) {
        if (request != null && request.getUserId() != null) {
            return request.getUserId();
        }
        if (request != null && request.getMetadata() != null) {
            Long metadataUserId = castToLong(request.getMetadata().get("userId"));
            if (metadataUserId != null) {
                return metadataUserId;
            }
        }
        return CurrentUserContextHolder.get()
                .map(currentUserContext -> currentUserContext.getUserId())
                .orElse(null);
    }

    /**
     * 方法：castToLong
     *
     * @author zhanghongyu
     */
    private Long castToLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Long.valueOf(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

