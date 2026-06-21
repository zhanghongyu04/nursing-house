package com.zhiling.system.interfaces.framework;

import com.zhiling.common.constant.RoleConstant;
import com.zhiling.framework.llm.model.VectorStoreDocumentInfo;
import com.zhiling.framework.llm.model.VectorStoreDocumentListResult;
import com.zhiling.framework.llm.model.VectorStoreStats;
import com.zhiling.framework.llm.model.VectorStoreSupportedFileTypes;
import com.zhiling.framework.llm.service.VectorStoreManageService;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.system.application.service.DocumentReaderService;
import com.zhiling.system.application.service.VectorStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 向量库管理服务适配器。
 *
 * @author zhanghongyu
 */
@Component
@Slf4j
public class VectorStoreManageServiceAdapter implements VectorStoreManageService {

    private static final int EMBEDDING_BATCH_SIZE = 10;
    private static final int MIN_SPLIT_TEXT_LENGTH = 200;
    private static final String VECTOR_STATS_PATH = "/web/vector-store/stats";
    private static final String VECTOR_DOCUMENTS_PATH = "/web/vector-store/documents";
    private static final String VECTOR_DOCUMENTS_OPERATION_PATH = "/web/vector-store/documents/**";
    private static final String VECTOR_UPLOAD_PATH = "/web/vector-store/upload";
    private static final String VECTOR_UPLOAD_BATCH_PATH = "/web/vector-store/upload/batch";
    private static final String VECTOR_SUPPORTED_TYPES_PATH = "/web/vector-store/supported-types";
    private static final String VECTOR_CLEAR_PATH = "/web/vector-store/clear";

    private final VectorStoreService vectorStoreService;
    private final DocumentReaderService documentReaderService;
    private final VectorStore vectorStore;
    private final SecurityHelper securityHelper;

    public VectorStoreManageServiceAdapter(VectorStoreService vectorStoreService,
                                           DocumentReaderService documentReaderService,
                                           VectorStore vectorStore,
                                           SecurityHelper securityHelper) {
        this.vectorStoreService = vectorStoreService;
        this.documentReaderService = documentReaderService;
        this.vectorStore = vectorStore;
        this.securityHelper = securityHelper;
    }

    /**
     * 方法：getStats
     *
     * @author zhanghongyu
     */
    @Override
    public VectorStoreStats getStats() {
        assertResourcePermission(VECTOR_STATS_PATH, "当前账号无知识库统计权限");
        VectorStoreService.VectorStoreStats stats = vectorStoreService.getStats();
        return new VectorStoreStats(
                stats.getTotalDocuments(),
                stats.getTotalChunks(),
                stats.getCollectionSize(),
                stats.getCollectionName()
        );
    }

    /**
     * 方法：getDocuments
     *
     * @author zhanghongyu
     */
    @Override
    public VectorStoreDocumentListResult getDocuments(int page, int pageSize) {
        assertResourcePermission(VECTOR_DOCUMENTS_PATH, "当前账号无知识库文档查询权限");
        VectorStoreService.DocumentListResult result = vectorStoreService.getDocuments(page, pageSize);
        List<VectorStoreDocumentInfo> documents = result.getDocuments() == null ? List.of() : result.getDocuments().stream()
                .map(this::toFrameworkDocumentInfo)
                .toList();
        return new VectorStoreDocumentListResult(documents, result.getTotal(), result.getPage(), result.getPageSize());
    }

    /**
     * 方法：getDocumentsByFileName
     *
     * @author zhanghongyu
     */
    @Override
    public List<VectorStoreDocumentInfo> getDocumentsByFileName(String fileName) {
        assertResourcePermission(VECTOR_DOCUMENTS_OPERATION_PATH, "当前账号无知识库文档详情权限");
        return vectorStoreService.getDocumentsByFileName(fileName).stream()
                .map(this::toFrameworkDocumentInfo)
                .toList();
    }

    /**
     * 方法：deleteDocumentsByFileName
     *
     * @author zhanghongyu
     */
    @Override
    public int deleteDocumentsByFileName(String fileName) {
        assertResourcePermission(VECTOR_DOCUMENTS_OPERATION_PATH, "当前账号无知识库文档删除权限");
        return vectorStoreService.deleteDocumentsByFileName(fileName);
    }

    /**
     * 方法：deleteDocument
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteDocument(String documentId) {
        assertResourcePermission(VECTOR_DOCUMENTS_OPERATION_PATH, "当前账号无知识库文档删除权限");
        return vectorStoreService.deleteDocument(documentId);
    }

    /**
     * 方法：deleteDocuments
     *
     * @author zhanghongyu
     */
    @Override
    public int deleteDocuments(List<String> documentIds) {
        assertResourcePermission(VECTOR_DOCUMENTS_OPERATION_PATH, "当前账号无知识库文档删除权限");
        return vectorStoreService.deleteDocuments(documentIds);
    }

    /**
     * 方法：clearAll
     *
     * @author zhanghongyu
     */
    @Override
    public int clearAll() {
        assertResourcePermission(VECTOR_CLEAR_PATH, "当前账号无知识库清空权限");
        return vectorStoreService.clearAll();
    }

    /**
     * 方法：uploadDocument
     *
     * @author zhanghongyu
     */
    @Override
    public String uploadDocument(MultipartFile file, Long sanaId) {
        assertResourcePermission(VECTOR_UPLOAD_PATH, "当前账号无知识库上传权限");
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (!documentReaderService.isSupportedFileType(fileName)) {
            throw new IllegalArgumentException("不支持的文件类型。支持的格式：PDF、Word (.doc/.docx)、TXT");
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }

        try {
            byte[] content = file.getBytes();
            List<Document> documents = documentReaderService.readDocument(fileName, content);
            List<Document> validDocuments = documents.stream()
                    .filter(d -> d.getText() != null && !d.getText().isBlank())
                    .toList();
            if (validDocuments.isEmpty()) {
                throw new IllegalArgumentException("无法从文件中提取有效内容，请确认文件不是扫描图片或受保护文档。");
            }
            List<Document> normalizedDocuments = sanitizeDocumentsBeforeEmbedding(validDocuments);
            Long targetSanaId = resolveUploadSanaId(sanaId);
            enrichAclMetadata(normalizedDocuments, targetSanaId);
            addDocumentsInBatches(normalizedDocuments);

            int totalChars = normalizedDocuments.stream().mapToInt(d -> d.getText().length()).sum();
            String fileInfo = String.format("文件名：%s，类型：%s，大小：%s，提取字符数：%d，分块数：%d",
                    fileName,
                    getFileExtension(fileName).toUpperCase(),
                    formatFileSize(file.getSize()),
                    totalChars,
                    normalizedDocuments.size());
            log.info("向量库文档上传成功: {}", fileInfo);
            return "上传并索引成功！" + fileInfo;
        } catch (Exception e) {
            log.error("向量库文档上传失败: {}", fileName, e);
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 方法：uploadDocuments
     *
     * @author zhanghongyu
     */
    @Override
    public String uploadDocuments(List<MultipartFile> files, Long sanaId) {
        assertResourcePermission(VECTOR_UPLOAD_BATCH_PATH, "当前账号无知识库批量上传权限");
        Long targetSanaId = resolveUploadSanaId(sanaId);
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }

        int successCount = 0;
        int failCount = 0;
        StringBuilder resultMessage = new StringBuilder();
        for (MultipartFile file : files) {
            try {
                String fileName = file.getOriginalFilename();
                if (fileName == null || !documentReaderService.isSupportedFileType(fileName)) {
                    failCount++;
                    resultMessage.append(String.format("【跳过】%s：不支持的文件类型\n", fileName));
                    continue;
                }

                byte[] content = file.getBytes();
                List<Document> documents = documentReaderService.readDocument(fileName, content);
                List<Document> validDocuments = documents.stream()
                        .filter(d -> d.getText() != null && !d.getText().isBlank())
                        .toList();
                if (!validDocuments.isEmpty()) {
                    List<Document> normalizedDocuments = sanitizeDocumentsBeforeEmbedding(validDocuments);
                    enrichAclMetadata(normalizedDocuments, targetSanaId);
                    addDocumentsInBatches(normalizedDocuments);
                    successCount++;
                    int totalChars = normalizedDocuments.stream().mapToInt(d -> d.getText().length()).sum();
                    resultMessage.append(String.format("【成功】%s：%d 字符（%d 分块）\n", fileName, totalChars, normalizedDocuments.size()));
                } else {
                    failCount++;
                    resultMessage.append(String.format("【失败】%s：无法提取内容\n", fileName));
                }
            } catch (Exception e) {
                failCount++;
                log.error("向量库批量上传处理失败: {}", file.getOriginalFilename(), e);
                resultMessage.append(String.format("【错误】%s：%s\n", file.getOriginalFilename(), e.getMessage()));
            }
        }

        String summary = String.format("批量上传完成！成功：%d，失败：%d", successCount, failCount);
        log.info(summary);
        return summary + "\n" + resultMessage;
    }

    /**
     * 方法：getSupportedFileTypes
     *
     * @author zhanghongyu
     */
    @Override
    public VectorStoreSupportedFileTypes getSupportedFileTypes() {
        assertResourcePermission(VECTOR_SUPPORTED_TYPES_PATH, "当前账号无知识库支持类型查询权限");
        return new VectorStoreSupportedFileTypes(
                documentReaderService.getSupportedFileTypes(),
                documentReaderService.getSupportedMimeTypes()
        );
    }

    /**
     * 方法：toFrameworkDocumentInfo
     *
     * @author zhanghongyu
     */
    private VectorStoreDocumentInfo toFrameworkDocumentInfo(VectorStoreService.DocumentInfo info) {
        return new VectorStoreDocumentInfo(
                info.getId(),
                info.getFileName(),
                info.getFileType(),
                info.getChunkIndex(),
                info.getTotalChunks(),
                info.getContent(),
                info.getContentLength(),
                info.getMetadata()
        );
    }

    /**
     * 方法：getFileExtension
     *
     * @author zhanghongyu
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 方法：formatFileSize
     *
     * @author zhanghongyu
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    /**
     * 方法：addDocumentsInBatches
     *
     * @author zhanghongyu
     */
    private void addDocumentsInBatches(List<Document> documents) {
        for (int i = 0; i < documents.size(); i += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(i + EMBEDDING_BATCH_SIZE, documents.size());
            List<Document> batch = documents.subList(i, end);
            try {
                vectorStore.add(batch);
            } catch (RuntimeException e) {
                if (!isTokenLimitExceeded(e)) {
                    throw e;
                }
                log.warn("批量向量化命中单文档 token 上限，降级逐条处理。batchSize={}", batch.size());
                for (Document document : batch) {
                    addDocumentWithAutoSplit(document);
                }
            }
        }
    }

    /**
     * 方法：addDocumentWithAutoSplit
     *
     * @author zhanghongyu
     */
    private void addDocumentWithAutoSplit(Document document) {
        Deque<Document> queue = new ArrayDeque<>();
        queue.add(document);
        while (!queue.isEmpty()) {
            Document current = queue.removeFirst();
            try {
                vectorStore.add(List.of(current));
            } catch (RuntimeException e) {
                if (!isTokenLimitExceeded(e)) {
                    throw e;
                }
                String text = current.getText();
                if (text == null || text.length() < MIN_SPLIT_TEXT_LENGTH * 2) {
                    throw e;
                }
                List<Document> splitDocs = splitDocument(current);
                if (splitDocs.size() < 2) {
                    throw e;
                }
                queue.addFirst(splitDocs.get(1));
                queue.addFirst(splitDocs.get(0));
            }
        }
    }

    /**
     * 方法：splitDocument
     *
     * @author zhanghongyu
     */
    private List<Document> splitDocument(Document source) {
        String text = source.getText();
        if (text == null || text.length() < MIN_SPLIT_TEXT_LENGTH * 2) {
            return List.of();
        }
        int splitPoint = findSplitPoint(text);
        if (splitPoint <= 0 || splitPoint >= text.length()) {
            splitPoint = text.length() / 2;
        }
        String leftText = normalizeTextForEmbedding(text.substring(0, splitPoint).trim());
        String rightText = normalizeTextForEmbedding(text.substring(splitPoint).trim());
        if (leftText.isEmpty() || rightText.isEmpty()) {
            return List.of();
        }

        Document left = new Document(leftText);
        left.getMetadata().putAll(source.getMetadata());
        left.getMetadata().put("split_part", "1");

        Document right = new Document(rightText);
        right.getMetadata().putAll(source.getMetadata());
        right.getMetadata().put("split_part", "2");
        return List.of(left, right);
    }

    /**
     * 方法：findSplitPoint
     *
     * @author zhanghongyu
     */
    private int findSplitPoint(String text) {
        int mid = text.length() / 2;
        int maxOffset = Math.min(mid, 1000);
        for (int offset = 0; offset <= maxOffset; offset++) {
            int left = mid - offset;
            int right = mid + offset;
            if (left > 0 && isSplitBoundary(text.charAt(left))) {
                return left + 1;
            }
            if (right < text.length() - 1 && isSplitBoundary(text.charAt(right))) {
                return right + 1;
            }
        }
        return mid;
    }

    /**
     * 方法：isSplitBoundary
     *
     * @author zhanghongyu
     */
    private boolean isSplitBoundary(char c) {
        return c == '。' || c == '！' || c == '？' || c == '；'
                || c == '.' || c == '!' || c == '?' || c == ';'
                || c == '\n' || c == '，' || c == ',';
    }

    /**
     * 方法：isTokenLimitExceeded
     *
     * @author zhanghongyu
     */
    private boolean isTokenLimitExceeded(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("Tokens in a single document exceeds")
                    || message.contains("maximum number of allowed input tokens"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 方法：sanitizeDocumentsBeforeEmbedding
     *
     * @author zhanghongyu
     */
    private List<Document> sanitizeDocumentsBeforeEmbedding(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<Document> normalizedDocuments = new ArrayList<>(documents.size());
        for (Document document : documents) {
            String normalized = normalizeTextForEmbedding(document.getText());
            if (normalized.isBlank()) {
                continue;
            }
            Document normalizedDoc = new Document(normalized);
            normalizedDoc.getMetadata().putAll(document.getMetadata());
            normalizedDoc.getMetadata().put("doc_content", normalized);
            normalizedDocuments.add(normalizedDoc);
        }
        return normalizedDocuments;
    }

    /**
     * 方法：normalizeTextForEmbedding
     *
     * @author zhanghongyu
     */
    private String normalizeTextForEmbedding(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ')
                .replace('\u2002', ' ')
                .replace('\u2003', ' ')
                .replace('\u2009', ' ')
                .replace('\u3000', ' ');
        String[] lines = normalized.split("\\n", -1);
        StringBuilder builder = new StringBuilder();
        int blankLineCount = 0;
        for (String rawLine : lines) {
            String line = rawLine.replaceAll("[\\t\\f\\x0B ]{2,}", " ").trim();
            if (line.isEmpty()) {
                blankLineCount++;
                if (blankLineCount > 1) {
                    continue;
                }
            } else {
                blankLineCount = 0;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString().trim();
    }

    /**
     * 方法：assertResourcePermission
     *
     * @author zhanghongyu
     */
    private void assertResourcePermission(String resourcePath, String message) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            throw new AccessDeniedException(message);
        }
    }

    /**
     * 方法：isGovAdmin
     *
     * @author zhanghongyu
     */
    private boolean isGovAdmin() {
        return securityHelper.hasGovAdminRoleForSensitiveOperation();
    }

    /**
     * 方法：enrichAclMetadata
     *
     * @author zhanghongyu
     */
    private void enrichAclMetadata(List<Document> documents, Long targetSanaId) {
        Long userId = securityHelper.getCurrentUserId();
        Set<Long> sanaScopeIds = securityHelper.getCurrentSanaScopeIdsForSensitiveOperation();
        Long sanaId = targetSanaId;
        String role = isGovAdmin() ? RoleConstant.GOV_ADMIN : RoleConstant.ORG_ADMIN;
        String visibility = isGovAdmin() ? "PUBLIC" : "ORG";
        log.info("[向量库上传] 用户ID: {}, sanaId: {}, sanaScopeIds: {}, 角色: {}, 可见性: {}, 文档数: {}",
                userId, sanaId, sanaScopeIds, role, visibility, documents.size());
        for (Document document : documents) {
            Map<String, Object> metadata = document.getMetadata();
            metadata.put("visibility", visibility);
            metadata.put("owner_user_id", userId == null ? "" : String.valueOf(userId));
            metadata.put("owner_sana_id", sanaId == null ? "" : String.valueOf(sanaId));
            metadata.put("created_by_role", role);
            metadata.put("acl_version", "v1");
        }
    }

    /**
     * 方法：resolveUploadSanaId
     *
     * @author zhanghongyu
     */
    private Long resolveUploadSanaId(Long requestSanaId) {
        if (isGovAdmin()) {
            return requestSanaId;
        }
        Set<Long> sanaScopeIds = securityHelper.getCurrentSanaScopeIdsForSensitiveOperation();
        if (sanaScopeIds == null || sanaScopeIds.isEmpty()) {
            throw new AccessDeniedException("当前用户未绑定机构");
        }
        if (requestSanaId != null) {
            if (!sanaScopeIds.contains(requestSanaId)) {
                throw new AccessDeniedException("仅可上传到授权机构");
            }
            return requestSanaId;
        }
        if (sanaScopeIds.size() == 1) {
            return sanaScopeIds.iterator().next();
        }
        throw new AccessDeniedException("当前账号已授权多个机构，请上传时指定 sanaId");
    }
}
