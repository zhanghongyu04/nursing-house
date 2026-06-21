package com.zhiling.agent.infrastructure.memory.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiling.framework.agent.ConversationMemoryPort;
import com.zhiling.framework.llm.model.AgentAttachment;
import com.zhiling.framework.llm.model.AgentHistoryMessage;
import com.zhiling.framework.redis.NhRedisBeanNames;
import com.zhiling.framework.security.SecurityHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agent 会话记忆 Redis 实现。
 *
 * <p>Key 格式为 {@code chat:{userId}:{conversationId}}，通过 userId 隔离不同用户的对话记忆，
 * 与 {@link RedisChatMemory} 共享相同的 key 策略。
 *
 * @author zhanghongyu
 */
@Slf4j
@Component
public class RedisConversationMemoryPort implements ConversationMemoryPort {

    private static final String PREFIX = "chat:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityHelper securityHelper;

    public RedisConversationMemoryPort(@Qualifier(NhRedisBeanNames.AGENT_STRING_REDIS_TEMPLATE) StringRedisTemplate redisTemplate,
                                       ObjectMapper objectMapper,
                                       SecurityHelper securityHelper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.securityHelper = securityHelper;
    }

    /**
     * 构建 Redis Key，包含 userId 实现用户级隔离。
     *
     * @param conversationId 会话 ID
     * @return 格式为 chat:{userId}:{conversationId}
     * @author zhanghongyu
     */
    private String buildKey(String conversationId) {
        Long userId = securityHelper.getCurrentUserId();
        if (userId != null) {
            return PREFIX + userId + ":" + conversationId;
        }
        log.warn("无法获取当前用户ID，Redis key 不含 userId 维度: conversationId={}", conversationId);
        return PREFIX + conversationId;
    }

    /**
     * 方法：append
     *
     * @author zhanghongyu
     */
    @Override
    public void append(String conversationId, List<AgentHistoryMessage> messages) {
        if (conversationId == null || conversationId.isBlank() || messages == null || messages.isEmpty()) {
            return;
        }
        List<String> payloads = messages.stream()
                .map(this::toRedisPayload)
                .map(this::writeAsJson)
                .toList();
        redisTemplate.opsForList().leftPushAll(buildKey(conversationId), payloads);
    }

    /**
     * 方法：get
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentHistoryMessage> get(String conversationId, int lastN) {
        if (conversationId == null || conversationId.isBlank() || lastN <= 0) {
            return List.of();
        }
        List<String> payloads = redisTemplate.opsForList().range(buildKey(conversationId), 0, lastN - 1L);
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }

        List<String> orderedPayloads = new ArrayList<>(payloads);
        Collections.reverse(orderedPayloads);
        return orderedPayloads.stream()
                .map(this::readFromJson)
                .map(item -> new AgentHistoryMessage(item.role(), item.content(), item.attachments()))
                .toList();
    }

    /**
     * 方法：clear
     *
     * @author zhanghongyu
     */
    @Override
    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        redisTemplate.delete(buildKey(conversationId));
    }

    /**
     * 方法：toRedisPayload
     *
     * @author zhanghongyu
     */
    private RedisHistoryMessage toRedisPayload(AgentHistoryMessage message) {
        return new RedisHistoryMessage(message.getRole(), message.getContent(), message.getAttachments());
    }

    /**
     * 方法：writeAsJson
     *
     * @author zhanghongyu
     */
    private String writeAsJson(RedisHistoryMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("序列化会话记忆失败", ex);
        }
    }

    /**
     * 方法：readFromJson
     *
     * @author zhanghongyu
     */
    private RedisHistoryMessage readFromJson(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<RedisHistoryMessage>() {
            });
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("读取会话记忆失败", ex);
        }
    }

    /**
     * 方法：RedisHistoryMessage
     *
     * @author zhanghongyu
     */
    private record RedisHistoryMessage(String role, String content, List<AgentAttachment> attachments) {
    }
}
