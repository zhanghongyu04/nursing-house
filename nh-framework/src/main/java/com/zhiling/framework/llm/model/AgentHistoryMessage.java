package com.zhiling.framework.llm.model;

import java.util.List;

/**
 * Agent 历史消息模型。
 *
 * @author zhanghongyu
 */
public class AgentHistoryMessage {

    private final String role;
    private final String content;
    private final List<AgentAttachment> attachments;

    /**
     * 构造器：AgentHistoryMessage
     *
     * @author zhanghongyu
     */
    public AgentHistoryMessage(String role, String content, List<AgentAttachment> attachments) {
        this.role = role;
        this.content = content;
        this.attachments = attachments == null ? List.of() : attachments;
    }

    /**
     * 方法：getRole
     *
     * @author zhanghongyu
     */
    public String getRole() {
        return role;
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
     * 方法：getAttachments
     *
     * @author zhanghongyu
     */
    public List<AgentAttachment> getAttachments() {
        return attachments;
    }
}