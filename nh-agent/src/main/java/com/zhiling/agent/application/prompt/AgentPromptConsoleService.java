package com.zhiling.agent.application.prompt;

import cn.hutool.core.util.StrUtil;
import com.zhiling.agent.application.prompt.model.PromptTemplateActiveView;
import com.zhiling.agent.application.prompt.model.PromptTemplateCacheView;
import com.zhiling.agent.application.prompt.model.PromptTemplateChangeLogCommand;
import com.zhiling.agent.application.prompt.model.PromptTemplateCreateSegmentRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplateCreateVersionRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplateDetail;
import com.zhiling.agent.application.prompt.model.PromptTemplateLogView;
import com.zhiling.agent.application.prompt.model.PromptTemplatePreviewRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplatePreviewView;
import com.zhiling.agent.application.prompt.model.PromptTemplateSegment;
import com.zhiling.agent.application.prompt.model.PromptTemplateSegmentEdit;
import com.zhiling.agent.application.prompt.model.PromptTemplateStatusRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplateSummary;
import com.zhiling.agent.application.prompt.model.PromptTemplateSyncView;
import com.zhiling.agent.application.prompt.model.PromptTemplateUpdateSegmentRequest;
import com.zhiling.agent.application.prompt.model.PromptSupportedTypeView;
import com.zhiling.agent.application.repository.PromptCachePort;
import com.zhiling.agent.application.repository.PromptTemplateLogRepository;
import com.zhiling.agent.application.repository.PromptTemplateRepository;
import com.zhiling.common.constant.PromptConstant;
import com.zhiling.framework.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 内置提示词控制台应用服务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPromptConsoleService {

    private static final String RESOURCE_QUERY = "/web/agent/prompt/templates";
    private static final String RESOURCE_PREVIEW = "/web/agent/prompt/preview";
    private static final String RESOURCE_SYNC = "/web/agent/prompt/sync";
    private static final String RESOURCE_LOGS = "/web/agent/prompt/logs";
    private static final String RESOURCE_MANAGE = "/web/agent/prompt/templates/**";
    private static final List<PromptSupportedTypeView> SUPPORTED_PROMPT_TYPES = List.of(
            new PromptSupportedTypeView(
                    PromptConstant.AGENT_CHAT_ROLE_PROMPT_NAME,
                    "系统角色提示词",
                    "主对话 ChatClient 的系统提示词，决定智能体身份、边界和默认回答规范。",
                    false,
                    null,
                    null),
            new PromptSupportedTypeView(
                    PromptConstant.AGENT_CHAT_KNOWLEDGE_RAG_PROMPT_NAME,
                    "知识库问答增强提示词",
                    "知识库检索增强问答使用的补充提示词，约束文档依据、引用和不确定性表达。",
                    false,
                    null,
                    null),
            new PromptSupportedTypeView(
                    PromptConstant.AGENT_CHAT_MULTIMODAL_PROMPT_NAME,
                    "多模态补充提示词",
                    "图片和多模态输入场景的补充提示词，约束视觉信息解释与降级口径。",
                    false,
                    null,
                    null)
    );
    private static final Set<String> SUPPORTED_PROMPT_NAMES = SUPPORTED_PROMPT_TYPES.stream()
            .map(PromptSupportedTypeView::promptName)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptTemplateLogRepository promptTemplateLogRepository;
    private final PromptCachePort promptCachePort;
    private final PromptTemplateMerger promptTemplateMerger;
    private final AgentPromptTemplateLoadService promptTemplateLoadService;
    private final SecurityHelper securityHelper;

    public List<PromptTemplateSummary> listTemplates(String promptName) {
        requireAccess(RESOURCE_QUERY);
        return promptTemplateRepository.selectVersionSummaries(promptName);
    }

    public List<PromptSupportedTypeView> listSupportedTypes() {
        requireAccess(RESOURCE_QUERY);
        return SUPPORTED_PROMPT_TYPES.stream()
                .map(type -> {
                    Integer maxVersion = promptTemplateRepository.selectMaxVersion(type.promptName());
                    Integer activeVersion = promptTemplateRepository.selectLatestActiveVersion(type.promptName());
                    return new PromptSupportedTypeView(
                            type.promptName(),
                            type.label(),
                            type.description(),
                            maxVersion != null,
                            maxVersion,
                            activeVersion
                    );
                })
                .toList();
    }

    public PromptTemplateActiveView getActive(String promptName) {
        requireAccess(RESOURCE_QUERY);
        String safePromptName = requirePromptName(promptName);
        Integer version = promptTemplateRepository.selectLatestActiveVersion(safePromptName);
        if (version == null) {
            throw new IllegalStateException("未找到可用提示词：" + safePromptName);
        }
        List<PromptTemplateDetail> details = promptTemplateRepository.selectDetailsByNameAndVersion(safePromptName, version);
        String mergedContent = promptTemplateMerger.merge(details.stream()
                .filter(detail -> detail.status() == null || detail.status() == 0)
                .map(detail -> new PromptTemplateSegment(detail.id(), detail.promptContent()))
                .toList());
        return new PromptTemplateActiveView(safePromptName, version, details, mergedContent);
    }

    public PromptTemplateActiveView getVersion(String promptName, Integer version) {
        requireAccess(RESOURCE_QUERY);
        String safePromptName = requirePromptName(promptName);
        Integer safeVersion = requireVersion(version);
        List<PromptTemplateDetail> details = promptTemplateRepository.selectDetailsByNameAndVersion(safePromptName, safeVersion);
        if (details.isEmpty()) {
            throw new IllegalStateException("未找到提示词版本：" + safePromptName + " v" + safeVersion);
        }
        String mergedContent = promptTemplateMerger.merge(details.stream()
                .filter(detail -> detail.status() == null || detail.status() == 0)
                .map(detail -> new PromptTemplateSegment(detail.id(), detail.promptContent()))
                .toList());
        return new PromptTemplateActiveView(safePromptName, safeVersion, details, mergedContent);
    }

    public PromptTemplatePreviewView preview(PromptTemplatePreviewRequest request) {
        requireAccess(RESOURCE_PREVIEW);
        if (request == null) {
            throw new IllegalArgumentException("预览请求不能为空");
        }
        String promptName = requirePromptName(request.promptName());
        Integer version = request.version();
        List<PromptTemplateSegment> segments;
        if (request.segments() != null && !request.segments().isEmpty()) {
            segments = request.segments().stream()
                    .map(content -> new PromptTemplateSegment(null, content))
                    .toList();
        } else {
            if (version == null) {
                version = promptTemplateRepository.selectLatestActiveVersion(promptName);
            }
            if (version == null) {
                throw new IllegalStateException("未找到可用提示词：" + promptName);
            }
            segments = promptTemplateRepository.selectActiveByNameAndVersion(promptName, version);
        }
        return new PromptTemplatePreviewView(promptName, version, promptTemplateMerger.merge(segments));
    }

    public PromptTemplateCacheView getCache(String promptName) {
        requireAccess(RESOURCE_QUERY);
        String safePromptName = requirePromptName(promptName);
        String redisKey = buildPromptRedisKey(safePromptName);
        return new PromptTemplateCacheView(safePromptName, redisKey, promptCachePort.get(redisKey));
    }

    public PromptTemplateSyncView sync(String promptName) {
        requireAccess(RESOURCE_SYNC);
        String safePromptName = requirePromptName(promptName);
        String operator = securityHelper.getCurrentUserId() == null
                ? "unknown"
                : String.valueOf(securityHelper.getCurrentUserId());
        String content = promptTemplateLoadService.loadPromptToRedis(
                safePromptName,
                "MANUAL_RELOAD",
                operator,
                "控制台手动同步提示词到Redis");
        log.info("提示词控制台手动同步Redis完成，promptName={} operator={} redisKey={} contentLength={}",
                safePromptName, operator, buildPromptRedisKey(safePromptName), content == null ? 0 : content.length());
        return new PromptTemplateSyncView(safePromptName, buildPromptRedisKey(safePromptName), content);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateActiveView createVersion(String promptName, PromptTemplateCreateVersionRequest request) {
        requireAccess(RESOURCE_MANAGE);
        String safePromptName = requirePromptName(promptName);
        if (request == null) {
            throw new IllegalArgumentException("新增版本请求不能为空");
        }
        List<PromptTemplateSegmentEdit> segments = normalizeCreateSegments(request);
        Integer previousMaxVersion = promptTemplateRepository.selectMaxVersion(safePromptName);
        Integer nextVersion = Objects.requireNonNullElse(previousMaxVersion, 0) + 1;
        List<PromptTemplateDetail> created = promptTemplateRepository.insertVersion(safePromptName, nextVersion, segments, 1);
        if (created.isEmpty()) {
            throw new IllegalStateException("新增提示词版本失败");
        }
        writeChangeLog(created.get(0).id(), safePromptName, 0, previousMaxVersion, nextVersion,
                null, mergeDetails(created), "CREATE_VERSION",
                StrUtil.blankToDefault(request.remark(), "控制台新增提示词版本"));
        if (Boolean.TRUE.equals(request.syncToRedis())) {
            // 新增版本默认停用，必须先启用后才能同步最新启用版本。
            activateVersion(safePromptName, nextVersion, new PromptTemplateStatusRequest("新增版本后启用并同步", true));
        }
        log.info("提示词控制台新增版本完成，promptName={} previousMaxVersion={} newVersion={} segmentCount={} status={} syncToRedis={} operator={}",
                safePromptName, previousMaxVersion, nextVersion, created.size(), 1, request.syncToRedis(), currentOperator());
        return getVersion(safePromptName, nextVersion);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateActiveView updateSegment(Long id, PromptTemplateUpdateSegmentRequest request) {
        requireAccess(RESOURCE_MANAGE);
        if (id == null) {
            throw new IllegalArgumentException("片段 id 不能为空");
        }
        if (request == null || StrUtil.isBlank(request.promptContent())) {
            throw new IllegalArgumentException("提示词片段内容不能为空");
        }
        PromptTemplateDetail oldDetail = requireDetail(id);
        PromptTemplateDetail updated = promptTemplateRepository.updateSegmentContent(id, request.promptContent().trim());
        writeChangeLog(id, oldDetail.promptName(), oldDetail.promptIndex(), oldDetail.version(), updated.version(),
                oldDetail.promptContent(), updated.promptContent(), "UPDATE_SEGMENT",
                StrUtil.blankToDefault(request.remark(), "控制台更新提示词片段"));
        syncIfRequested(updated.promptName(), request.syncToRedis(), "UPDATE_SEGMENT", "更新片段后同步提示词到Redis");
        log.info("提示词控制台更新片段完成，promptName={} version={} segmentId={} promptIndex={} oldLength={} newLength={} syncToRedis={} operator={}",
                updated.promptName(), updated.version(), id, updated.promptIndex(),
                oldDetail.promptContent() == null ? 0 : oldDetail.promptContent().length(),
                updated.promptContent() == null ? 0 : updated.promptContent().length(),
                request.syncToRedis(), currentOperator());
        return getVersion(updated.promptName(), updated.version());
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateActiveView createSegment(String promptName, Integer version, PromptTemplateCreateSegmentRequest request) {
        requireAccess(RESOURCE_MANAGE);
        String safePromptName = requirePromptName(promptName);
        Integer safeVersion = requireVersion(version);
        if (request == null || StrUtil.isBlank(request.promptContent())) {
            throw new IllegalArgumentException("提示词片段内容不能为空");
        }
        List<PromptTemplateDetail> details = promptTemplateRepository.selectDetailsByNameAndVersion(safePromptName, safeVersion);
        if (details.isEmpty()) {
            throw new IllegalStateException("未找到提示词版本：" + safePromptName + " v" + safeVersion);
        }
        Integer nextPromptIndex = request.promptIndex();
        if (nextPromptIndex == null || nextPromptIndex <= 0) {
            nextPromptIndex = details.stream()
                    .map(PromptTemplateDetail::promptIndex)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
        }
        PromptTemplateDetail created = promptTemplateRepository.insertSegment(
                safePromptName,
                safeVersion,
                new PromptTemplateSegmentEdit(nextPromptIndex, request.promptContent().trim()),
                0);
        writeChangeLog(created.id(), safePromptName, created.promptIndex(), safeVersion, safeVersion,
                null, created.promptContent(), "CREATE_SEGMENT",
                StrUtil.blankToDefault(request.remark(), "控制台新增提示词片段"));
        syncIfRequested(safePromptName, Boolean.TRUE.equals(request.syncToRedis()),
                "CREATE_SEGMENT", "新增片段后同步提示词到Redis");
        log.info("提示词控制台新增片段完成，promptName={} version={} segmentId={} promptIndex={} contentLength={} syncToRedis={} operator={}",
                safePromptName, safeVersion, created.id(), created.promptIndex(),
                created.promptContent() == null ? 0 : created.promptContent().length(),
                request.syncToRedis(), currentOperator());
        return getVersion(safePromptName, safeVersion);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateActiveView activateVersion(String promptName, Integer version, PromptTemplateStatusRequest request) {
        requireAccess(RESOURCE_MANAGE);
        String safePromptName = requirePromptName(promptName);
        Integer safeVersion = requireVersion(version);
        List<PromptTemplateDetail> before = promptTemplateRepository.selectDetailsByNameAndVersion(safePromptName, safeVersion);
        if (before.isEmpty()) {
            throw new IllegalStateException("未找到提示词版本：" + safePromptName + " v" + safeVersion);
        }
        promptTemplateRepository.updateOtherVersionsStatus(safePromptName, safeVersion, 1);
        List<PromptTemplateDetail> after = promptTemplateRepository.updateVersionStatus(safePromptName, safeVersion, 0);
        writeChangeLog(after.get(0).id(), safePromptName, 0, safeVersion, safeVersion,
                mergeDetails(before), mergeDetails(after), "ENABLE_VERSION",
                resolveRemark(request, "控制台启用提示词版本"));
        syncIfRequested(safePromptName, request == null || !Boolean.FALSE.equals(request.syncToRedis()),
                "ENABLE_VERSION", "启用版本后同步提示词到Redis");
        log.info("提示词控制台启用版本完成，promptName={} version={} segmentCount={} exclusive=true syncToRedis={} operator={}",
                safePromptName, safeVersion, after.size(),
                request == null || !Boolean.FALSE.equals(request.syncToRedis()), currentOperator());
        return getVersion(safePromptName, safeVersion);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateActiveView disableVersion(String promptName, Integer version, PromptTemplateStatusRequest request) {
        requireAccess(RESOURCE_MANAGE);
        String safePromptName = requirePromptName(promptName);
        Integer safeVersion = requireVersion(version);
        List<PromptTemplateDetail> before = promptTemplateRepository.selectDetailsByNameAndVersion(safePromptName, safeVersion);
        if (before.isEmpty()) {
            throw new IllegalStateException("未找到提示词版本：" + safePromptName + " v" + safeVersion);
        }
        List<PromptTemplateDetail> after = promptTemplateRepository.updateVersionStatus(safePromptName, safeVersion, 1);
        writeChangeLog(after.get(0).id(), safePromptName, 0, safeVersion, safeVersion,
                mergeDetails(before), mergeDetails(after), "DISABLE_VERSION",
                resolveRemark(request, "控制台停用提示词版本"));
        syncIfRequested(safePromptName, request != null && Boolean.TRUE.equals(request.syncToRedis()),
                "DISABLE_VERSION", "停用版本后同步提示词到Redis");
        log.info("提示词控制台停用版本完成，promptName={} version={} segmentCount={} syncToRedis={} operator={}",
                safePromptName, safeVersion, after.size(),
                request != null && Boolean.TRUE.equals(request.syncToRedis()), currentOperator());
        return getVersion(safePromptName, safeVersion);
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateActiveView enableSegment(Long id, PromptTemplateStatusRequest request) {
        return updateSegmentStatus(id, 0, "ENABLE_SEGMENT", resolveRemark(request, "控制台启用提示词片段"),
                request != null && Boolean.TRUE.equals(request.syncToRedis()));
    }

    @Transactional(rollbackFor = Exception.class)
    public PromptTemplateActiveView disableSegment(Long id, PromptTemplateStatusRequest request) {
        return updateSegmentStatus(id, 1, "DISABLE_SEGMENT", resolveRemark(request, "控制台停用提示词片段"),
                request != null && Boolean.TRUE.equals(request.syncToRedis()));
    }

    public List<PromptTemplateLogView> listLogs(String promptName, String operationType, Integer limit) {
        requireAccess(RESOURCE_LOGS);
        return promptTemplateLogRepository.listLogs(promptName, operationType, limit == null ? 20 : limit);
    }

    private void requireAccess(String resourcePath) {
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            throw new AccessDeniedException("无权访问内置提示词控制台");
        }
        if (securityHelper.hasResourcePathForSensitiveOperation(resourcePath)
                || securityHelper.hasResourcePathForSensitiveOperation(RESOURCE_MANAGE)) {
            return;
        }
        throw new AccessDeniedException("无权访问内置提示词控制台");
    }

    private String requirePromptName(String promptName) {
        if (StrUtil.isBlank(promptName)) {
            throw new IllegalArgumentException("promptName 不能为空");
        }
        String safePromptName = promptName.trim();
        if (!SUPPORTED_PROMPT_NAMES.contains(safePromptName)) {
            throw new IllegalArgumentException("不支持的提示词类型：" + safePromptName);
        }
        return safePromptName;
    }

    private Integer requireVersion(Integer version) {
        if (version == null || version <= 0) {
            throw new IllegalArgumentException("version 必须为正整数");
        }
        return version;
    }

    private PromptTemplateDetail requireDetail(Long id) {
        PromptTemplateDetail detail = promptTemplateRepository.selectDetailById(id);
        if (detail == null) {
            throw new IllegalStateException("未找到提示词片段：" + id);
        }
        return detail;
    }

    private List<PromptTemplateSegmentEdit> normalizeCreateSegments(PromptTemplateCreateVersionRequest request) {
        List<PromptTemplateSegmentEdit> sourceSegments = request.segments();
        if (sourceSegments == null || sourceSegments.isEmpty()) {
            throw new IllegalArgumentException("新增版本至少需要一个提示词片段");
        }
        AtomicInteger fallbackIndex = new AtomicInteger(1);
        List<PromptTemplateSegmentEdit> normalized = sourceSegments.stream()
                .filter(segment -> segment != null && StrUtil.isNotBlank(segment.promptContent()))
                .sorted(Comparator.comparing(segment -> segment.promptIndex() == null ? Integer.MAX_VALUE : segment.promptIndex()))
                .map(segment -> normalizeSegment(segment, fallbackIndex.getAndIncrement()))
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("新增版本至少需要一个非空提示词片段");
        }
        return normalized;
    }

    private PromptTemplateSegmentEdit normalizeSegment(PromptTemplateSegmentEdit segment, int fallbackIndex) {
        Integer promptIndex = segment.promptIndex() == null || segment.promptIndex() <= 0 ? fallbackIndex : segment.promptIndex();
        return new PromptTemplateSegmentEdit(promptIndex, segment.promptContent().trim());
    }

    private PromptTemplateActiveView updateSegmentStatus(Long id, Integer status, String operationType, String remark, boolean syncToRedis) {
        requireAccess(RESOURCE_MANAGE);
        if (id == null) {
            throw new IllegalArgumentException("片段 id 不能为空");
        }
        PromptTemplateDetail oldDetail = requireDetail(id);
        PromptTemplateDetail updated = promptTemplateRepository.updateSegmentStatus(id, status);
        writeChangeLog(id, oldDetail.promptName(), oldDetail.promptIndex(), oldDetail.version(), updated.version(),
                statusText(oldDetail.status()), statusText(updated.status()), operationType, remark);
        syncIfRequested(updated.promptName(), syncToRedis, operationType, "片段启停后同步提示词到Redis");
        log.info("提示词控制台片段状态变更完成，operationType={} promptName={} version={} segmentId={} promptIndex={} oldStatus={} newStatus={} syncToRedis={} operator={}",
                operationType, updated.promptName(), updated.version(), id, updated.promptIndex(),
                oldDetail.status(), updated.status(), syncToRedis, currentOperator());
        return getVersion(updated.promptName(), updated.version());
    }

    private void syncIfRequested(String promptName, boolean syncToRedis, String operationType, String remark) {
        if (syncToRedis) {
            syncPromptToRedis(promptName, operationType, remark);
        }
    }

    private void syncPromptToRedis(String promptName, String operationType, String remark) {
        String operator = currentOperator();
        log.info("提示词控制台准备同步Redis，promptName={} operationType={} operator={} remark={}",
                promptName, operationType, operator, remark);
        promptTemplateLoadService.loadPromptToRedis(promptName, operationType + "_SYNC", operator, remark);
    }

    private void writeChangeLog(Long promptId, String promptName, Integer promptIndex, Integer oldVersion, Integer newVersion,
                                String oldContent, String newContent, String operationType, String remark) {
        promptTemplateLogRepository.saveChangeLog(new PromptTemplateChangeLogCommand(
                promptId,
                promptName,
                promptIndex,
                oldVersion,
                newVersion,
                oldContent,
                newContent,
                operationType,
                currentOperator(),
                remark
        ));
    }

    private String currentOperator() {
        return securityHelper.getCurrentUserId() == null
                ? "unknown"
                : String.valueOf(securityHelper.getCurrentUserId());
    }

    private String mergeDetails(List<PromptTemplateDetail> details) {
        return promptTemplateMerger.merge(details.stream()
                .sorted(Comparator.comparing(PromptTemplateDetail::promptIndex))
                .filter(detail -> detail.status() == null || detail.status() == 0)
                .map(detail -> new PromptTemplateSegment(detail.id(), detail.promptContent()))
                .toList());
    }

    private String resolveRemark(PromptTemplateStatusRequest request, String defaultRemark) {
        return request == null ? defaultRemark : StrUtil.blankToDefault(request.remark(), defaultRemark);
    }

    private String statusText(Integer status) {
        return status == null || status == 0 ? "启用" : "停用";
    }

    private String buildPromptRedisKey(String promptName) {
        if (PromptConstant.AGENT_CHAT_ROLE_PROMPT_NAME.equals(promptName)) {
            return PromptConstant.AGENT_CHAT_ROLE_PROMPT_REDIS_KEY;
        }
        return PromptConstant.AGENT_PROMPT_CACHE_PREFIX + promptName;
    }
}
