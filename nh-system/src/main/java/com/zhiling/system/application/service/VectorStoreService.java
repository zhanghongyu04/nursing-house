package com.zhiling.system.application.service;

import java.util.List;
import java.util.Map;

/**
 * 向量库管理服务接口
 * 提供文档的增删查统计功能
 *
 * @author zhanghongyu
 */
public interface VectorStoreService {

    /**
     * 获取向量库统计信息
     * @return 统计信息
     */
    VectorStoreStats getStats();

    /**
     * 获取文档列表
     * @param page 页码
     * @param pageSize 每页大小
     * @return 文档分页列表
     */
    DocumentListResult getDocuments(int page, int pageSize);

    /**
     * 根据文件名获取文档
     * @param fileName 文件名
     * @return 文档列表
     */
    List<DocumentInfo> getDocumentsByFileName(String fileName);

    /**
     * 删除指定文件名的所有文档
     * @param fileName 文件名
     * @return 删除的文档数量
     */
    int deleteDocumentsByFileName(String fileName);

    /**
     * 根据文档ID删除文档
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    boolean deleteDocument(String documentId);

    /**
     * 批量删除文档
     * @param documentIds 文档ID列表
     * @return 删除的文档数量
     */
    int deleteDocuments(List<String> documentIds);

    /**
     * 清空向量库
     * @return 清空的文档数量
     */
    int clearAll();

    /**
     * 向量库统计信息
     */
    class VectorStoreStats {
        private long totalDocuments;
        private long totalChunks;
        private long collectionSize;
        private String collectionName;

        /**
         * 方法：VectorStoreStats
         *
         * @author zhanghongyu
         */
        public VectorStoreStats(long totalDocuments, long totalChunks, long collectionSize, String collectionName) {
            this.totalDocuments = totalDocuments;
            this.totalChunks = totalChunks;
            this.collectionSize = collectionSize;
            this.collectionName = collectionName;
        }

        public long getTotalDocuments() { return totalDocuments; }
        public long getTotalChunks() { return totalChunks; }
        public long getCollectionSize() { return collectionSize; }
        public String getCollectionName() { return collectionName; }

        public void setTotalDocuments(long totalDocuments) { this.totalDocuments = totalDocuments; }
        public void setTotalChunks(long totalChunks) { this.totalChunks = totalChunks; }
        public void setCollectionSize(long collectionSize) { this.collectionSize = collectionSize; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    }

    /**
     * 文档信息
     */
    class DocumentInfo {
        private String id;
        private String fileName;
        private String fileType;
        private Integer chunkIndex;
        private Integer totalChunks;
        private String content;
        private Long contentLength;
        private Map<String, Object> metadata;

        public DocumentInfo() {}

        public DocumentInfo(String id, String fileName, String fileType, Integer chunkIndex,
                           Integer totalChunks, String content, Long contentLength, Map<String, Object> metadata) {
            this.id = id;
            this.fileName = fileName;
            this.fileType = fileType;
            this.chunkIndex = chunkIndex;
            this.totalChunks = totalChunks;
            this.content = content;
            this.contentLength = contentLength;
            this.metadata = metadata;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public Integer getChunkIndex() { return chunkIndex; }
        public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
        public Integer getTotalChunks() { return totalChunks; }
        public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getContentLength() { return contentLength; }
        public void setContentLength(Long contentLength) { this.contentLength = contentLength; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 文档列表结果
     */
    class DocumentListResult {
        private List<DocumentInfo> documents;
        private long total;
        private int page;
        private int pageSize;

        public DocumentListResult() {}

        /**
         * 方法：DocumentListResult
         *
         * @author zhanghongyu
         */
        public DocumentListResult(List<DocumentInfo> documents, long total, int page, int pageSize) {
            this.documents = documents;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<DocumentInfo> getDocuments() { return documents; }
        public void setDocuments(List<DocumentInfo> documents) { this.documents = documents; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    }
}
