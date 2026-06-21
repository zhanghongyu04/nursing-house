package com.zhiling.gateway.config;
import com.zhiling.common.properties.RedisProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 *
 * @author zhanghongyu
 */
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class RedisConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);
    private final RedisProperties redisProperties;

    /**
     * 构造器：RedisConfig
     *
     * @author zhanghongyu
     */
    public RedisConfig(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }


    /**
     * Redis连接工厂配置
     * @return
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost()); // 替换为实际的 Redis 主机名
        config.setPort(redisProperties.getPort()); // 替换为实际的 Redis 端口号
        config.setPassword(redisProperties.getPassword()); // 替换为实际的 Redis 密码
        config.setDatabase(redisProperties.getDatabase()); // 替换为实际的 Redis 数据库索引
        return new LettuceConnectionFactory(config);
    }


    /**
     * RedisTemplate配置
     * @param
     * @return
     */
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory){
        log.info("开始创建模板对象");
        RedisTemplate redisTemplate=new RedisTemplate();
        //设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置redis key 的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }
}
