package com.zhiling.agent.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiling.agent.application.port.AgentContextMemoryRecoveryPort;
import com.zhiling.framework.autoconfigure.capability.CapabilityRegistry;
import com.zhiling.framework.autoconfigure.properties.NhModuleProperties;
import com.zhiling.framework.llm.core.service.LlmService;
import com.zhiling.framework.llm.core.service.GuardPort;
import com.zhiling.framework.llm.core.service.MemoryPort;
import com.zhiling.framework.llm.core.service.PromptProvider;
import com.zhiling.framework.llm.core.service.RagPort;
import com.zhiling.framework.llm.core.service.ToolPort;
import com.zhiling.framework.redis.NhRedisBeanNames;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.agent.application.NoOpLlmService;
import com.zhiling.agent.application.SpringAiLlmService;
import com.zhiling.agent.domain.guard.DefaultGuardService;
import com.zhiling.agent.domain.guard.PassThroughGuardService;
import com.zhiling.agent.infrastructure.memory.InMemoryMemoryService;
import com.zhiling.agent.infrastructure.memory.NoOpMemoryService;
import com.zhiling.agent.infrastructure.memory.RedisMemoryService;
import com.zhiling.agent.domain.prompt.BeanPromptProvider;
import com.zhiling.agent.domain.prompt.DefaultPromptProvider;
import com.zhiling.agent.infrastructure.config.properties.LlmCoreChatProperties;
import com.zhiling.agent.infrastructure.config.properties.LlmCoreGuardProperties;
import com.zhiling.agent.infrastructure.config.properties.LlmCoreMemoryProperties;
import com.zhiling.agent.infrastructure.config.properties.LlmCorePromptProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

/**
 * LLM Core 统一自动装配（chat/prompt/guard/memory）。
 *
 * 只负责 LLM 核心能力的 bean 注册，不耦合 HTTP 控制器暴露。
 * HTTP 控制器由 {@link AgentAutoConfiguration} 按需扫描注册。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
@ConditionalOnClass(LlmService.class)
@EnableConfigurationProperties({
        LlmCoreChatProperties.class,
        LlmCorePromptProperties.class,
        LlmCoreGuardProperties.class,
        LlmCoreMemoryProperties.class
})
public class LlmCoreAutoConfiguration {

    @Bean
    @ConditionalOnBean(name = "chatRoleSystemPrompt")
    @ConditionalOnMissingBean(PromptProvider.class)
    public PromptProvider beanPromptProvider(@Qualifier("chatRoleSystemPrompt") String chatRoleSystemPrompt,
                                             CapabilityRegistry capabilityRegistry) {
        capabilityRegistry.register("llm.prompt", true);
        return new BeanPromptProvider(chatRoleSystemPrompt);
    }

    @Bean
    @ConditionalOnMissingBean(PromptProvider.class)
    public PromptProvider defaultPromptProvider(LlmCorePromptProperties properties,
                                                CapabilityRegistry capabilityRegistry) {
        capabilityRegistry.register("llm.prompt", true);
        return new DefaultPromptProvider(properties);
    }

    @Bean
    @ConditionalOnMissingBean(GuardPort.class)
    public GuardPort guardPort(LlmCoreGuardProperties properties,
                               CapabilityRegistry capabilityRegistry) {
        if (properties.isEnabled()) {
            capabilityRegistry.register("llm.guard", true);
            return new DefaultGuardService();
        }
        capabilityRegistry.register("llm.guard", false);
        return new PassThroughGuardService();
    }

    @Bean
    @ConditionalOnMissingBean(MemoryPort.class)
    @ConditionalOnProperty(prefix = "nh.llm.memory", name = "provider", havingValue = "redis")
    @ConditionalOnClass(StringRedisTemplate.class)
    public MemoryPort redisMemoryPort(@Qualifier(NhRedisBeanNames.AGENT_STRING_REDIS_TEMPLATE) StringRedisTemplate redisTemplate,
                                      ObjectMapper objectMapper,
                                      NhModuleProperties moduleProperties,
                                      LlmCoreMemoryProperties properties,
                                      CapabilityRegistry capabilityRegistry,
                                      SecurityHelper securityHelper,
                                      ObjectProvider<AgentContextMemoryRecoveryPort> memoryRecoveryPortProvider) {
        if (!moduleProperties.isLlmEnabled() || !properties.isEnabled()) {
            capabilityRegistry.register("llm.memory", false);
            return new NoOpMemoryService();
        }
        capabilityRegistry.register("llm.memory", true);
        return new RedisMemoryService(
                redisTemplate,
                objectMapper,
                properties.getRedisKeyPrefix(),
                securityHelper,
                memoryRecoveryPortProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnMissingBean(MemoryPort.class)
    @ConditionalOnProperty(prefix = "nh.llm.memory", name = "provider", havingValue = "in-memory", matchIfMissing = true)
    public MemoryPort inMemoryMemoryPort(NhModuleProperties moduleProperties,
                                         LlmCoreMemoryProperties properties,
                                         CapabilityRegistry capabilityRegistry) {
        if (!moduleProperties.isLlmEnabled() || !properties.isEnabled()) {
            capabilityRegistry.register("llm.memory", false);
            return new NoOpMemoryService();
        }
        capabilityRegistry.register("llm.memory", true);
        return new InMemoryMemoryService();
    }

    /**
     * 方法：noOpMemoryPort
     *
     * @author zhanghongyu
     */
    @Bean
    @ConditionalOnMissingBean(MemoryPort.class)
    public MemoryPort noOpMemoryPort(CapabilityRegistry capabilityRegistry) {
        capabilityRegistry.register("llm.memory", false);
        return new NoOpMemoryService();
    }

    /**
     * 统一组装 LLM 主服务。
     * 当聊天能力未启用、ChatClient 缺失或模块被整体关闭时，自动降级为 NoOp 实现。
     */
    @Bean
    @ConditionalOnMissingBean(LlmService.class)
    public LlmService llmService(ObjectProvider<ChatClient> chatClientProvider,
                               PromptProvider promptProvider,
                               GuardPort guardPort,
                               ObjectProvider<MemoryPort> memoryPortProvider,
                               ObjectProvider<RagPort> ragPortProvider,
                               ObjectProvider<ToolPort> toolPortProvider,
                               CapabilityRegistry capabilityRegistry,
                               LlmCoreChatProperties chatProperties,
                               NhModuleProperties moduleProperties) {
        ChatClient chatClient = chatClientProvider.stream().findFirst().orElse(null);
        if (!moduleProperties.isLlmEnabled() || !chatProperties.isEnabled() || chatClient == null) {
            capabilityRegistry.register("llm.chat", false);
            capabilityRegistry.logMatrix();
            return new NoOpLlmService();
        }
        capabilityRegistry.register("llm.chat", true);
        MemoryPort memoryPort = memoryPortProvider.getIfAvailable(NoOpMemoryService::new);
        if (!capabilityRegistry.snapshot().containsKey("llm.memory")) {
            capabilityRegistry.register("llm.memory", !(memoryPort instanceof NoOpMemoryService));
        }
        RagPort ragPort = ragPortProvider.getIfAvailable(() -> (prompt, metadata) ->
                com.zhiling.framework.llm.core.model.RagResult.builder()
                        .prompt(prompt == null ? "" : prompt)
                        .sources(List.of())
                        .build());
        ToolPort toolPort = toolPortProvider.getIfAvailable(() -> (prompt, metadata) ->
                com.zhiling.framework.llm.core.model.ToolResult.builder()
                        .prompt(prompt == null ? "" : prompt)
                        .build());
        Map<String, Boolean> flags = SpringAiLlmService.defaultFlags();
        flags.putAll(capabilityRegistry.snapshot());
        capabilityRegistry.logMatrix();
        return new SpringAiLlmService(
                chatClient,
                promptProvider,
                guardPort,
                memoryPort,
                ragPort,
                toolPort,
                flags,
                chatProperties.getContextMessageLimit()
        );
    }
}

