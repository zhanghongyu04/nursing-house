package com.zhiling.agent.infrastructure.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiling.agent.application.port.AgentContextMemoryRecoveryPort;
import com.zhiling.framework.llm.core.model.MemoryMessage;
import com.zhiling.framework.llm.core.service.MemoryPort;
import com.zhiling.framework.security.SecurityHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis 列表的记忆实现。
 *
 * <p>Key 格式为 {@code {prefix}{userId}:{chatId}}，通过 userId 隔离不同用户的 LLM 上下文记忆。
 *
 * @author zhanghongyu
 */
@Slf4j
public class RedisMemoryService implements MemoryPort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String keyPrefix;
    private final SecurityHelper securityHelper;
    private final AgentContextMemoryRecoveryPort recoveryPort;

    /**
     * 构造器：RedisMemoryService
     *
     * @author zhanghongyu
     */
    public RedisMemoryService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                              String keyPrefix, SecurityHelper securityHelper,
                              AgentContextMemoryRecoveryPort recoveryPort) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyPrefix = keyPrefix;
        this.securityHelper = securityHelper;
        this.recoveryPort = recoveryPort;
    }

    /**
     * 方法：load
     *
     * @author zhanghongyu
     */
    @Override
    public List<MemoryMessage> load(String chatId, int limit) {
        return load(chatId, limit, resolveCurrentUserId());
    }

    @Override
    public List<MemoryMessage> load(String chatId, int limit, Long userId) {
        if (chatId == null || chatId.isBlank() || limit <= 0) {
            return List.of();
        }
        List<String> rawList = redisTemplate.opsForList().range(key(chatId, userId), 0, limit - 1L);
        if (rawList == null || rawList.isEmpty()) {
            return recoverFromMysql(chatId, limit, userId);
        }
        List<String> ordered = new ArrayList<>(rawList);
        Collections.reverse(ordered);
        return ordered.stream().map(this::deserialize).toList();
    }

    /**
     * 方法：append
     *
     * @author zhanghongyu
     */
    @Override
    public void append(String chatId, MemoryMessage message) {
        append(chatId, message, resolveCurrentUserId());
    }

    @Override
    public void append(String chatId, MemoryMessage message, Long userId) {
        if (chatId == null || chatId.isBlank() || message == null) {
            return;
        }
        redisTemplate.opsForList().leftPush(key(chatId, userId), serialize(message));
    }

    /**
     * 方法：key
     *
     * @author zhanghongyu
     */
    /**
     * 构建 Redis Key，包含 userId 实现用户级隔离。
     *
     * @param chatId 会话 ID
     * @return 格式为 {prefix}{userId}:{chatId}
     * @author zhanghongyu
     */
    private String key(String chatId, Long userId) {
        if (userId != null) {
            return keyPrefix + userId + ":" + chatId;
        }
        log.warn("无法获取当前用户ID，Redis key 不含 userId 维度: chatId={}", chatId);
        return keyPrefix + chatId;
    }

    /**
     * 方法：serialize
     *
     * @author zhanghongyu
     */
    private String serialize(MemoryMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("serialize memory message failed", ex);
        }
    }

    /**
     * 方法：deserialize
     *
     * @author zhanghongyu
     */
    private MemoryMessage deserialize(String raw) {
        try {
            return objectMapper.readValue(raw, MemoryMessage.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("deserialize memory message failed", ex);
        }
    }

    /**
     * 方法：resolveCurrentUserId
     *
     * @author zhanghongyu
     */
    private Long resolveCurrentUserId() {
        return securityHelper.getCurrentUserId();
    }

    private List<MemoryMessage> recoverFromMysql(String chatId, int limit, Long userId) {
        if (userId == null || recoveryPort == null) {
            return List.of();
        }
        List<MemoryMessage> messages = recoveryPort.recover(userId, chatId, limit);
        if (!messages.isEmpty()) {
            String redisKey = key(chatId, userId);
            List<String> payloads = messages.stream()
                    .map(this::serialize)
                    .toList();
            redisTemplate.delete(redisKey);
            redisTemplate.opsForList().leftPushAll(redisKey, payloads);
            log.info("模型上下文 Redis 为空，已从 MySQL 恢复并回填: userId={}, chatId={}, size={}",
                    userId, chatId, messages.size());
        }
        return messages;
    }
}
