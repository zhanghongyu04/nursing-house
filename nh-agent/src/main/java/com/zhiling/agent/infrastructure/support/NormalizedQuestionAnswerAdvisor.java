package com.zhiling.agent.infrastructure.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 归一化查询词的知识库问答顾问。
 *
 * Spring AI 官方 {@code QuestionAnswerAdvisor} 会直接使用当前 user message 全文做检索。
 * 当前项目会在进入 ChatClient 前把历史对话拼到 user message 中，导致实际检索词可能被历史噪音稀释。
 * 因此此处仅提取“当前用户问题”片段作为向量检索 query，其余流程保持与官方实现一致。
 *
 * @author zhanghongyu
 */
public class NormalizedQuestionAnswerAdvisor implements BaseAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";
    public static final String FILTER_EXPRESSION = "qa_filter_expression";

    private static final Logger log = LoggerFactory.getLogger(NormalizedQuestionAnswerAdvisor.class);
    private static final String MEMORY_QUESTION_MARKER = "[当前用户问题]";
    private static final String LEGACY_QUESTION_MARKER = "当前用户问题：";
    private static final int DEFAULT_ORDER = 0;

    private final VectorStore vectorStore;
    private final PromptTemplate promptTemplate;
    private final SearchRequest searchRequest;
    private final Scheduler scheduler;
    private final int order;

    NormalizedQuestionAnswerAdvisor(VectorStore vectorStore, SearchRequest searchRequest,
                                    @Nullable PromptTemplate promptTemplate,
                                    @Nullable Scheduler scheduler,
                                    int order) {
        Assert.notNull(vectorStore, "vectorStore cannot be null");
        Assert.notNull(searchRequest, "searchRequest cannot be null");
        Assert.notNull(promptTemplate, "promptTemplate cannot be null");
        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.promptTemplate = promptTemplate;
        this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
        this.order = order;
    }

    /**
     * 方法：builder
     *
     * @author zhanghongyu
     */
    public static Builder builder(VectorStore vectorStore) {
        return new Builder(vectorStore);
    }

    /**
     * 方法：getOrder
     *
     * @author zhanghongyu
     */
    @Override
    public int getOrder() {
        return this.order;
    }

    /**
     * 方法：before
     *
     * @author zhanghongyu
     */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
        String rawUserText = userMessage == null ? "" : userMessage.getText();
        String effectiveQuery = extractEffectiveQuery(rawUserText);
        ConversationQueryType queryType = ConversationQueryClassifier.classify(effectiveQuery);
        if (queryType != ConversationQueryType.KNOWLEDGE) {
            if (log.isInfoEnabled()) {
                log.info("[RAG][Advisor] skip queryType={}, query='{}'", queryType, truncate(effectiveQuery, 120));
            }
            Map<String, Object> context = new HashMap<>(chatClientRequest.context());
            context.put(RETRIEVED_DOCUMENTS, List.of());
            return chatClientRequest.mutate()
                    .context(context)
                    .build();
        }

        Filter.Expression filterExpression = doGetFilterExpression(chatClientRequest.context());
        List<Document> documents = RagSearchSupport.similaritySearchWithFallback(
                this.vectorStore,
                this.searchRequest,
                effectiveQuery,
                filterExpression);
        List<String> textPayloads = documents == null ? List.of() : documents.stream()
                .map(this::resolveDocumentText)
                .filter(StringUtils::hasText)
                .toList();
        List<Document> effectiveDocuments = documents == null ? List.of() : documents.stream()
                .filter(Objects::nonNull)
                .toList();

        Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        context.put(RETRIEVED_DOCUMENTS, documents);

        String documentContext = textPayloads.stream()
                .collect(Collectors.joining(System.lineSeparator()));

        if (log.isInfoEnabled()) {
            log.info("[RAG][Advisor] query='{}', rawQueryLength={}, hitCount={}, textDocCount={}, contextLength={}",
                    truncate(effectiveQuery, 120),
                    rawUserText == null ? 0 : rawUserText.length(),
                    documents == null ? 0 : documents.size(),
                    textPayloads.size(),
                    documentContext.length());
        }

        String augmentedUserText = this.promptTemplate.render(Map.of(
                "query", effectiveQuery,
                "question_answer_context", documentContext
        ));

        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
                .context(context)
                .build();
    }

    /**
     * 方法：after
     *
     * @author zhanghongyu
     */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }
        chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, chatClientResponse.context().get(RETRIEVED_DOCUMENTS));
        return ChatClientResponse.builder()
                .chatResponse(chatResponseBuilder.build())
                .context(chatClientResponse.context())
                .build();
    }

    /**
     * 方法：doGetFilterExpression
     *
     * @author zhanghongyu
     */
    @Nullable
    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
        if (!context.containsKey(FILTER_EXPRESSION)
                || !StringUtils.hasText(String.valueOf(context.get(FILTER_EXPRESSION)))) {
            return this.searchRequest.getFilterExpression();
        }
        return new FilterExpressionTextParser().parse(String.valueOf(context.get(FILTER_EXPRESSION)));
    }

    /**
     * 方法：getScheduler
     *
     * @author zhanghongyu
     */
    @Override
    public Scheduler getScheduler() {
        return this.scheduler;
    }

    /**
     * 方法：extractEffectiveQuery
     *
     * @author zhanghongyu
     */
    private String extractEffectiveQuery(String userText) {
        if (!StringUtils.hasText(userText)) {
            return "";
        }
        int questionIndex = userText.lastIndexOf(MEMORY_QUESTION_MARKER);
        if (questionIndex >= 0) {
            return userText.substring(questionIndex + MEMORY_QUESTION_MARKER.length()).trim();
        }
        questionIndex = userText.lastIndexOf(LEGACY_QUESTION_MARKER);
        if (questionIndex >= 0) {
            return userText.substring(questionIndex + LEGACY_QUESTION_MARKER.length()).trim();
        }
        return userText.trim();
    }

    /**
     * 方法：truncate
     *
     * @author zhanghongyu
     */
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * 方法：resolveDocumentText
     *
     * @author zhanghongyu
     */
    private String resolveDocumentText(Document document) {
        if (document == null) {
            return "";
        }
        if (StringUtils.hasText(document.getText())) {
            return document.getText();
        }
        Map<String, Object> metadata = document.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        String[] fallbackKeys = {
                "doc_content",
                "document_content",
                "content",
                "text",
                "page_content",
                "chunk_content",
                "data"
        };
        for (String key : fallbackKeys) {
            Object value = metadata.get(key);
            if (value instanceof String valueText && StringUtils.hasText(valueText)) {
                return valueText;
            }
        }
        return "";
    }

    public static final class Builder {

        private final VectorStore vectorStore;
        private SearchRequest searchRequest = SearchRequest.builder().build();
        private PromptTemplate promptTemplate;
        private Scheduler scheduler;
        private int order = DEFAULT_ORDER;

        /**
         * 方法：Builder
         *
         * @author zhanghongyu
         */
        private Builder(VectorStore vectorStore) {
            Assert.notNull(vectorStore, "The vectorStore must not be null!");
            this.vectorStore = vectorStore;
        }

        /**
         * 方法：promptTemplate
         *
         * @author zhanghongyu
         */
        public Builder promptTemplate(PromptTemplate promptTemplate) {
            Assert.notNull(promptTemplate, "promptTemplate cannot be null");
            this.promptTemplate = promptTemplate;
            return this;
        }

        /**
         * 方法：searchRequest
         *
         * @author zhanghongyu
         */
        public Builder searchRequest(SearchRequest searchRequest) {
            Assert.notNull(searchRequest, "The searchRequest must not be null!");
            this.searchRequest = searchRequest;
            return this;
        }

        /**
         * 方法：protectFromBlocking
         *
         * @author zhanghongyu
         */
        public Builder protectFromBlocking(boolean protectFromBlocking) {
            this.scheduler = protectFromBlocking ? BaseAdvisor.DEFAULT_SCHEDULER : Schedulers.immediate();
            return this;
        }

        /**
         * 方法：scheduler
         *
         * @author zhanghongyu
         */
        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        /**
         * 方法：order
         *
         * @author zhanghongyu
         */
        public Builder order(int order) {
            this.order = order;
            return this;
        }

        /**
         * 方法：build
         *
         * @author zhanghongyu
         */
        public NormalizedQuestionAnswerAdvisor build() {
            return new NormalizedQuestionAnswerAdvisor(
                    this.vectorStore,
                    this.searchRequest,
                    this.promptTemplate,
                    this.scheduler,
                    this.order
            );
        }
    }
}
