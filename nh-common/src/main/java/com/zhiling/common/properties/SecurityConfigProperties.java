package com.zhiling.common.properties;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 忽略配置及跨域
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nursing-house.framework.security")
public class SecurityConfigProperties {

    String defaulePassword;

    List<String> ignoreUrl = new ArrayList<>();

    List<String> origins = new ArrayList<>();

    String loginPage;

    /**
     * 令牌有效时间
     */
    Integer accessTokenValiditySeconds = 3 * 24 * 3600;

    /**
     * 刷新令牌有效时间
     */
    Integer refreshTokenValiditySeconds = 7 * 24 * 3600;
}