package com.zhiling.agent.infrastructure.support;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * RAG 检索辅助能力。
 *
 * 对中文护理问题做轻量查询扩展，降低用户自然语言和教材章节标题不完全一致时的漏召回。
 *
 * @author zhanghongyu
 */
public final class RagSearchSupport {

    private static final Logger log = LoggerFactory.getLogger(RagSearchSupport.class);
    private static final double FALLBACK_SIMILARITY_THRESHOLD = 0.25D;
    private static final int FALLBACK_TOP_K_MULTIPLIER = 3;
    private static final int MAX_EXPANDED_QUERY_COUNT = 5;
    private RagSearchSupport() {
    }

    /**
     * 先按原始 query 检索；无结果时，使用扩展 query 和更低阈值兜底检索。
     */
    public static List<Document> similaritySearchWithFallback(VectorStore vectorStore,
                                                              SearchRequest baseRequest,
                                                              String query,
                                                              Filter.Expression filterExpression) {
        if (vectorStore == null || baseRequest == null || !StringUtils.hasText(query)
                || shouldSkipKnowledgeRetrieval(query)) {
            return List.of();
        }

        SearchRequest primaryRequest = SearchRequest.from(baseRequest)
                .query(query)
                .filterExpression(filterExpression)
                .build();
        List<Document> primaryDocs = safeSearch(vectorStore, primaryRequest);
        if (!primaryDocs.isEmpty()) {
            return limitDistinct(primaryDocs, Math.max(primaryRequest.getTopK(), 1));
        }

        List<String> expandedQueries = expandQuery(query);
        if (expandedQueries.size() <= 1) {
            return List.of();
        }

        int requestedTopK = Math.max(baseRequest.getTopK(), 1);
        int fallbackTopK = Math.max(requestedTopK * FALLBACK_TOP_K_MULTIPLIER, requestedTopK + 4);
        Map<String, Document> merged = new LinkedHashMap<>();
        for (String expandedQuery : expandedQueries) {
            SearchRequest fallbackRequest = SearchRequest.from(baseRequest)
                    .query(expandedQuery)
                    .topK(fallbackTopK)
                    .similarityThreshold(Math.min(baseRequest.getSimilarityThreshold(), FALLBACK_SIMILARITY_THRESHOLD))
                    .filterExpression(filterExpression)
                    .build();
            for (Document document : safeSearch(vectorStore, fallbackRequest)) {
                if (document == null) {
                    continue;
                }
                merged.putIfAbsent(documentKey(document), document);
                if (merged.size() >= requestedTopK) {
                    return new ArrayList<>(merged.values());
                }
            }
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * 为护理领域常见说法补充教材中更可能出现的章节词。
     */
    public static List<String> expandQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        String normalized = query.trim();
        List<String> queries = new ArrayList<>();
        addQuery(queries, normalized);

        String compact = normalized.replaceAll("\\s+", "");
        if (containsAny(compact, "压疮", "褥疮", "皮肤发红", "皮肤破损", "长期卧床", "卧床老人", "翻身")) {
            addQuery(queries, normalized + " 压疮 皮肤护理 病人清洁的护理 卧床 更换卧位 翻身 安全护理");
            addQuery(queries, "压疮的预防及护理 皮肤护理 卧床病人 更换卧位");
        }
        if (containsAny(compact, "生命体征", "体征", "体温", "脉搏", "呼吸", "血压")) {
            addQuery(queries, normalized + " 生命体征的评估及护理 体温 脉搏 呼吸 血压");
            addQuery(queries, "生命体征 体温评估 脉搏评估 呼吸评估 血压评估");
        }
        if (containsAny(compact, "口腔", "晨晚间", "清洁", "头发", "床单")) {
            addQuery(queries, normalized + " 病人清洁的护理 口腔护理 晨晚间护理 皮肤护理");
            addQuery(queries, "病人清洁的护理 口腔护理 头发护理 皮肤护理 晨晚间护理");
        }
        if (containsAny(compact, "给药", "服药", "药物", "过敏", "注射")) {
            addQuery(queries, normalized + " 药物疗法 给药的基本知识 口服给药 注射法 药物过敏试验");
            addQuery(queries, "药物疗法和过敏试验法 给药的基本知识 口服给药法 注射法");
        }
        if (containsAny(compact, "饮食", "鼻饲", "营养", "出入液量")) {
            addQuery(queries, normalized + " 病人饮食的护理 医院饮食 饮食护理 鼻饲法 出入液量记录");
            addQuery(queries, "病人饮食的护理 饮食护理 鼻饲法 出入液量记录");
        }
        if (containsAny(compact, "排泄", "排尿", "排便", "便秘", "尿潴留")) {
            addQuery(queries, normalized + " 排泄护理 排尿护理 排便护理 排气护理");
            addQuery(queries, "排泄护理 排尿护理 排便护理 排气护理");
        }

        if (queries.size() > MAX_EXPANDED_QUERY_COUNT) {
            return queries.subList(0, MAX_EXPANDED_QUERY_COUNT);
        }
        return queries;
    }

    /**
     * 过滤寒暄、确认等不需要知识库支撑的问题，避免无意义 embedding 调用放大网络抖动。
     */
    public static boolean shouldSkipKnowledgeRetrieval(String query) {
        return ConversationQueryClassifier.shouldSkipKnowledgeRetrieval(query);
    }

    private static List<Document> safeSearch(VectorStore vectorStore, SearchRequest request) {
        List<Document> documents;
        try {
            documents = vectorStore.similaritySearch(request);
        } catch (Exception ex) {
            log.warn("[RAG] 向量检索失败，已降级为空命中: query='{}', reason={}",
                    request == null ? null : truncate(request.getQuery(), 120),
                    ex.getMessage());
            return List.of();
        }
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        return documents.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<Document> limitDistinct(List<Document> documents, int limit) {
        Map<String, Document> merged = new LinkedHashMap<>();
        for (Document document : documents) {
            if (document == null) {
                continue;
            }
            merged.putIfAbsent(documentKey(document), document);
            if (merged.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(merged.values());
    }

    private static String documentKey(Document document) {
        if (StringUtils.hasText(document.getId())) {
            return document.getId();
        }
        Map<String, Object> metadata = document.getMetadata();
        if (metadata != null) {
            String fileName = Objects.toString(metadata.getOrDefault("file_name", ""), "");
            String chunkIndex = Objects.toString(metadata.getOrDefault("chunk_index", ""), "");
            if (StringUtils.hasText(fileName) || StringUtils.hasText(chunkIndex)) {
                return fileName + "#" + chunkIndex;
            }
        }
        return Integer.toHexString(System.identityHashCode(document));
    }

    private static void addQuery(List<String> queries, String query) {
        if (!StringUtils.hasText(query)) {
            return;
        }
        String normalized = query.trim();
        String normalizedKey = normalized.toLowerCase(Locale.ROOT);
        boolean exists = queries.stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedKey::equals);
        if (!exists) {
            queries.add(normalized);
        }
    }

    private static boolean containsAny(String text, String... words) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String word : words) {
            if (StringUtils.hasText(word) && text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
