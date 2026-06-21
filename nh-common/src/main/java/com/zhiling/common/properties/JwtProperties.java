package com.zhiling.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT Properties
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nursing-house.token")
public class JwtProperties {
    private String secret;
    private long expireTime;
    private String header;

}