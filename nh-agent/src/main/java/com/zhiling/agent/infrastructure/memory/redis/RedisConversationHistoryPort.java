package com.zhiling.agent.infrastructure.memory.redis;

import com.zhiling.framework.agent.ConversationHistoryPort;
import com.zhiling.framework.redis.NhRedisBeanNames;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Agent 会话历史索引 Redis 实现。
 *
 * @author zhanghongyu
 */
@Component
public class RedisConversationHistoryPort implements ConversationHistoryPort {

    private static final String PREFIX = "chat:history:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 构造器：RedisConversationHistoryPort
     *
     * @author zhanghongyu
     */
    public RedisConversationHistoryPort(@Qualifier(NhRedisBeanNames.AGENT_STRING_REDIS_TEMPLATE) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 方法：save
     *
     * @author zhanghongyu
     */
    @Override
    public void save(String type, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        redisTemplate.opsForSet().add(PREFIX + type, conversationId);
    }

    /**
     * 方法：getConversationIds
     *
     * @author zhanghongyu
     */
    @Override
    public List<String> getConversationIds(String type) {
        Set<String> ids = redisTemplate.opsForSet().members(PREFIX + type);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream().sorted(String::compareTo).toList();
    }

    /**
     * 方法：remove
     *
     * @author zhanghongyu
     */
    @Override
    public void remove(String type, String conversationId) {
        redisTemplate.opsForSet().remove(PREFIX + type, conversationId);
    }
}
