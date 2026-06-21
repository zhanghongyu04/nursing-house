package com.zhiling.framework.llm.model;

/**
 * RAG 命中来源项。
 *
 * @author zhanghongyu
 */
public class AgentRagSource {

    private final String fileName;
    private final Integer chunkIndex;
    private final Double score;
    private final String snippet;

    /**
     * 构造器：AgentRagSource
     *
     * @author zhanghongyu
     */
    public AgentRagSource(String fileName, Integer chunkIndex, Double score, String snippet) {
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.score = score;
        this.snippet = snippet;
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
     * 方法：getChunkIndex
     *
     * @author zhanghongyu
     */
    public Integer getChunkIndex() {
        return chunkIndex;
    }

    /**
     * 方法：getScore
     *
     * @author zhanghongyu
     */
    public Double getScore() {
        return score;
    }

    /**
     * 方法：getSnippet
     *
     * @author zhanghongyu
     */
    public String getSnippet() {
        return snippet;
    }
}