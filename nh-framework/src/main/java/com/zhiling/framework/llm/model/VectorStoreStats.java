package com.zhiling.framework.llm.model;

/**
 * 向量库统计信息。
 *
 * @author zhanghongyu
 */
public class VectorStoreStats {

    private long totalDocuments;
    private long totalChunks;
    private long collectionSize;
    private String collectionName;

    /**
     * 构造器：VectorStoreStats
     *
     * @author zhanghongyu
     */
    public VectorStoreStats() {
    }

    /**
     * 构造器：VectorStoreStats
     *
     * @author zhanghongyu
     */
    public VectorStoreStats(long totalDocuments, long totalChunks, long collectionSize, String collectionName) {
        this.totalDocuments = totalDocuments;
        this.totalChunks = totalChunks;
        this.collectionSize = collectionSize;
        this.collectionName = collectionName;
    }

    /**
     * 方法：getTotalDocuments
     *
     * @author zhanghongyu
     */
    public long getTotalDocuments() {
        return totalDocuments;
    }

    /**
     * 方法：setTotalDocuments
     *
     * @author zhanghongyu
     */
    public void setTotalDocuments(long totalDocuments) {
        this.totalDocuments = totalDocuments;
    }

    /**
     * 方法：getTotalChunks
     *
     * @author zhanghongyu
     */
    public long getTotalChunks() {
        return totalChunks;
    }

    /**
     * 方法：setTotalChunks
     *
     * @author zhanghongyu
     */
    public void setTotalChunks(long totalChunks) {
        this.totalChunks = totalChunks;
    }

    /**
     * 方法：getCollectionSize
     *
     * @author zhanghongyu
     */
    public long getCollectionSize() {
        return collectionSize;
    }

    /**
     * 方法：setCollectionSize
     *
     * @author zhanghongyu
     */
    public void setCollectionSize(long collectionSize) {
        this.collectionSize = collectionSize;
    }

    /**
     * 方法：getCollectionName
     *
     * @author zhanghongyu
     */
    public String getCollectionName() {
        return collectionName;
    }

    /**
     * 方法：setCollectionName
     *
     * @author zhanghongyu
     */
    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }
}