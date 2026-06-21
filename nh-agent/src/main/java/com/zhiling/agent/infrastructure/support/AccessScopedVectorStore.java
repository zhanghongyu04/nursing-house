package com.zhiling.agent.infrastructure.support;

import com.zhiling.common.constant.RoleConstant;
import com.zhiling.framework.security.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Agent 检索用向量库访问范围包装器。
 *
 * 为聊天 RAG 和预检索统一补齐机构范围过滤，避免跨机构命中文档。
 *
 * @author zhanghongyu
 */
public class AccessScopedVectorStore implements VectorStore {

    private static final Logger log = LoggerFactory.getLogger(AccessScopedVectorStore.class);
    private static final int MAX_EXPANDED_TOP_K = 18;
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("(?<![A-Za-z])\\d{2,4}(?![A-Za-z])");
    private static final Pattern OUTLINE_HEADING_PATTERN = Pattern.compile(
            "第[一二三四五六七八九十百千万零〇\\d]+[章节部分篇卷条款目]|第[一二三四五六七八九十百千万零〇\\d]+节");
    private static final Pattern OCR_NOISE_PATTERN = Pattern.compile("[!！?？]{4,}|[•·●■◆◦]{3,}");

    private final VectorStore delegate;
    private final SecurityHelper securityHelper;
    private final FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();

    /**
     * 构造器：AccessScopedVectorStore
     *
     * @author zhanghongyu
     */
    public AccessScopedVectorStore(VectorStore delegate, SecurityHelper securityHelper) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.securityHelper = Objects.requireNonNull(securityHelper, "securityHelper must not be null");
    }

    /**
     * 方法：getName
     *
     * @author zhanghongyu
     */
    @Override
    public String getName() {
        return delegate.getName();
    }

    /**
     * 方法：add
     *
     * @author zhanghongyu
     */
    @Override
    public void add(List<Document> documents) {
        delegate.add(documents);
    }

    /**
     * 方法：delete
     *
     * @author zhanghongyu
     */
    @Override
    public void delete(List<String> ids) {
        delegate.delete(ids);
    }

    /**
     * 方法：delete
     *
     * @author zhanghongyu
     */
    @Override
    public void delete(Filter.Expression filterExpression) {
        delegate.delete(filterExpression);
    }

    /**
     * 方法：getNativeClient
     *
     * @author zhanghongyu
     */
    @Override
    public <T> Optional<T> getNativeClient() {
        return delegate.getNativeClient();
    }

    /**
     * 方法：similaritySearch
     *
     * @author zhanghongyu
     */
    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        SearchRequest scopedRequest = applyScopeFilter(request);
        if (scopedRequest == null) {
            if (log.isDebugEnabled()) {
                log.debug("[RAG] 当前用户无知识库检索范围, userId={}, sanaId={}, roles={}",
                        securityHelper.getCurrentUserId(),
                        securityHelper.getCurrentSanaId(),
                        securityHelper.getCurrentAccessScope().map(scope -> scope.getRoleLabels()).orElse(Set.of()));
            }
            return List.of();
        }
        int requestedTopK = Math.max(scopedRequest.getTopK(), 1);
        SearchRequest expandedRequest = expandCandidateWindow(scopedRequest, requestedTopK);
        List<Document> rawDocuments = delegate.similaritySearch(expandedRequest);
        return optimizeDocuments(rawDocuments, requestedTopK);
    }

    /**
     * 方法：applyScopeFilter
     *
     * @author zhanghongyu
     */
    private SearchRequest applyScopeFilter(SearchRequest request) {
        if (request == null) {
            return null;
        }
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            return request;
        }
        if (!securityHelper.hasAnyRoleForSensitiveOperation(
                RoleConstant.PARENT_ORG_ADMIN, RoleConstant.ORG_ADMIN, RoleConstant.NURSE)) {
            return null;
        }

        Set<Long> sanaScopeIds = securityHelper.getCurrentSanaScopeIdsForSensitiveOperation();
        if (sanaScopeIds == null || sanaScopeIds.isEmpty()) {
            return null;
        }

        List<Object> scopedOwnerIds = sanaScopeIds.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .map(value -> (Object) value)
                .toList();
        if (scopedOwnerIds.isEmpty()) {
            return null;
        }

        Filter.Expression scopeExpression = filterExpressionBuilder.in("owner_sana_id", scopedOwnerIds).build();
        Filter.Expression mergedExpression = mergeExpression(request.getFilterExpression(), scopeExpression);
        return SearchRequest.from(request)
                .filterExpression(mergedExpression)
                .build();
    }

    /**
     * 方法：expandCandidateWindow
     *
     * @author zhanghongyu
     */
    private SearchRequest expandCandidateWindow(SearchRequest request, int requestedTopK) {
        int expandedTopK = Math.min(Math.max(requestedTopK * 3, requestedTopK + 6), MAX_EXPANDED_TOP_K);
        if (expandedTopK <= requestedTopK) {
            return request;
        }
        return SearchRequest.from(request)
                .topK(expandedTopK)
                .build();
    }

    /**
     * 方法：optimizeDocuments
     *
     * @author zhanghongyu
     */
    private List<Document> optimizeDocuments(List<Document> documents, int requestedTopK) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        List<Document> filteredDocuments = documents.stream()
                .filter(Objects::nonNull)
                .filter(document -> !isDirectoryLikeNoise(document))
                .limit(requestedTopK)
                .toList();
        if (!filteredDocuments.isEmpty()) {
            return filteredDocuments;
        }
        return documents.stream()
                .filter(Objects::nonNull)
                .limit(requestedTopK)
                .toList();
    }

    /**
     * 方法：isDirectoryLikeNoise
     *
     * @author zhanghongyu
     */
    private boolean isDirectoryLikeNoise(Document document) {
        String text = resolveDocumentText(document);
        if (text == null || text.isBlank()) {
            return true;
        }

        String normalized = text.replace('\r', '\n').trim();
        String[] lines = normalized.split("\\n+");
        long shortLineCount = Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !line.isEmpty() && line.length() <= 26)
                .count();
        long pageNumberCount = PAGE_NUMBER_PATTERN.matcher(normalized).results().count();
        long outlineHeadingCount = OUTLINE_HEADING_PATTERN.matcher(normalized).results().count();
        long ocrNoiseCount = OCR_NOISE_PATTERN.matcher(normalized).results().count();
        long sentenceEndingCount = normalized.chars()
                .filter(this::isSentenceEnding)
                .count();

        boolean likelyDirectoryPage = pageNumberCount >= 8
                && outlineHeadingCount >= 4
                && shortLineCount >= 10
                && sentenceEndingCount <= 3;
        boolean likelyOcrNoisePage = ocrNoiseCount >= 2
                && pageNumberCount >= 5
                && shortLineCount >= 8
                && sentenceEndingCount <= 4;

        if ((likelyDirectoryPage || likelyOcrNoisePage) && log.isDebugEnabled()) {
            log.debug("[RAG] 过滤目录噪音块, file={}, chunkIndex={}, score={}",
                    metadataValue(document, "file_name"),
                    metadataValue(document, "chunk_index"),
                    document.getScore());
        }

        return likelyDirectoryPage || likelyOcrNoisePage;
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
        if (document.getText() != null && !document.getText().trim().isEmpty()) {
            return document.getText();
        }
        if (document.getMetadata() == null || document.getMetadata().isEmpty()) {
            return "";
        }
        String[] keys = {"doc_content", "document_content", "content", "text", "page_content", "chunk_content", "data"};
        for (String key : keys) {
            Object value = document.getMetadata().get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    /**
     * 方法：isSentenceEnding
     *
     * @author zhanghongyu
     */
    private boolean isSentenceEnding(int codePoint) {
        return codePoint == '。'
                || codePoint == '！'
                || codePoint == '？'
                || codePoint == '；'
                || codePoint == '.'
                || codePoint == '!'
                || codePoint == '?'
                || codePoint == ';';
    }

    /**
     * 方法：metadataValue
     *
     * @author zhanghongyu
     */
    private Object metadataValue(Document document, String key) {
        if (document == null || document.getMetadata() == null) {
            return null;
        }
        return document.getMetadata().get(key);
    }

    /**
     * 方法：mergeExpression
     *
     * @author zhanghongyu
     */
    private Filter.Expression mergeExpression(Filter.Expression originalExpression, Filter.Expression scopeExpression) {
        if (originalExpression == null) {
            return scopeExpression;
        }
        return filterExpressionBuilder.and(
                new FilterExpressionBuilder.Op(new Filter.Group(originalExpression)),
                new FilterExpressionBuilder.Op(new Filter.Group(scopeExpression))
        ).build();
    }
}
