package com.zhiling.framework.autoconfigure;

import com.zhiling.framework.autoconfigure.capability.CapabilityRegistry;
import com.zhiling.framework.autoconfigure.properties.NhLlmProperties;
import com.zhiling.framework.autoconfigure.properties.NhModuleProperties;
import com.zhiling.framework.security.CurrentUserProvider;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.security.port.LiveAccessScopePort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * 平台自动装配基座。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
@EnableConfigurationProperties({NhModuleProperties.class, NhLlmProperties.class})
@ComponentScan(basePackages = {
    "com.zhiling.framework.monitor.adapter",
    "com.zhiling.framework.file"
})
public class NhPlatformAutoConfiguration {

    /**
     * 方法：capabilityRegistry
     *
     * @author zhanghongyu
     */
    @Bean
    public CapabilityRegistry capabilityRegistry() {
        return new CapabilityRegistry();
    }

    /**
     * 方法：securityHelper
     *
     * @author zhanghongyu
     */
    @Bean
    @ConditionalOnBean(CurrentUserProvider.class)
    @ConditionalOnMissingBean
    public SecurityHelper securityHelper(CurrentUserProvider currentUserProvider,
                                       ObjectProvider<LiveAccessScopePort> liveAccessScopePortProvider) {
        return new SecurityHelper(currentUserProvider, liveAccessScopePortProvider.getIfAvailable());
    }
}