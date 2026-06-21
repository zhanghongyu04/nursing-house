package com.zhiling.framework.llm.model;

import java.util.List;

/**
 * Agent 历史用户消息解析结果。
 *
 * @author zhanghongyu
 */
public class AgentHistoryUserPayload {

    private final String displayContent;
    private final List<AgentAttachment> attachments;

    /**
     * 构造器：AgentHistoryUserPayload
     *
     * @author zhanghongyu
     */
    public AgentHistoryUserPayload(String displayContent, List<AgentAttachment> attachments) {
        this.displayContent = displayContent;
        this.attachments = attachments == null ? List.of() : attachments;
    }

    /**
     * 方法：getDisplayContent
     *
     * @author zhanghongyu
     */
    public String getDisplayContent() {
        return displayContent;
    }

    /**
     * 方法：getAttachments
     *
     * @author zhanghongyu
     */
    public List<AgentAttachment> getAttachments() {
        return attachments;
    }
}