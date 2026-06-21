package com.zhiling.system.interfaces.security;

import com.zhiling.common.properties.JwtProperties;
import com.zhiling.common.utils.JwtUtil;
import com.zhiling.system.auth.service.support.AuthResourceQueryService;
import com.zhiling.system.auth.service.support.TokenStorePort;
import com.zhiling.system.interfaces.interceptor.UserTokenInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 安全模块自动配置。
 *
 * 注册 JWT 认证/授权相关的 bean，并扫描安全组件。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
@ConditionalOnClass({JwtAuthenticationFilter.class, JwtAuthorizationManager.class})
@ComponentScan(basePackages = "com.zhiling.system.interfaces.security")
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationFilter.class)
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            TokenStorePort tokenStorePort,
            JwtUtil jwtUtil,
            JwtProperties jwtProperties) {
        return new JwtAuthenticationFilter(tokenStorePort, jwtUtil, jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationManager<RequestAuthorizationContext> jwtAuthorizationManager(
            TokenStorePort tokenStorePort,
            JwtUtil jwtUtil,
            AuthResourceQueryService authResourceQueryService) {
        return new JwtAuthorizationManager(tokenStorePort, jwtUtil, authResourceQueryService);
    }

    /**
     * 方法：userTokenInterceptor
     *
     * @author zhanghongyu
     */
    @Bean
    @ConditionalOnMissingBean
    public HandlerInterceptor userTokenInterceptor() {
        return new UserTokenInterceptor();
    }
}