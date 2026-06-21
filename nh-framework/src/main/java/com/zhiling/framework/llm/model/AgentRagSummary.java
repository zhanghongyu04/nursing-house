package com.zhiling.framework.llm.model;

import java.util.List;

/**
 * RAG 命中汇总。
 *
 * @author zhanghongyu
 */
public class AgentRagSummary {

    private final boolean hit;
    private final int hitCount;
    private final List<AgentRagSource> sources;

    /**
     * 构造器：AgentRagSummary
     *
     * @author zhanghongyu
     */
    public AgentRagSummary(List<AgentRagSource> sources) {
        this.sources = sources == null ? List.of() : sources;
        this.hitCount = this.sources.size();
        this.hit = this.hitCount > 0;
    }

    /**
     * 方法：empty
     *
     * @author zhanghongyu
     */
    public static AgentRagSummary empty() {
        return new AgentRagSummary(List.of());
    }

    /**
     * 方法：isHit
     *
     * @author zhanghongyu
     */
    public boolean isHit() {
        return hit;
    }

    /**
     * 方法：getHitCount
     *
     * @author zhanghongyu
     */
    public int getHitCount() {
        return hitCount;
    }

    /**
     * 方法：getSources
     *
     * @author zhanghongyu
     */
    public List<AgentRagSource> getSources() {
        return sources;
    }
}