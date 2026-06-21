package com.zhiling.agent.interfaces.framework;

import com.zhiling.framework.llm.model.AgentPromptEnhanceResult;
import com.zhiling.agent.application.AgentPromptEnhanceService;
import com.zhiling.framework.system.port.DocumentParsePort;
import com.zhiling.framework.system.port.MediaFilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Agent 文件增强提示词适配器。
 *
 * @author zhanghongyu
 */
@Component
@Slf4j
public class AgentPromptEnhanceServiceAdapter implements AgentPromptEnhanceService {

    private static final String DOCUMENT_CONTEXT_START = "=== 上传的文档内容 ===";
    private static final String DOCUMENT_CONTEXT_END = "=== 文档内容结束 ===";
    private static final String MEDIA_CONTEXT_START = "=== 上传的媒体文件 ===";
    private static final String MEDIA_CONTEXT_END = "=== 媒体文件结束 ===";

    private final MediaFilePort mediaFilePort;
    private final DocumentParsePort documentParsePort;

    public AgentPromptEnhanceServiceAdapter(MediaFilePort mediaFilePort,
                                            DocumentParsePort documentParsePort) {
        this.mediaFilePort = mediaFilePort;
        this.documentParsePort = documentParsePort;
    }

    /**
     * 方法：buildEnhancedPrompt
     *
     * @author zhanghongyu
     */
    @Override
    public AgentPromptEnhanceResult buildEnhancedPrompt(String userPrompt, List<MultipartFile> files) {
        String basePrompt = ensurePromptNotEmpty(userPrompt);
        if (files == null || files.isEmpty()) {
            return new AgentPromptEnhanceResult(basePrompt, basePrompt, false, false, List.of());
        }

        StringBuilder documentContext = new StringBuilder();
        StringBuilder mediaContext = new StringBuilder();
        List<MultipartFile> documentFiles = new ArrayList<>();
        List<MultipartFile> mediaFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                String contentType = file.getContentType();
                if (contentType != null && (contentType.startsWith("image/")
                        || contentType.startsWith("audio/")
                        || contentType.startsWith("video/"))) {
                    mediaFiles.add(file);
                }
                continue;
            }

            String ext = getFileExtension(fileName).toLowerCase(Locale.ROOT);
            if (List.of("pdf", "doc", "docx", "txt").contains(ext)) {
                documentFiles.add(file);
            } else if (List.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "mp3", "wav", "ogg", "mp4", "webm", "mov")
                    .contains(ext)) {
                mediaFiles.add(file);
            }
        }

        if (!documentFiles.isEmpty()) {
            documentContext.append("\n\n").append(DOCUMENT_CONTEXT_START).append("\n");

            for (MultipartFile file : documentFiles) {
                try {
                    String fileName = file.getOriginalFilename();
                    byte[] content = file.getBytes();

                    List<String> texts = documentParsePort.readDocumentTexts(fileName, content);
                    if (!texts.isEmpty()) {
                        StringBuilder fileContentBuilder = new StringBuilder();
                        for (String text : texts) {
                            fileContentBuilder.append(text);
                        }
                        String fileContent = fileContentBuilder.toString();

                        if (fileContent.length() > 8000) {
                            fileContent = fileContent.substring(0, 8000) + "\n...(内容过长已截断)";
                        }
                        documentContext.append(String.format("\n【文件：%s】\n%s\n", fileName, fileContent));
                    }
                } catch (Exception e) {
                    log.error("解析文档失败: {}", file.getOriginalFilename(), e);
                    documentContext.append(String.format("\n【文件：%s - 解析失败：%s】\n",
                            file.getOriginalFilename(), e.getMessage()));
                }
            }

            documentContext.append(DOCUMENT_CONTEXT_END).append("\n\n");
        }

        if (!mediaFiles.isEmpty()) {
            mediaContext.append("\n\n").append(MEDIA_CONTEXT_START).append("\n");
            for (MultipartFile file : mediaFiles) {
                String fileName = file.getOriginalFilename();
                if (fileName == null || fileName.trim().isEmpty()) {
                    continue;
                }
                String normalizedName = fileName.trim();
                String fileType = getFileExtension(normalizedName).toUpperCase(Locale.ROOT);
                String persistedUrl = "";
                try {
                    persistedUrl = mediaFilePort.upload(file);
                } catch (Exception ex) {
                    log.warn("媒体附件持久化失败，fileName={}", normalizedName, ex);
                }
                mediaContext.append(String.format("【文件：%s】\n", normalizedName));
                if (persistedUrl != null && !persistedUrl.isBlank()) {
                    mediaContext.append(String.format("【附件：%s|%s|%s】\n", normalizedName, fileType, persistedUrl));
                }
            }
            mediaContext.append(MEDIA_CONTEXT_END).append("\n\n");
        }

        String modelPrompt = basePrompt;
        String storagePrompt = basePrompt;

        if (!documentFiles.isEmpty()) {
            String documentOnlyContext = documentContext.toString();
            if (userPrompt == null || userPrompt.trim().isEmpty()
                    || userPrompt.equals("请帮我分析上传的文件内容，提供详细的解读和建议。")) {
                modelPrompt = String.format("请分析以下文档内容，提供详细的解读、摘要和建议。如果文档是表格或数据，请帮助分析数据含义。%s",
                        documentOnlyContext);
            } else {
                modelPrompt = String.format("%s\n\n用户问题：%s", documentOnlyContext, basePrompt);
            }
            storagePrompt = modelPrompt;
        }

        if (!mediaFiles.isEmpty()) {
            storagePrompt = buildMediaStoragePrompt(userPrompt, basePrompt, mediaContext.toString());
            modelPrompt = buildMediaModelPrompt(userPrompt, basePrompt);
        }

        return new AgentPromptEnhanceResult(modelPrompt, storagePrompt, !documentFiles.isEmpty(), !mediaFiles.isEmpty(), mediaFiles);
    }

    /**
     * 方法：ensurePromptNotEmpty
     *
     * @author zhanghongyu
     */
    @Override
    public String ensurePromptNotEmpty(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "你好";
        }
        return prompt;
    }

    /**
     * 方法：getFileExtension
     *
     * @author zhanghongyu
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * 方法：buildMediaStoragePrompt
     *
     * @author zhanghongyu
     */
    private String buildMediaStoragePrompt(String userPrompt, String basePrompt, String mediaContext) {
        if (userPrompt == null || userPrompt.trim().isEmpty()
                || userPrompt.equals("请帮我分析上传的图片/音视频内容。")) {
            return String.format("请分析以下媒体内容并回答用户问题。%s", mediaContext);
        }
        return String.format("%s\n\n用户问题：%s", mediaContext, basePrompt);
    }

    /**
     * 方法：buildMediaModelPrompt
     *
     * @author zhanghongyu
     */
    private String buildMediaModelPrompt(String userPrompt, String basePrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()
                || userPrompt.equals("请帮我分析上传的图片/音视频内容。")) {
            return "请直接基于我上传的媒体内容进行分析，优先描述可见主体、场景、文字信息、关键细节和可能用途；无法确认的部分请明确说明不确定性，但不要因为缺少系统数据或文档依据而拒绝分析媒体本身。";
        }
        return basePrompt;
    }
}

