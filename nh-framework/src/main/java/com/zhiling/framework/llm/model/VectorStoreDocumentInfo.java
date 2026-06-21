package com.zhiling.framework.llm.model;

import java.util.Map;

/**
 * 向量库文档信息。
 *
 * @author zhanghongyu
 */
public class VectorStoreDocumentInfo {

    private String id;
    private String fileName;
    private String fileType;
    private Integer chunkIndex;
    private Integer totalChunks;
    private String content;
    private Long contentLength;
    private Map<String, Object> metadata;

    /**
     * 构造器：VectorStoreDocumentInfo
     *
     * @author zhanghongyu
     */
    public VectorStoreDocumentInfo() {
    }

    public VectorStoreDocumentInfo(String id, String fileName, String fileType, Integer chunkIndex,
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

    /**
     * 方法：getId
     *
     * @author zhanghongyu
     */
    public String getId() {
        return id;
    }

    /**
     * 方法：setId
     *
     * @author zhanghongyu
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 方法：getFileName
     *
     * @author zhanghongyu
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 方法：setFileName
     *
     * @author zhanghongyu
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 方法：getFileType
     *
     * @author zhanghongyu
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * 方法：setFileType
     *
     * @author zhanghongyu
     */
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    /**
     * 方法：getChunkIndex
     *
     * @author zhanghongyu
     */
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    /**
     * 方法：setChunkIndex
     *
     * @author zhanghongyu
     */
    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    /**
     * 方法：getTotalChunks
     *
     * @author zhanghongyu
     */
    public Integer getTotalChunks() {
        return totalChunks;
    }

    /**
     * 方法：setTotalChunks
     *
     * @author zhanghongyu
     */
    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    /**
     * 方法：getContent
     *
     * @author zhanghongyu
     */
    public String getContent() {
        return content;
    }

    /**
     * 方法：setContent
     *
     * @author zhanghongyu
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * 方法：getContentLength
     *
     * @author zhanghongyu
     */
    public Long getContentLength() {
        return contentLength;
    }

    /**
     * 方法：setContentLength
     *
     * @author zhanghongyu
     */
    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * 方法：getMetadata
     *
     * @author zhanghongyu
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 方法：setMetadata
     *
     * @author zhanghongyu
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}