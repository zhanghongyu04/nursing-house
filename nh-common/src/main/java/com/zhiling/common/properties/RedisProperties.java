package com.zhiling.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.data.redis")
@Data
/**
 * RedisProperties
 *
 * @author zhanghongyu
 */
public class RedisProperties {
    private String host;
    private int port;
    private String password;
    private int database;
}
