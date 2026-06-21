package com.zhiling.agent.infrastructure.config;

import com.zhiling.agent.infrastructure.mcp.AgentInternalDataMcp;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * Agent 模块自动配置。
 *
 * 注册 Agent 相关的服务适配器、HTTP 控制器和基础设施 bean。
 * 仅在 {@code nh.agent.enabled=true}（默认 true）时激活，移除依赖后系统仍可正常启动。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "nh.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("com.zhiling.agent.infrastructure.persistence.mapper")
@ComponentScan(basePackages = {
    "com.zhiling.agent.interfaces.framework",
    "com.zhiling.agent.interfaces.http",
    "com.zhiling.agent.infrastructure.mcp",
    "com.zhiling.agent.infrastructure.memory.redis",
    "com.zhiling.agent.infrastructure.persistence.repository",
    "com.zhiling.agent.application"
})
@Import(AgentSpringAIConfig.class)
public class AgentAutoConfiguration {

    /**
     * 方法：agentInternalDataToolCallbackProvider
     *
     * @author zhanghongyu
     */
    @Bean("agentInternalDataToolCallbackProvider")
    public ToolCallbackProvider agentInternalDataToolCallbackProvider(AgentInternalDataMcp agentInternalDataMcp) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(agentInternalDataMcp)
                .build();
    }
}