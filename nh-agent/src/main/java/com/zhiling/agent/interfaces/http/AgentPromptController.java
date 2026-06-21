package com.zhiling.agent.interfaces.http;

import com.zhiling.agent.application.prompt.AgentPromptConsoleService;
import com.zhiling.agent.application.prompt.model.PromptTemplateActiveView;
import com.zhiling.agent.application.prompt.model.PromptTemplateCacheView;
import com.zhiling.agent.application.prompt.model.PromptTemplateCreateSegmentRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplateCreateVersionRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplateLogView;
import com.zhiling.agent.application.prompt.model.PromptTemplatePreviewRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplatePreviewView;
import com.zhiling.agent.application.prompt.model.PromptTemplateStatusRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplateSummary;
import com.zhiling.agent.application.prompt.model.PromptTemplateSyncRequest;
import com.zhiling.agent.application.prompt.model.PromptTemplateSyncView;
import com.zhiling.agent.application.prompt.model.PromptTemplateUpdateSegmentRequest;
import com.zhiling.agent.application.prompt.model.PromptSupportedTypeView;
import com.zhiling.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内置提示词控制台接口。
 */
@RestController
@RequestMapping("/api/v1/agent/prompt")
@Tag(name = "内置提示词控制台")
@RequiredArgsConstructor
public class AgentPromptController {

    private final AgentPromptConsoleService promptConsoleService;

    @GetMapping("/templates")
    @Operation(summary = "查询提示词模板版本摘要")
    public Result<List<PromptTemplateSummary>> listTemplates(@RequestParam(required = false) String promptName) {
        return Result.success(promptConsoleService.listTemplates(promptName));
    }

    @GetMapping("/supported-types")
    @Operation(summary = "查询系统支持的提示词类型")
    public Result<List<PromptSupportedTypeView>> listSupportedTypes() {
        return Result.success(promptConsoleService.listSupportedTypes());
    }

    @GetMapping("/templates/{promptName}/active")
    @Operation(summary = "查询当前启用提示词")
    public Result<PromptTemplateActiveView> getActive(@PathVariable String promptName) {
        return Result.success(promptConsoleService.getActive(promptName));
    }

    @GetMapping("/templates/{promptName}/versions/{version}")
    @Operation(summary = "查询指定提示词版本")
    public Result<PromptTemplateActiveView> getVersion(@PathVariable String promptName, @PathVariable Integer version) {
        return Result.success(promptConsoleService.getVersion(promptName, version));
    }

    @PostMapping("/preview")
    @Operation(summary = "预览提示词合并结果")
    public Result<PromptTemplatePreviewView> preview(@RequestBody PromptTemplatePreviewRequest request) {
        return Result.success(promptConsoleService.preview(request));
    }

    @GetMapping("/templates/{promptName}/cache")
    @Operation(summary = "查询提示词 Redis 缓存")
    public Result<PromptTemplateCacheView> getCache(@PathVariable String promptName) {
        return Result.success(promptConsoleService.getCache(promptName));
    }

    @PostMapping("/sync")
    @Operation(summary = "手动同步提示词到 Redis")
    public Result<PromptTemplateSyncView> sync(@RequestBody PromptTemplateSyncRequest request) {
        String promptName = request == null ? null : request.promptName();
        return Result.success(promptConsoleService.sync(promptName));
    }

    @PostMapping("/templates/{promptName}/versions")
    @Operation(summary = "新增提示词版本")
    public Result<PromptTemplateActiveView> createVersion(@PathVariable String promptName,
                                                          @RequestBody PromptTemplateCreateVersionRequest request) {
        return Result.success(promptConsoleService.createVersion(promptName, request));
    }

    @PutMapping("/templates/{promptName}/versions/{version}/activate")
    @Operation(summary = "启用提示词版本")
    public Result<PromptTemplateActiveView> activateVersion(@PathVariable String promptName,
                                                            @PathVariable Integer version,
                                                            @RequestBody(required = false) PromptTemplateStatusRequest request) {
        return Result.success(promptConsoleService.activateVersion(promptName, version, request));
    }

    @PutMapping("/templates/{promptName}/versions/{version}/disable")
    @Operation(summary = "停用提示词版本")
    public Result<PromptTemplateActiveView> disableVersion(@PathVariable String promptName,
                                                           @PathVariable Integer version,
                                                           @RequestBody(required = false) PromptTemplateStatusRequest request) {
        return Result.success(promptConsoleService.disableVersion(promptName, version, request));
    }

    @PutMapping("/templates/segments/{id}")
    @Operation(summary = "更新提示词片段")
    public Result<PromptTemplateActiveView> updateSegment(@PathVariable Long id,
                                                          @RequestBody PromptTemplateUpdateSegmentRequest request) {
        return Result.success(promptConsoleService.updateSegment(id, request));
    }

    @PostMapping("/templates/{promptName}/versions/{version}/segments")
    @Operation(summary = "新增提示词片段")
    public Result<PromptTemplateActiveView> createSegment(@PathVariable String promptName,
                                                          @PathVariable Integer version,
                                                          @RequestBody PromptTemplateCreateSegmentRequest request) {
        return Result.success(promptConsoleService.createSegment(promptName, version, request));
    }

    @PutMapping("/templates/segments/{id}/enable")
    @Operation(summary = "启用提示词片段")
    public Result<PromptTemplateActiveView> enableSegment(@PathVariable Long id,
                                                          @RequestBody(required = false) PromptTemplateStatusRequest request) {
        return Result.success(promptConsoleService.enableSegment(id, request));
    }

    @PutMapping("/templates/segments/{id}/disable")
    @Operation(summary = "停用提示词片段")
    public Result<PromptTemplateActiveView> disableSegment(@PathVariable Long id,
                                                           @RequestBody(required = false) PromptTemplateStatusRequest request) {
        return Result.success(promptConsoleService.disableSegment(id, request));
    }

    @GetMapping("/logs")
    @Operation(summary = "查询提示词日志")
    public Result<List<PromptTemplateLogView>> listLogs(@RequestParam(required = false) String promptName,
                                                        @RequestParam(required = false) String operationType,
                                                        @RequestParam(required = false) Integer limit) {
        return Result.success(promptConsoleService.listLogs(promptName, operationType, limit));
    }
}
