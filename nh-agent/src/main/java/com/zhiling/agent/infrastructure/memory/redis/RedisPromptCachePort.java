package com.zhiling.agent.infrastructure.memory.redis;

import com.zhiling.agent.application.repository.PromptCachePort;
import com.zhiling.framework.redis.NhRedisBeanNames;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
/**
 * RedisPromptCachePort
 *
 * @author zhanghongyu
 */
public class RedisPromptCachePort implements PromptCachePort {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造器：RedisPromptCachePort
     *
     * @author zhanghongyu
     */
    public RedisPromptCachePort(@Qualifier(NhRedisBeanNames.AGENT_STRING_REDIS_TEMPLATE) StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 方法：get
     *
     * @author zhanghongyu
     */
    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 方法：set
     *
     * @author zhanghongyu
     */
    @Override
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }
}