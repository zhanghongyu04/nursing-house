package com.zhiling.system.application.service.impl;

import com.zhiling.common.properties.QdrantProperties;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.system.application.service.VectorStoreService;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 向量库管理服务实现。
 *
 * 管理类查询改为直接走 Qdrant 的 count/scroll，避免仅查看知识库列表时
 * 也触发 embedding 请求，进而受到外部 DashScope DNS/网络抖动影响。
 *
 * @author zhanghongyu
 */
@Service
@Slf4j
public class VectorStoreServiceImpl implements VectorStoreService {

    private static final int SCROLL_BATCH_SIZE = 256;
    private static final Duration QDRANT_TIMEOUT = Duration.ofSeconds(10);

    private final VectorStore vectorStore;
    private final QdrantClient qdrantClient;
    private final QdrantProperties qdrantProperties;
    private final SecurityHelper securityHelper;

    public VectorStoreServiceImpl(VectorStore vectorStore,
                                  QdrantClient qdrantClient,
                                  QdrantProperties qdrantProperties,
                                  SecurityHelper securityHelper) {
        this.vectorStore = vectorStore;
        this.qdrantClient = qdrantClient;
        this.qdrantProperties = qdrantProperties;
        this.securityHelper = securityHelper;
    }

    @Override
    public VectorStoreStats getStats() {
        try {
            Long currentSanaId = securityHelper.getCurrentSanaId();
            Set<Long> sanaScopeIds = securityHelper.getCurrentSanaScopeIdsForSensitiveOperation();
            log.info("[向量库Stats] 当前用户ID: {}, sanaId: {}, sanaScopeIds: {}, 角色: {}",
                    securityHelper.getCurrentUserId(), currentSanaId, sanaScopeIds,
                    securityHelper.getCurrentAccessScope().map(as -> as.getRoleLabels()).orElse(null));

            List<DocumentInfo> documentInfos = loadAccessibleDocumentInfos(null);
            log.info("[向量库Stats] 过滤后文档数量: {}", documentInfos.size());

            long totalChunks = documentInfos.size();
            long totalDocuments = documentInfos.stream()
                    .map(info -> StringUtils.defaultIfBlank(info.getFileName(), "unknown"))
                    .distinct()
                    .count();

            return new VectorStoreStats(totalDocuments, totalChunks, totalChunks, qdrantProperties.getCollectionName());
        } catch (Exception e) {
            log.error("获取向量库统计信息失败: {}", e.getMessage(), e);
            return new VectorStoreStats(0, 0, 0, qdrantProperties.getCollectionName());
        }
    }

    @Override
    public DocumentListResult getDocuments(int page, int pageSize) {
        try {
            List<DocumentInfo> documentInfos = normalizeChunkSequence(loadAccessibleDocumentInfos(null));

            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, documentInfos.size());
            List<DocumentInfo> pagedDocuments = start < documentInfos.size()
                    ? documentInfos.subList(start, end)
                    : new ArrayList<>();

            return new DocumentListResult(pagedDocuments, documentInfos.size(), page, pageSize);
        } catch (Exception e) {
            log.error("获取文档列表失败: {}", e.getMessage(), e);
            return new DocumentListResult(new ArrayList<>(), 0, page, pageSize);
        }
    }

    @Override
    public List<DocumentInfo> getDocumentsByFileName(String fileName) {
        try {
            return normalizeChunkSequence(loadAccessibleDocumentInfos(fileName));
        } catch (Exception e) {
            log.error("根据文件名获取文档失败: {}, 文件名: {}", e.getMessage(), fileName, e);
            return new ArrayList<>();
        }
    }

    @Override
    public int deleteDocumentsByFileName(String fileName) {
        try {
            List<DocumentInfo> documents = getDocumentsByFileName(fileName);
            if (documents.isEmpty()) {
                log.warn("未找到文件: {}", fileName);
                return 0;
            }

            List<String> ids = documents.stream()
                    .map(DocumentInfo::getId)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            return deleteDocuments(ids);
        } catch (Exception e) {
            log.error("根据文件名删除文档失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public boolean deleteDocument(String documentId) {
        try {
            if (StringUtils.isBlank(documentId)) {
                log.warn("文档ID为空，无法删除");
                return false;
            }

            boolean accessible = loadAccessibleDocumentInfos(null).stream()
                    .anyMatch(d -> documentId.equals(d.getId()));
            if (!accessible) {
                log.warn("无权限删除文档或文档不存在: {}", documentId);
                return false;
            }

            vectorStore.delete(Collections.singletonList(documentId));
            log.info("删除文档成功: {}", documentId);
            return true;
        } catch (Exception e) {
            log.error("删除文档失败: {}, ID: {}", e.getMessage(), documentId, e);
            return false;
        }
    }

    @Override
    public int deleteDocuments(List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return 0;
        }

        try {
            List<String> validIds = documentIds.stream()
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (validIds.isEmpty()) {
                return 0;
            }

            Set<String> accessibleIds = loadAccessibleDocumentInfos(null).stream()
                    .map(DocumentInfo::getId)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());

            List<String> idsToDelete = validIds.stream()
                    .filter(accessibleIds::contains)
                    .toList();
            if (idsToDelete.isEmpty()) {
                return 0;
            }

            vectorStore.delete(idsToDelete);
            int count = idsToDelete.size();
            log.info("批量删除文档成功, 数量: {}", count);
            return count;
        } catch (Exception e) {
            log.error("批量删除文档失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    @Override
    public int clearAll() {
        try {
            List<DocumentInfo> results = loadAccessibleDocumentInfos(null);
            if (results.isEmpty()) {
                log.info("向量库为空，无需清空");
                return 0;
            }

            List<String> ids = results.stream()
                    .map(DocumentInfo::getId)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());

            vectorStore.delete(ids);

            int deletedCount = ids.size();
            log.info("清空向量库成功, 集合: {}, 删除数量: {}", qdrantProperties.getCollectionName(), deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("清空向量库失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    private List<DocumentInfo> normalizeChunkSequence(List<DocumentInfo> documentInfos) {
        if (documentInfos == null || documentInfos.isEmpty()) {
            return documentInfos;
        }

        Map<String, List<DocumentInfo>> grouped = documentInfos.stream()
                .collect(Collectors.groupingBy(info -> StringUtils.defaultIfBlank(info.getFileName(), "unknown")));

        List<DocumentInfo> normalized = new ArrayList<>(documentInfos.size());
        grouped.forEach((fileName, infos) -> {
            infos.sort(Comparator
                    .comparing((DocumentInfo info) -> info.getChunkIndex() == null ? Integer.MAX_VALUE : info.getChunkIndex())
                    .thenComparing(info -> StringUtils.defaultString(info.getId())));

            int total = infos.size();
            for (int i = 0; i < infos.size(); i++) {
                DocumentInfo info = infos.get(i);
                info.setChunkIndex(i + 1);
                info.setTotalChunks(total);
                normalized.add(info);
            }
        });

        normalized.sort(Comparator
                .comparing((DocumentInfo info) -> StringUtils.defaultIfBlank(info.getFileName(), "unknown"))
                .thenComparing(info -> info.getChunkIndex() == null ? Integer.MAX_VALUE : info.getChunkIndex())
                .thenComparing(info -> StringUtils.defaultString(info.getId())));

        return normalized;
    }

    private List<DocumentInfo> loadAccessibleDocumentInfos(String fileName) throws Exception {
        Points.Filter filter = buildAccessFilter(fileName);
        return scrollAllPoints(filter).stream()
                .map(this::retrievedPointToInfo)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<Points.RetrievedPoint> scrollAllPoints(Points.Filter filter) throws Exception {
        List<Points.RetrievedPoint> results = new ArrayList<>();
        Points.PointId offset = null;

        while (true) {
            Points.ScrollPoints.Builder builder = Points.ScrollPoints.newBuilder()
                    .setCollectionName(qdrantProperties.getCollectionName())
                    .setLimit(SCROLL_BATCH_SIZE)
                    .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                    .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(false).build());

            if (filter != null) {
                builder.setFilter(filter);
            }
            if (offset != null) {
                builder.setOffset(offset);
            }

            Points.ScrollResponse response = qdrantClient.scrollAsync(builder.build(), QDRANT_TIMEOUT).get();
            results.addAll(response.getResultList());

            if (!response.hasNextPageOffset() || response.getResultCount() < SCROLL_BATCH_SIZE) {
                break;
            }
            offset = response.getNextPageOffset();
        }

        return results;
    }

    private Points.Filter buildAccessFilter(String fileName) {
        List<Points.Condition> mustConditions = new ArrayList<>();
        if (StringUtils.isNotBlank(fileName)) {
            mustConditions.add(ConditionFactory.matchKeyword("file_name", fileName));
        }

        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            return mustConditions.isEmpty() ? null : Points.Filter.newBuilder().addAllMust(mustConditions).build();
        }

        Set<Long> sanaScopeIds = securityHelper.getCurrentSanaScopeIdsForSensitiveOperation();
        if (sanaScopeIds == null || sanaScopeIds.isEmpty()) {
            mustConditions.add(ConditionFactory.matchKeyword("__never_match__", "__never_match__"));
            return Points.Filter.newBuilder().addAllMust(mustConditions).build();
        }

        Points.Filter.Builder scopeFilter = Points.Filter.newBuilder();
        sanaScopeIds.forEach(sanaId -> scopeFilter.addShould(ConditionFactory.match("owner_sana_id", sanaId)));
        mustConditions.add(ConditionFactory.filter(scopeFilter.build()));
        return Points.Filter.newBuilder().addAllMust(mustConditions).build();
    }

    private DocumentInfo retrievedPointToInfo(Points.RetrievedPoint point) {
        if (point == null) {
            return null;
        }

        try {
            Map<String, Object> metadata = point.getPayloadMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> toJavaValue(entry.getValue()),
                            (left, right) -> right,
                            LinkedHashMap::new
                    ));

            String fileName = Objects.toString(metadata.getOrDefault("file_name", ""), "");
            String fileType = Objects.toString(metadata.getOrDefault("file_type", ""), "");
            Integer chunkIndex = parseInteger(metadata.get("chunk_index"), 0);
            Integer totalChunks = parseInteger(metadata.get("total_chunks"), 1);
            String content = resolvePayloadContent(metadata);

            return new DocumentInfo(
                    pointIdToString(point.getId()),
                    fileName,
                    fileType,
                    chunkIndex,
                    totalChunks,
                    content,
                    (long) content.length(),
                    metadata
            );
        } catch (Exception e) {
            log.warn("转换 Qdrant point 到 DocumentInfo 失败: {}", e.getMessage());
            return null;
        }
    }

    private String resolvePayloadContent(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        String[] keys = {"doc_content", "document_content", "content", "text", "page_content", "chunk_content", "data"};
        for (String key : keys) {
            Object value = metadata.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private Integer parseInteger(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value);
        return StringUtils.isNumeric(text) ? Integer.parseInt(text) : defaultValue;
    }

    private String pointIdToString(Points.PointId pointId) {
        if (pointId == null) {
            return "";
        }
        if (pointId.hasUuid()) {
            return pointId.getUuid();
        }
        if (pointId.hasNum()) {
            return String.valueOf(pointId.getNum());
        }
        return "";
    }

    private Object toJavaValue(JsonWithInt.Value value) {
        if (value == null) {
            return null;
        }
        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case INTEGER_VALUE -> value.getIntegerValue();
            case DOUBLE_VALUE -> value.getDoubleValue();
            case BOOL_VALUE -> value.getBoolValue();
            case LIST_VALUE -> value.getListValue().getValuesList().stream()
                    .map(this::toJavaValue)
                    .collect(Collectors.toList());
            case STRUCT_VALUE -> value.getStructValue().getFieldsMap().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> toJavaValue(entry.getValue()),
                            (left, right) -> right,
                            LinkedHashMap::new
                    ));
            case NULL_VALUE, KIND_NOT_SET -> null;
        };
    }
}
