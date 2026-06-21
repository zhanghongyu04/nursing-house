package com.zhiling.framework.llm.model;

/**
 * Agent 历史附件模型。
 *
 * @author zhanghongyu
 */
public class AgentAttachment {

    private final String fileName;
    private final String fileType;
    private final String fileUrl;

    /**
     * 构造器：AgentAttachment
     *
     * @author zhanghongyu
     */
    public AgentAttachment(String fileName, String fileType, String fileUrl) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileUrl = fileUrl;
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
     * 方法：getFileType
     *
     * @author zhanghongyu
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * 方法：getFileUrl
     *
     * @author zhanghongyu
     */
    public String getFileUrl() {
        return fileUrl;
    }
}