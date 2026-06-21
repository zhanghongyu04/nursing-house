package com.zhiling.agent.infrastructure.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zhiling.common.constant.PromptConstant;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.llm.service.AgentPromptTemplateService;
import com.zhiling.agent.infrastructure.support.AccessScopedVectorStore;
import com.zhiling.agent.infrastructure.support.NormalizedQuestionAnswerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 模块的 Spring AI 配置。
 *
 * 注册 ChatClient 相关 Bean，包括系统提示词、对话客户端和多模态客户端。
 * 仅在 {@code nh.agent.enabled=true}（默认 true）且 Spring AI 在 classpath 时激活。
 *
 * 此配置从 gateway 迁移而来，使 agent 模块完全自治、可插拔。
 *
 * @author zhanghongyu
 */
@Configuration
@ConditionalOnClass({ChatClient.class, ChatModel.class})
@ConditionalOnProperty(prefix = "nh.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentSpringAIConfig {

    private static final int KNOWLEDGE_RAG_TOP_K = 5;
    private static final double KNOWLEDGE_RAG_SIMILARITY_THRESHOLD = 0.35D;

    private final AgentPromptTemplateService agentPromptTemplateService;

    /**
     * 构造器：AgentSpringAIConfig
     *
     * @author zhanghongyu
     */
    public AgentSpringAIConfig(AgentPromptTemplateService agentPromptTemplateService) {
        this.agentPromptTemplateService = agentPromptTemplateService;
    }

    /**
     * 方法：chatRoleSystemPrompt
     *
     * @author zhanghongyu
     */
    @Bean("chatRoleSystemPrompt")
    public String chatRoleSystemPrompt() {
        return agentPromptTemplateService.loadPromptToRedis(PromptConstant.AGENT_CHAT_ROLE_PROMPT_NAME);
    }

    /**
     * 方法：chatKnowledgeRagPromptTemplate
     *
     * @author zhanghongyu
     */
    @Bean("chatKnowledgeRagPromptTemplate")
    public PromptTemplate chatKnowledgeRagPromptTemplate() {
        return new PromptTemplate(agentPromptTemplateService.loadPromptToRedis(
                PromptConstant.AGENT_CHAT_KNOWLEDGE_RAG_PROMPT_NAME));
    }

    /**
     * 方法：Qualifier
     *
     * @author zhanghongyu
     */
    @Bean("chatRoleMultimodalSystemPrompt")
    public String chatRoleMultimodalSystemPrompt(@Qualifier("chatRoleSystemPrompt") String chatRoleSystemPrompt) {
        return chatRoleSystemPrompt + "\n\n"
                + agentPromptTemplateService.loadPromptToRedis(PromptConstant.AGENT_CHAT_MULTIMODAL_PROMPT_NAME);
    }

    /**
     * 构建文本对话客户端。
     * 默认挂载日志顾问与知识库检索顾问，形成基础 RAG 对话链路。
     */
    @Bean
    public ChatClient chatSenceClient(ChatModel model,
                                      VectorStore vectorStore,
                                      SecurityHelper securityHelper,
                                      @Qualifier("chatRoleSystemPrompt") String chatRoleSystemPrompt,
                                      @Qualifier("chatKnowledgeRagPromptTemplate") PromptTemplate chatKnowledgeRagPromptTemplate,
                                      @Qualifier("agentInternalDataToolCallbackProvider") ToolCallbackProvider agentInternalDataToolCallbackProvider) {
        VectorStore ragVectorStore = new AccessScopedVectorStore(vectorStore, securityHelper);
        return baseChatClientBuilder(model, chatRoleSystemPrompt, agentInternalDataToolCallbackProvider)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        NormalizedQuestionAnswerAdvisor.builder(ragVectorStore)
                                // 保持在请求线程执行，避免切到 boundedElastic 后丢失当前用户上下文（机构权限范围）
                                .protectFromBlocking(false)
                                .promptTemplate(chatKnowledgeRagPromptTemplate)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(KNOWLEDGE_RAG_SIMILARITY_THRESHOLD)
                                        .topK(KNOWLEDGE_RAG_TOP_K)
                                        .build())
                                .build()
                )
                .build();
    }

    /**
     * 构建多模态对话客户端。
     * 保留系统提示词，但开启 DashScope 多模态开关，不挂载知识库检索顾问。
     */
    @Bean
    public ChatClient chatSenceMultimodalClient(ChatModel model,
                                                @Qualifier("chatRoleMultimodalSystemPrompt") String chatRoleMultimodalSystemPrompt) {
        return ChatClient.builder(model)
                .defaultSystem(chatRoleMultimodalSystemPrompt)
                .defaultOptions(DashScopeChatOptions.builder()
                        .multiModel(true)
                        .build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    private ChatClient.Builder baseChatClientBuilder(ChatModel model,
                                                     String systemPrompt,
                                                     ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(model)
                .defaultSystem(systemPrompt)
                .defaultToolCallbacks(toolCallbackProvider);
    }
}
