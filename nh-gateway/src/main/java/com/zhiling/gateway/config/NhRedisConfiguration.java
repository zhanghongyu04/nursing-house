package com.zhiling.gateway.config;

import com.zhiling.framework.redis.NhRedisBeanNames;
import com.zhiling.framework.redis.NhRedisProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 认证与会话 Redis 分库隔离。
 */
@Configuration
@AutoConfigureBefore(RedisAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties({RedisProperties.class, NhRedisProperties.class})
public class NhRedisConfiguration {

    private static final String AUTH_REDIS_CONNECTION_FACTORY = "authRedisConnectionFactory";
    private static final String AGENT_REDIS_CONNECTION_FACTORY = "agentRedisConnectionFactory";

    @Bean
    @Primary
    public RedisConnectionFactory authRedisConnectionFactory(RedisProperties redisProperties,
                                                             NhRedisProperties nhRedisProperties) {
        return buildConnectionFactory(redisProperties, nhRedisProperties.getAuthDatabase());
    }

    @Bean
    public RedisConnectionFactory agentRedisConnectionFactory(RedisProperties redisProperties,
                                                              NhRedisProperties nhRedisProperties) {
        return buildConnectionFactory(redisProperties, nhRedisProperties.getAgentDatabase());
    }

    @Bean
    @Primary
    @Qualifier(NhRedisBeanNames.AUTH_STRING_REDIS_TEMPLATE)
    public StringRedisTemplate authStringRedisTemplate(
            @Qualifier(AUTH_REDIS_CONNECTION_FACTORY) RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    @Qualifier(NhRedisBeanNames.AGENT_STRING_REDIS_TEMPLATE)
    public StringRedisTemplate agentStringRedisTemplate(
            @Qualifier(AGENT_REDIS_CONNECTION_FACTORY) RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    private RedisConnectionFactory buildConnectionFactory(RedisProperties redisProperties, int database) {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisProperties.getHost());
        configuration.setPort(redisProperties.getPort());
        configuration.setDatabase(database);
        if (redisProperties.getPassword() != null) {
            configuration.setPassword(redisProperties.getPassword());
        }
        return new LettuceConnectionFactory(configuration);
    }
}
