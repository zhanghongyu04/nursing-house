package com.zhiling.system.interfaces.system;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.common.constant.RoleConstant;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.system.model.NursingLogQueryResult;
import com.zhiling.framework.system.model.NursingLogRecord;
import com.zhiling.framework.system.model.NursingTaskQueryResult;
import com.zhiling.framework.system.model.NursingTaskRecord;
import com.zhiling.framework.system.port.NursingCareQueryPort;
import com.zhiling.model.dto.NursingLogMyPageQueryDto;
import com.zhiling.model.dto.NursingLogPageQueryDto;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.entity.NursingLog;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.model.entity.Sanatorium;
import com.zhiling.system.infrastructure.persistence.query.NursingLogQueryService;
import com.zhiling.system.infrastructure.persistence.query.NursingTaskQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 护理任务与护理日志查询公开契约适配器。
 *
 * @author zhanghongyu
 */
@Component
public class NursingCareQueryPortAdapter implements NursingCareQueryPort {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    private final NursingTaskQueryService nursingTaskQueryService;
    private final NursingLogQueryService nursingLogQueryService;
    private final SanatoriumScopeResolver scopeResolver;
    private final SecurityHelper securityHelper;

    public NursingCareQueryPortAdapter(NursingTaskQueryService nursingTaskQueryService,
                                       NursingLogQueryService nursingLogQueryService,
                                       SanatoriumScopeResolver scopeResolver,
                                       SecurityHelper securityHelper) {
        this.nursingTaskQueryService = nursingTaskQueryService;
        this.nursingLogQueryService = nursingLogQueryService;
        this.scopeResolver = scopeResolver;
        this.securityHelper = securityHelper;
    }

    @Override
    public NursingTaskQueryResult listNursingTasks(String sanaName, Set<Integer> statuses, Integer limit) {
        ScopeQuery scope = resolveScope(sanaName);
        if (scope.isEmpty()) {
            return NursingTaskQueryResult.builder()
                    .scope("none")
                    .scopeName("当前账号无可查询机构范围")
                    .total(0L)
                    .records(List.of())
                    .build();
        }

        Page<NursingTask> page = new Page<>(1, normalizeLimit(limit));
        IPage<NursingTask> result;
        if (isNurseOnly()) {
            NursingTaskMyPageQueryDto dto = NursingTaskMyPageQueryDto.builder()
                    .page(1)
                    .pageSize(normalizeLimit(limit))
                    .sanaId(scope.sanaId())
                    .assigneeUserId(securityHelper.getCurrentUserId())
                    .statuses(statuses)
                    .sanaScopeIds(scope.scopeIds())
                    .build();
            result = nursingTaskQueryService.myPage(page, dto);
        } else {
            NursingTaskPageQueryDto dto = NursingTaskPageQueryDto.builder()
                    .page(1)
                    .pageSize(normalizeLimit(limit))
                    .sanaId(scope.sanaId())
                    .statuses(statuses)
                    .sanaScopeIds(scope.scopeIds())
                    .build();
            result = nursingTaskQueryService.page(page, dto);
        }

        return NursingTaskQueryResult.builder()
                .scope(scope.scope())
                .scopeName(scope.scopeName())
                .total(result.getTotal())
                .records(records(result.getRecords()).stream().map(this::toTaskRecord).toList())
                .build();
    }

    @Override
    public NursingLogQueryResult listNursingLogs(String sanaName, Integer abnormalFlag, Integer limit) {
        ScopeQuery scope = resolveScope(sanaName);
        if (scope.isEmpty()) {
            return NursingLogQueryResult.builder()
                    .scope("none")
                    .scopeName("当前账号无可查询机构范围")
                    .total(0L)
                    .records(List.of())
                    .build();
        }

        Page<NursingLog> page = new Page<>(1, normalizeLimit(limit));
        IPage<NursingLog> result;
        if (isNurseOnly()) {
            NursingLogMyPageQueryDto dto = NursingLogMyPageQueryDto.builder()
                    .page(1)
                    .pageSize(normalizeLimit(limit))
                    .sanaId(scope.sanaId())
                    .nurseUserId(securityHelper.getCurrentUserId())
                    .abnormalFlag(abnormalFlag)
                    .sanaScopeIds(scope.scopeIds())
                    .build();
            result = nursingLogQueryService.myPage(page, dto);
        } else {
            NursingLogPageQueryDto dto = NursingLogPageQueryDto.builder()
                    .page(1)
                    .pageSize(normalizeLimit(limit))
                    .sanaId(scope.sanaId())
                    .abnormalFlag(abnormalFlag)
                    .sanaScopeIds(scope.scopeIds())
                    .build();
            result = nursingLogQueryService.page(page, dto);
        }

        return NursingLogQueryResult.builder()
                .scope(scope.scope())
                .scopeName(scope.scopeName())
                .total(result.getTotal())
                .records(records(result.getRecords()).stream().map(this::toLogRecord).toList())
                .build();
    }

    private ScopeQuery resolveScope(String sanaName) {
        Set<Long> scopeIds = scopeResolver.currentScopeIds();
        if (scopeIds.isEmpty()) {
            return ScopeQuery.empty();
        }
        if (StrUtil.isBlank(sanaName)) {
            return new ScopeQuery("organization", scopeResolver.resolveScopeName(), null, scopeIds, false);
        }
        Sanatorium sanatorium = scopeResolver.resolveScopedSanatorium(sanaName);
        if (sanatorium == null) {
            return ScopeQuery.empty();
        }
        return new ScopeQuery("organization", sanatorium.getSanaName(), sanatorium.getId(), scopeIds, false);
    }

    private boolean isNurseOnly() {
        boolean nurse = securityHelper.hasAnyRoleForSensitiveOperation(RoleConstant.NURSE);
        boolean orgManager = securityHelper.hasAnyRoleForSensitiveOperation(RoleConstant.ORG_ADMIN, RoleConstant.PARENT_ORG_ADMIN);
        return nurse && !orgManager;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private <T> List<T> records(List<T> records) {
        return records == null ? List.of() : records;
    }

    private NursingTaskRecord toTaskRecord(NursingTask task) {
        return NursingTaskRecord.builder()
                .id(task.getId())
                .sanaId(task.getSanaId())
                .sanaName(task.getSanaName())
                .elderId(task.getElderId())
                .elderName(task.getElderName())
                .taskTitle(task.getTaskTitle())
                .taskContent(task.getTaskContent())
                .taskType(task.getTaskType())
                .taskTypeName(formatTaskType(task.getTaskType()))
                .priority(task.getPriority())
                .priorityName(formatPriority(task.getPriority()))
                .status(task.getStatus())
                .statusName(formatTaskStatus(task.getStatus()))
                .assigneeUserId(task.getAssigneeUserId())
                .assigneeUsername(task.getAssigneeUsername())
                .assignerUserId(task.getAssignerUserId())
                .assignerUsername(task.getAssignerUsername())
                .plannedStartTime(task.getPlannedStartTime())
                .plannedEndTime(task.getPlannedEndTime())
                .completionTime(task.getCompletionTime())
                .createTime(task.getCreateTime())
                .build();
    }

    private NursingLogRecord toLogRecord(NursingLog log) {
        return NursingLogRecord.builder()
                .id(log.getId())
                .sanaId(log.getSanaId())
                .sanaName(log.getSanaName())
                .taskId(log.getTaskId())
                .taskTitle(log.getTaskTitle())
                .elderId(log.getElderId())
                .elderName(log.getElderName())
                .nurseUserId(log.getNurseUserId())
                .nurseUsername(log.getNurseUsername())
                .logTime(log.getLogTime())
                .content(log.getContent())
                .abnormalFlag(log.getAbnormalFlag())
                .abnormalName(log.getAbnormalFlag() != null && log.getAbnormalFlag() == 1 ? "异常" : "正常")
                .hasAttachment(StrUtil.isNotBlank(log.getAttachmentUrls()))
                .createTime(log.getCreateTime())
                .build();
    }

    private String formatTaskStatus(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待执行";
            case 1 -> "执行中";
            case 2 -> "已完成";
            case 3 -> "已取消";
            case 4 -> "已超时";
            default -> "未知";
        };
    }

    private String formatPriority(Integer priority) {
        if (priority == null) {
            return "未知";
        }
        return switch (priority) {
            case 0 -> "低";
            case 1 -> "普通";
            case 2 -> "高";
            default -> "未知";
        };
    }

    private String formatTaskType(Integer taskType) {
        if (taskType == null) {
            return "未知";
        }
        return switch (taskType) {
            case 0 -> "生活护理";
            case 1 -> "用药护理";
            case 2 -> "康复护理";
            case 3 -> "巡检观察";
            default -> "其他";
        };
    }

    private record ScopeQuery(String scope, String scopeName, Long sanaId, Set<Long> scopeIds, boolean emptyScope) {
        static ScopeQuery empty() {
            return new ScopeQuery("none", "当前账号无可查询机构范围", null, Set.of(), true);
        }

        boolean isEmpty() {
            return emptyScope;
        }
    }
}
