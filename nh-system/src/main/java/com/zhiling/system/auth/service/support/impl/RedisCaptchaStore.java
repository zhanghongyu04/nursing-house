package com.zhiling.system.auth.service.support.impl;

import com.zhiling.framework.redis.NhRedisBeanNames;
import com.zhiling.system.auth.service.support.CaptchaStorePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
/**
 * RedisCaptchaStore
 *
 * @author zhanghongyu
 */
public class RedisCaptchaStore implements CaptchaStorePort {

    private static final String CAPTCHA_PREFIX = "captcha:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造器：RedisCaptchaStore
     *
     * @author zhanghongyu
     */
    public RedisCaptchaStore(@Qualifier(NhRedisBeanNames.AUTH_STRING_REDIS_TEMPLATE) StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 方法：save
     *
     * @author zhanghongyu
     */
    @Override
    public void save(String captchaKey, String captchaText, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(CAPTCHA_PREFIX + captchaKey, captchaText, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 方法：get
     *
     * @author zhanghongyu
     */
    @Override
    public String get(String captchaKey) {
        return stringRedisTemplate.opsForValue().get(CAPTCHA_PREFIX + captchaKey);
    }

    /**
     * 方法：remove
     *
     * @author zhanghongyu
     */
    @Override
    public void remove(String captchaKey) {
        stringRedisTemplate.delete(CAPTCHA_PREFIX + captchaKey);
    }
}