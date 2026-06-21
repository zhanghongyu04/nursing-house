package com.zhiling.agent.infrastructure.memory.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiling.agent.infrastructure.memory.model.Msg;
import com.zhiling.framework.redis.NhRedisBeanNames;
import com.zhiling.framework.security.SecurityHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Redis ChatMemory 实现类。
 *
 * <p>Key 格式为 {@code chat:{userId}:{conversationId}}，通过 userId 隔离不同用户的对话记忆，
 * 防止 conversationId 泄露后跨用户访问对话历史。
 *
 * @author zhanghongyu
 */
@Slf4j
@Component
public class RedisChatMemory implements ChatMemory {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SecurityHelper securityHelper;
    private static final String PREFIX = "chat:";
    private static final int DEFAULT_RETRIEVE_SIZE = 20;

    /**
     * 构造器：RedisChatMemory
     *
     * @author zhanghongyu
     */
    public RedisChatMemory(@Qualifier(NhRedisBeanNames.AGENT_STRING_REDIS_TEMPLATE) StringRedisTemplate redisTemplate,
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
        return buildKey(conversationId, securityHelper.getCurrentUserId());
    }

    /**
     * 构建 Redis Key，优先使用显式传入的 userId 实现用户级隔离。
     *
     * @param conversationId 会话 ID
     * @param userId 显式传入的用户 ID
     * @return 格式为 chat:{userId}:{conversationId}
     * @author zhanghongyu
     */
    private String buildKey(String conversationId, Long userId) {
        if (userId != null) {
            return PREFIX + userId + ":" + conversationId;
        }
        log.warn("无法获取当前用户ID，Redis key 不含 userId 维度: conversationId={}", conversationId);
        return PREFIX + conversationId;
    }

    /**
     * 方法：add
     *
     * @author zhanghongyu
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        add(conversationId, null, messages);
    }

    /**
     * 方法：add
     *
     * @author zhanghongyu
     */
    public void add(String conversationId, Long userId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<String> list = messages.stream().map(Msg::new).map(msg -> {
            try {
                return objectMapper.writeValueAsString(msg);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
        redisTemplate.opsForList().leftPushAll(buildKey(conversationId, userId), list);
    }

    /**
     * 方法：get
     *
     * @author zhanghongyu
     */
    @Override
    public List<Message> get(String conversationId) {
        return get(conversationId, DEFAULT_RETRIEVE_SIZE);
    }

    /**
     * 方法：get
     *
     * @author zhanghongyu
     */
    public List<Message> get(String conversationId, int lastN) {
        if (lastN <= 0) {
            return List.of();
        }
        List<String> list = redisTemplate.opsForList().range(buildKey(conversationId), 0, lastN - 1L);
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        // Redis 中当前采用 leftPush 追加新消息，读取时需反转为"旧 -> 新"顺序再返回前端
        List<String> orderedList = new ArrayList<>(list);
        Collections.reverse(orderedList);

        return orderedList.stream().map(s -> {
            try {
                return objectMapper.readValue(s, Msg.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).map(Msg::toMessage).toList();
    }

    /**
     * 方法：clear
     *
     * @author zhanghongyu
     */
    @Override
    public void clear(String conversationId) {
        redisTemplate.delete(buildKey(conversationId));
    }
}
