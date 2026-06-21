package com.zhiling.system.auth.service.support.impl;

import com.zhiling.common.constant.UserConstant;
import com.zhiling.framework.redis.NhRedisBeanNames;
import com.zhiling.system.auth.service.support.TokenStorePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
/**
 * RedisTokenStore
 *
 * @author zhanghongyu
 */
public class RedisTokenStore implements TokenStorePort {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造器：RedisTokenStore
     *
     * @author zhanghongyu
     */
    public RedisTokenStore(@Qualifier(NhRedisBeanNames.AUTH_STRING_REDIS_TEMPLATE) StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 方法：saveTokens
     *
     * @author zhanghongyu
     */
    @Override
    public void saveTokens(String username, String userToken, String jwtToken, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(UserConstant.USER_TOKEN + username, userToken, ttlSeconds, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(UserConstant.JWT_TOKEN + userToken, jwtToken, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 方法：refreshTokens
     *
     * @author zhanghongyu
     */
    @Override
    public void refreshTokens(String username, String userToken, String jwtToken, long ttlSeconds) {
        saveTokens(username, userToken, jwtToken, ttlSeconds);
    }

    /**
     * 方法：getJwtToken
     *
     * @author zhanghongyu
     */
    @Override
    public String getJwtToken(String userToken) {
        return stringRedisTemplate.opsForValue().get(UserConstant.JWT_TOKEN + userToken);
    }

    /**
     * 方法：getUserToken
     *
     * @author zhanghongyu
     */
    @Override
    public String getUserToken(String username) {
        return stringRedisTemplate.opsForValue().get(UserConstant.USER_TOKEN + username);
    }

    /**
     * 方法：deleteTokens
     *
     * @author zhanghongyu
     */
    @Override
    public void deleteTokens(String username, String userToken) {
        stringRedisTemplate.delete(UserConstant.USER_TOKEN + username);
        stringRedisTemplate.delete(UserConstant.JWT_TOKEN + userToken);
    }
}