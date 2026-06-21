package com.zhiling.framework.llm.model;

import java.util.List;

/**
 * Agent 历史消息展示模型。
 *
 * @author zhanghongyu
 */
public class AgentHistoryView {

    private String role;
    private String content;
    private List<AttachmentView> attachments;

    /**
     * 构造器：AgentHistoryView
     *
     * @author zhanghongyu
     */
    public AgentHistoryView() {
    }

    /**
     * 构造器：AgentHistoryView
     *
     * @author zhanghongyu
     */
    public AgentHistoryView(String role, String content, List<AttachmentView> attachments) {
        this.role = role;
        this.content = content;
        this.attachments = attachments;
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
     * 方法：setRole
     *
     * @author zhanghongyu
     */
    public void setRole(String role) {
        this.role = role;
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
     * 方法：getAttachments
     *
     * @author zhanghongyu
     */
    public List<AttachmentView> getAttachments() {
        return attachments;
    }

    /**
     * 方法：setAttachments
     *
     * @author zhanghongyu
     */
    public void setAttachments(List<AttachmentView> attachments) {
        this.attachments = attachments;
    }

    /**
     * 附件展示模型。
     */
    public static class AttachmentView {
        private String name;
        private String fileType;
        private String url;

        /**
         * 方法：AttachmentView
         *
         * @author zhanghongyu
         */
        public AttachmentView() {
        }

        /**
         * 方法：AttachmentView
         *
         * @author zhanghongyu
         */
        public AttachmentView(String name, String fileType, String url) {
            this.name = name;
            this.fileType = fileType;
            this.url = url;
        }

        /**
         * 方法：getName
         *
         * @author zhanghongyu
         */
        public String getName() {
            return name;
        }

        /**
         * 方法：setName
         *
         * @author zhanghongyu
         */
        public void setName(String name) {
            this.name = name;
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
         * 方法：getUrl
         *
         * @author zhanghongyu
         */
        public String getUrl() {
            return url;
        }

        /**
         * 方法：setUrl
         *
         * @author zhanghongyu
         */
        public void setUrl(String url) {
            this.url = url;
        }
    }
}