package com.zhiling.framework.llm.model;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Agent 提示词增强结果。
 *
 * @author zhanghongyu
 */
public class AgentPromptEnhanceResult {

    private final String modelPrompt;
    private final String storagePrompt;
    private final boolean hasDocumentFiles;
    private final boolean hasMediaFiles;
    private final List<MultipartFile> mediaFiles;

    public AgentPromptEnhanceResult(String modelPrompt,
                                    String storagePrompt,
                                    boolean hasDocumentFiles,
                                    boolean hasMediaFiles,
                                    List<MultipartFile> mediaFiles) {
        this.modelPrompt = modelPrompt;
        this.storagePrompt = storagePrompt;
        this.hasDocumentFiles = hasDocumentFiles;
        this.hasMediaFiles = hasMediaFiles;
        this.mediaFiles = mediaFiles == null ? List.of() : mediaFiles;
    }

    /**
     * 方法：getEnhancedPrompt
     *
     * @author zhanghongyu
     */
    public String getEnhancedPrompt() {
        return modelPrompt;
    }

    /**
     * 方法：getModelPrompt
     *
     * @author zhanghongyu
     */
    public String getModelPrompt() {
        return modelPrompt;
    }

    /**
     * 方法：getStoragePrompt
     *
     * @author zhanghongyu
     */
    public String getStoragePrompt() {
        return storagePrompt;
    }

    /**
     * 方法：isHasDocumentFiles
     *
     * @author zhanghongyu
     */
    public boolean isHasDocumentFiles() {
        return hasDocumentFiles;
    }

    /**
     * 方法：isHasMediaFiles
     *
     * @author zhanghongyu
     */
    public boolean isHasMediaFiles() {
        return hasMediaFiles;
    }

    /**
     * 方法：getMediaFiles
     *
     * @author zhanghongyu
     */
    public List<MultipartFile> getMediaFiles() {
        return mediaFiles;
    }
}