package com.zhiling.framework.llm.model;

import java.util.List;

/**
 * 向量库文档分页结果。
 *
 * @author zhanghongyu
 */
public class VectorStoreDocumentListResult {

    private List<VectorStoreDocumentInfo> documents;
    private long total;
    private int page;
    private int pageSize;

    /**
     * 构造器：VectorStoreDocumentListResult
     *
     * @author zhanghongyu
     */
    public VectorStoreDocumentListResult() {
    }

    /**
     * 构造器：VectorStoreDocumentListResult
     *
     * @author zhanghongyu
     */
    public VectorStoreDocumentListResult(List<VectorStoreDocumentInfo> documents, long total, int page, int pageSize) {
        this.documents = documents;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * 方法：getDocuments
     *
     * @author zhanghongyu
     */
    public List<VectorStoreDocumentInfo> getDocuments() {
        return documents;
    }

    /**
     * 方法：setDocuments
     *
     * @author zhanghongyu
     */
    public void setDocuments(List<VectorStoreDocumentInfo> documents) {
        this.documents = documents;
    }

    /**
     * 方法：getTotal
     *
     * @author zhanghongyu
     */
    public long getTotal() {
        return total;
    }

    /**
     * 方法：setTotal
     *
     * @author zhanghongyu
     */
    public void setTotal(long total) {
        this.total = total;
    }

    /**
     * 方法：getPage
     *
     * @author zhanghongyu
     */
    public int getPage() {
        return page;
    }

    /**
     * 方法：setPage
     *
     * @author zhanghongyu
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * 方法：getPageSize
     *
     * @author zhanghongyu
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 方法：setPageSize
     *
     * @author zhanghongyu
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}