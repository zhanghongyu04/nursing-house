package com.zhiling.agent.interfaces.framework;

import com.zhiling.framework.llm.model.AgentRagSource;
import com.zhiling.framework.llm.model.AgentRagSummary;
import com.zhiling.agent.application.AgentRagService;
import com.zhiling.agent.infrastructure.support.AccessScopedVectorStore;
import com.zhiling.agent.infrastructure.support.ConversationMetaQueryDetector;
import com.zhiling.agent.infrastructure.support.RagSearchSupport;
import com.zhiling.framework.security.SecurityHelper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent RAG 预检索适配器。
 *
 * @author zhanghongyu
 */
@Component
@Slf4j
public class AgentRagServiceAdapter implements AgentRagService {

    private static final double RAG_SIMILARITY_THRESHOLD = 0.35D;
    private static final int RAG_TOP_K = 5;

    private final VectorStore vectorStore;

    /**
     * 构造器：AgentRagServiceAdapter
     *
     * @author zhanghongyu
     */
    public AgentRagServiceAdapter(VectorStore vectorStore, SecurityHelper securityHelper) {
        this.vectorStore = new AccessScopedVectorStore(vectorStore, securityHelper);
    }

    /**
     * 方法：inspectKnowledgeRetrieval
     *
     * @author zhanghongyu
     */
    @Override
    public AgentRagSummary inspectKnowledgeRetrieval(String query) {
        if (query == null || query.trim().isEmpty()) {
            return AgentRagSummary.empty();
        }
        if (ConversationMetaQueryDetector.isConversationMetaQuery(query)) {
            log.info("[RAG] 跳过会话元问题预检索 query='{}'", truncate(query, 120));
            return AgentRagSummary.empty();
        }
        if (RagSearchSupport.shouldSkipKnowledgeRetrieval(query)) {
            log.info("[RAG] 跳过非知识库问题预检索 query='{}'", truncate(query, 120));
            return AgentRagSummary.empty();
        }
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .similarityThreshold(RAG_SIMILARITY_THRESHOLD)
                    .topK(RAG_TOP_K)
                    .build();
            List<Document> hitDocs = RagSearchSupport.similaritySearchWithFallback(
                    vectorStore,
                    searchRequest,
                    query,
                    searchRequest.getFilterExpression());
            if (hitDocs == null || hitDocs.isEmpty()) {
                log.info("[RAG] 命中=0 query='{}'", truncate(query, 120));
                return AgentRagSummary.empty();
            }

            List<AgentRagSource> sources = hitDocs.stream()
                    .map(this::toRetrievalSource)
                    .collect(Collectors.toList());
            String sourceLog = sources.stream()
                    .map(source -> String.format(
                            "file=%s chunk=%s score=%s",
                            source.getFileName(),
                            source.getChunkIndex() == null ? "-" : source.getChunkIndex(),
                            source.getScore() == null ? "-" : String.format("%.4f", source.getScore())
                    ))
                    .collect(Collectors.joining("; "));
            log.info("[RAG] 命中={} query='{}' sources=[{}]", sources.size(), truncate(query, 120), sourceLog);
            return new AgentRagSummary(sources);
        } catch (Exception e) {
            log.error("[RAG] 检索预查询失败 query='{}'", truncate(query, 120), e);
            return AgentRagSummary.empty();
        }
    }

    /**
     * 方法：emptySummary
     *
     * @author zhanghongyu
     */
    @Override
    public AgentRagSummary emptySummary() {
        return AgentRagSummary.empty();
    }

    /**
     * 方法：writeRagHeaders
     *
     * @author zhanghongyu
     */
    @Override
    public void writeRagHeaders(HttpServletResponse response, AgentRagSummary summary) {
        response.setHeader("Access-Control-Expose-Headers", "Captcha-Key,X-RAG-HIT,X-RAG-HIT-COUNT,X-RAG-SOURCES");
        response.setHeader("X-RAG-HIT", summary.isHit() ? "1" : "0");
        response.setHeader("X-RAG-HIT-COUNT", String.valueOf(summary.getHitCount()));
        String encodedSources = URLEncoder.encode(toHeaderValue(summary), StandardCharsets.UTF_8);
        response.setHeader("X-RAG-SOURCES", encodedSources);
    }

    /**
     * 方法：toRetrievalSource
     *
     * @author zhanghongyu
     */
    private AgentRagSource toRetrievalSource(Document doc) {
        Map<String, Object> metadata = doc.getMetadata() == null ? Map.of() : doc.getMetadata();
        String fileName = firstNonBlank(
                asString(metadata.get("file_name")),
                asString(metadata.get("fileName")),
                asString(metadata.get("source")),
                "未知来源"
        );
        Integer chunkIndex = asInteger(metadata.get("chunk_index"));
        if (chunkIndex == null) {
            chunkIndex = asInteger(metadata.get("chunkIndex"));
        }
        Double score = doc.getScore();
        if (score == null) {
            score = asDouble(metadata.get("score"));
        }
        if (score == null) {
            score = asDouble(metadata.get("similarity"));
        }
        if (score == null && metadata.containsKey("distance")) {
            Double distance = asDouble(metadata.get("distance"));
            if (distance != null) {
                score = 1 - distance;
            }
        }
        String snippet = truncate(resolveDocumentText(doc), 80);
        return new AgentRagSource(fileName, chunkIndex, score, snippet);
    }

    /**
     * 方法：resolveDocumentText
     *
     * @author zhanghongyu
     */
    private String resolveDocumentText(Document doc) {
        if (doc == null) {
            return "";
        }
        if (doc.getText() != null && !doc.getText().trim().isEmpty()) {
            return doc.getText();
        }
        Map<String, Object> metadata = doc.getMetadata() == null ? Map.of() : doc.getMetadata();
        String[] keys = {"doc_content", "document_content", "content", "text", "page_content", "chunk_content", "data"};
        for (String key : keys) {
            String value = asString(metadata.get(key));
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    /**
     * 方法：toHeaderValue
     *
     * @author zhanghongyu
     */
    private String toHeaderValue(AgentRagSummary summary) {
        if (summary == null || summary.getSources() == null || summary.getSources().isEmpty()) {
            return "";
        }
        return summary.getSources().stream()
                .map(this::toHeaderItem)
                .collect(Collectors.joining("||"));
    }

    /**
     * 方法：toHeaderItem
     *
     * @author zhanghongyu
     */
    private String toHeaderItem(AgentRagSource source) {
        String safeName = source.getFileName() == null ? "未知来源" : source.getFileName().replace("|", " ");
        String safeSnippet = source.getSnippet() == null ? "" : source.getSnippet().replace("|", " ").replace("||", " ");
        String scoreText = source.getScore() == null ? "" : String.format(Locale.ROOT, "%.4f", source.getScore());
        String chunkText = source.getChunkIndex() == null ? "" : String.valueOf(source.getChunkIndex());
        return String.join("|", safeName, chunkText, scoreText, safeSnippet);
    }

    /**
     * 方法：asString
     *
     * @author zhanghongyu
     */
    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 方法：asInteger
     *
     * @author zhanghongyu
     */
    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 方法：asDouble
     *
     * @author zhanghongyu
     */
    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 方法：firstNonBlank
     *
     * @author zhanghongyu
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 方法：truncate
     *
     * @author zhanghongyu
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String compact = text.replace("\n", " ").replace("\r", " ").trim();
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength) + "...";
    }
}

