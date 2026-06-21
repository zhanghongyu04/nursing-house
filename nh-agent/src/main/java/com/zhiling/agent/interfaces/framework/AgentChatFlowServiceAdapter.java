package com.zhiling.agent.interfaces.framework;

import com.zhiling.agent.infrastructure.support.InMemoryMultipartFile;
import com.zhiling.agent.application.AgentConversationService;
import com.zhiling.framework.llm.model.AgentPromptEnhanceResult;
import com.zhiling.framework.llm.model.AgentAttachment;
import com.zhiling.framework.llm.service.AgentChatFlowService;
import com.zhiling.agent.application.AgentChatService;
import com.zhiling.agent.application.AgentContentPolicyService;
import com.zhiling.agent.application.AgentPromptEnhanceService;
import com.zhiling.agent.application.AgentRagService;
import com.zhiling.framework.llm.service.AgentSessionHistoryService;
import com.zhiling.framework.system.port.MediaFilePort;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Agent 聊天流程编排适配器。
 *
 * @author zhanghongyu
 */
@Component
@Slf4j
public class AgentChatFlowServiceAdapter implements AgentChatFlowService {

    private final AgentSessionHistoryService agentSessionHistoryService;
    private final AgentContentPolicyService agentContentPolicyService;
    private final AgentPromptEnhanceService agentPromptEnhanceService;
    private final AgentRagService agentRagService;
    private final AgentChatService agentChatService;
    private final AgentConversationService agentConversationService;
    private final MediaFilePort mediaFilePort;

    public AgentChatFlowServiceAdapter(AgentSessionHistoryService agentSessionHistoryService,
                                       AgentContentPolicyService agentContentPolicyService,
                                       AgentPromptEnhanceService agentPromptEnhanceService,
                                       AgentRagService agentRagService,
                                       AgentChatService agentChatService,
                                       AgentConversationService agentConversationService,
                                       MediaFilePort mediaFilePort) {
        this.agentSessionHistoryService = agentSessionHistoryService;
        this.agentContentPolicyService = agentContentPolicyService;
        this.agentPromptEnhanceService = agentPromptEnhanceService;
        this.agentRagService = agentRagService;
        this.agentChatService = agentChatService;
        this.agentConversationService = agentConversationService;
        this.mediaFilePort = mediaFilePort;
    }

    /**
     * 方法：chat
     *
     * @author zhanghongyu
     */
    @Override
    public Flux<String> chat(String prompt, String chatId, List<MultipartFile> files, HttpServletResponse response) {
        if (chatId != null && !chatId.trim().isEmpty()) {
            agentSessionHistoryService.ensureConversationAccessible(chatId, null);
            agentSessionHistoryService.saveConversationReference(chatId);
        }

        Optional<String> preInterceptReply = agentContentPolicyService.applyInputPreIntercept(prompt);
        if (preInterceptReply.isPresent()) {
            String safeReply = preInterceptReply.get();
            if (chatId != null && !chatId.trim().isEmpty()) {
                agentSessionHistoryService.saveInterceptExchange(
                        chatId,
                        agentPromptEnhanceService.ensurePromptNotEmpty(prompt),
                        safeReply
                );
            }
            agentRagService.writeRagHeaders(response, agentRagService.emptySummary());
            return Flux.just(safeReply);
        }

        AgentPromptEnhanceResult promptEnhanceResult = agentPromptEnhanceService.buildEnhancedPrompt(prompt, files);
        String modelPrompt = promptEnhanceResult.getModelPrompt();
        String storagePrompt = promptEnhanceResult.getStoragePrompt();
        boolean hasDocumentFiles = promptEnhanceResult.isHasDocumentFiles();
        boolean hasMediaFiles = promptEnhanceResult.isHasMediaFiles();
        int uploadFileCount = files == null ? 0 : files.size();

        log.info("AgentChatFlow 路由判定: chatId={}, uploadFileCount={}, hasDocumentFiles={}, hasMediaFiles={}, promptPreview={}",
                chatId,
                uploadFileCount,
                hasDocumentFiles,
                hasMediaFiles,
                previewPrompt(prompt));
        log.info("AgentChatFlow 模型提示词预览: chatId={}, modelPromptPreview={}",
                chatId,
                previewPrompt(modelPrompt));
        log.info("AgentChatFlow 存储提示词预览: chatId={}, storagePromptPreview={}",
                chatId,
                previewPrompt(storagePrompt));

        if (hasDocumentFiles) {
            log.info("AgentChatFlow 路由到文本链路(文档解析): chatId={}", chatId);
            return agentChatService.textChat(modelPrompt, chatId, response);
        }
        if (hasMediaFiles) {
            agentRagService.writeRagHeaders(response, agentRagService.emptySummary());
            log.info("AgentChatFlow 路由到多模态链路(本轮上传媒体): chatId={}, mediaCount={}",
                    chatId,
                    promptEnhanceResult.getMediaFiles() == null ? 0 : promptEnhanceResult.getMediaFiles().size());
            return agentChatService.multiModalChat(modelPrompt, storagePrompt, chatId, promptEnhanceResult.getMediaFiles(), response);
        }
        if (shouldReuseRecentMedia(chatId, prompt)) {
            List<MultipartFile> recentMediaFiles = loadRecentMediaFiles(chatId);
            if (!recentMediaFiles.isEmpty()) {
                String replayPrompt = agentPromptEnhanceService.ensurePromptNotEmpty(prompt);
                agentRagService.writeRagHeaders(response, agentRagService.emptySummary());
                log.info("复用历史多模态附件，chatId={}, mediaCount={}", chatId, recentMediaFiles.size());
                return agentChatService.multiModalChat(replayPrompt, replayPrompt, chatId, recentMediaFiles, response);
            }
            log.warn("检测到图片追问但未找到可复用历史媒体，回退文本链路: chatId={}, promptPreview={}",
                    chatId,
                    previewPrompt(prompt));
        }
        log.info("AgentChatFlow 路由到文本链路(普通对话): chatId={}", chatId);
        return agentChatService.textChat(modelPrompt, chatId, response);
    }

    /**
     * 方法：shouldReuseRecentMedia
     *
     * @author zhanghongyu
     */
    private boolean shouldReuseRecentMedia(String chatId, String prompt) {
        if (chatId == null || chatId.trim().isEmpty() || prompt == null || prompt.trim().isEmpty()) {
            return false;
        }
        String normalized = prompt.toLowerCase(Locale.ROOT);
        return normalized.contains("图片")
                || normalized.contains("这张图")
                || normalized.contains("那张图")
                || normalized.contains("图里")
                || normalized.contains("截图")
                || normalized.contains("照片")
                || normalized.contains("重新分析")
                || normalized.contains("再讲")
                || normalized.contains("再说")
                || normalized.contains("继续分析");
    }

    /**
     * 方法：loadRecentMediaFiles
     *
     * @author zhanghongyu
     */
    private List<MultipartFile> loadRecentMediaFiles(String chatId) {
        List<AgentAttachment> attachments = agentConversationService.findRecentMediaAttachments(chatId, 12);
        if (attachments == null || attachments.isEmpty()) {
            log.info("未检索到历史媒体附件: chatId={}", chatId);
            return List.of();
        }

        log.info("检索到历史媒体附件: chatId={}, attachmentCount={}, fileNames={}",
                chatId,
                attachments.size(),
                attachments.stream().map(AgentAttachment::getFileName).toList());

        return attachments.stream()
                .map(this::toMultipartFile)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * 方法：toMultipartFile
     *
     * @author zhanghongyu
     */
    private MultipartFile toMultipartFile(AgentAttachment attachment) {
        if (attachment == null || attachment.getFileUrl() == null || attachment.getFileUrl().isBlank()) {
            log.warn("历史媒体附件缺少 URL，无法复用: fileName={}", attachment == null ? null : attachment.getFileName());
            return null;
        }
        byte[] bytes = mediaFilePort.downloadAsBytesByUrl(attachment.getFileUrl());
        if (bytes == null || bytes.length == 0) {
            log.warn("复用历史媒体附件失败，下载内容为空: fileName={}, url={}",
                    attachment.getFileName(), attachment.getFileUrl());
            return null;
        }
        String fileName = attachment.getFileName() == null || attachment.getFileName().isBlank()
                ? "media"
                : attachment.getFileName();
        String contentType = inferContentType(attachment);
        log.info("复用历史媒体附件成功: fileName={}, contentType={}, size={}",
                fileName, contentType, bytes.length);
        return new InMemoryMultipartFile("files", fileName, contentType, bytes);
    }

    /**
     * 方法：inferContentType
     *
     * @author zhanghongyu
     */
    private String inferContentType(AgentAttachment attachment) {
        String type = attachment.getFileType();
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "bmp" -> "image/bmp";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mov" -> "video/quicktime";
            default -> "application/octet-stream";
        };
    }

    /**
     * 方法：previewPrompt
     *
     * @author zhanghongyu
     */
    private String previewPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "";
        }
        String normalized = prompt.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 60 ? normalized : normalized.substring(0, 60) + "...";
    }
}