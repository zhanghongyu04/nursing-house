package com.zhiling.system.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiling.common.constant.RoleConstant;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.common.result.PageResult;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.dto.TaskTemplateCreateDto;
import com.zhiling.model.dto.TaskTemplatePageQueryDto;
import com.zhiling.model.dto.TaskTemplateScheduleConfigDto;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.model.entity.NursingTaskTemplate;
import com.zhiling.system.application.repository.NursingTaskRepository;
import com.zhiling.system.application.service.NursingTaskTemplateService;
import com.zhiling.system.application.service.support.TaskTemplateScheduleSupport;
import com.zhiling.system.infrastructure.persistence.mapper.NursingTaskTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
/**
 * NursingTaskTemplateServiceImpl
 *
 * @author zhanghongyu
 */
public class NursingTaskTemplateServiceImpl implements NursingTaskTemplateService {

    private static final int DEFAULT_PLANNED_DURATION = 60;
    private static final int MAX_CATCH_UP_COUNT = 100;
    private static final String TEMPLATE_PAGE_PATH = "/web/nursing-task-template/page";
    private static final String TEMPLATE_CREATE_PATH = "/web/nursing-task-template/create";
    private static final String TEMPLATE_UPDATE_PATH = "/web/nursing-task-template/update";
    private static final String TEMPLATE_TOGGLE_PATH = "/web/nursing-task-template/toggle/**";
    private static final String TEMPLATE_REMOVE_PATH = "/web/nursing-task-template/remove/**";
    private static final String TEMPLATE_GENERATE_PATH = "/web/nursing-task-template/generate/**";

    private final NursingTaskTemplateMapper templateMapper;
    private final NursingTaskRepository nursingTaskRepository;
    private final SecurityHelper securityHelper;
    private final ObjectMapper objectMapper;

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult page(TaskTemplatePageQueryDto dto) {
        requireResource("template.page", TEMPLATE_PAGE_PATH);
        ensureOrgManagerRole("template.page");
        if (!securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            dto.setSanaScopeIds(securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation());
        }
        Page<NursingTaskTemplate> page = new Page<>(dto.getPage(), dto.getPageSize());
        IPage<NursingTaskTemplate> result = templateMapper.page(page, dto);
        result.getRecords().forEach(this::hydrateScheduleView);
        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 方法：create
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean create(TaskTemplateCreateDto dto) {
        requireResource("template.create", TEMPLATE_CREATE_PATH);
        ensureOrgManagerRole("template.create");
        PreparedSchedule prepared = validateAndPrepareTemplate(dto);

        NursingTaskTemplate template = new NursingTaskTemplate();
        Set<Long> scopeIds = securityHelper.hasGovAdminRoleForSensitiveOperation()
                ? Set.of()
                : securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
        Long targetSanaId = resolveTargetSanaId(dto.getSanaId(), scopeIds);
        fillTemplate(template, dto, targetSanaId, prepared);
        template.setEnabled(1);
        template.setStatus(0);
        template.setVersion(0);
        template.setLastTriggerTime(null);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        boolean created = templateMapper.insert(template) > 0;
        if (created) {
            log.info("[TaskTemplate] 模板创建成功: templateId={}, sanaId={}, operatorUserId={}, scheduleType={}, timeMode={}, nextTriggerTime={}",
                    template.getId(),
                    template.getSanaId(),
                    securityHelper.getCurrentUserId(),
                    template.getScheduleType(),
                    template.getTimeMode(),
                    template.getNextTriggerTime());
        }
        return created;
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean update(TaskTemplateCreateDto dto) {
        requireResource("template.update", TEMPLATE_UPDATE_PATH);
        ensureOrgManagerRole("template.update");
        if (dto.getId() == null) {
            throw new ProjectException(400, "模板ID不能为空");
        }

        NursingTaskTemplate existing = templateMapper.selectById(dto.getId());
        if (existing == null) {
            throw new ProjectException(404, "模板不存在");
        }
        ensureTemplateScope(existing);

        PreparedSchedule prepared = validateAndPrepareTemplate(dto);
        Set<Long> scopeIds = securityHelper.hasGovAdminRoleForSensitiveOperation()
                ? Set.of()
                : securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
        Long targetSanaId = resolveTargetSanaId(dto.getSanaId() != null ? dto.getSanaId() : existing.getSanaId(), scopeIds);
        fillTemplate(existing, dto, targetSanaId, prepared);
        existing.setUpdateTime(LocalDateTime.now());
        existing.setVersion(existing.getVersion() == null ? 0 : existing.getVersion() + 1);
        boolean updated = templateMapper.updateById(existing) > 0;
        if (updated) {
            log.info("[TaskTemplate] 模板更新成功: templateId={}, sanaId={}, operatorUserId={}, version={}, scheduleType={}, timeMode={}, nextTriggerTime={}",
                    existing.getId(),
                    existing.getSanaId(),
                    securityHelper.getCurrentUserId(),
                    existing.getVersion(),
                    existing.getScheduleType(),
                    existing.getTimeMode(),
                    existing.getNextTriggerTime());
        }
        return updated;
    }

    /**
     * 方法：toggleEnabled
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean toggleEnabled(Long id) {
        requireResource("template.toggle", TEMPLATE_TOGGLE_PATH);
        ensureOrgManagerRole("template.toggle");
        NursingTaskTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new ProjectException(404, "模板不存在");
        }
        ensureTemplateScope(template);

        template.setEnabled(template.getEnabled() == 1 ? 0 : 1);
        if (template.getEnabled() == 1) {
            TaskTemplateScheduleConfigDto config = readScheduleConfig(template);
            template.setNextTriggerTime(TaskTemplateScheduleSupport.calculateNextTrigger(
                    config,
                    template.getStartDate(),
                    template.getEndDate(),
                    template.getTimezone(),
                    LocalDateTime.now()
            ));
        } else {
            template.setNextTriggerTime(null);
        }
        template.setVersion(template.getVersion() == null ? 0 : template.getVersion() + 1);
        template.setUpdateTime(LocalDateTime.now());
        boolean toggled = templateMapper.updateById(template) > 0;
        if (toggled) {
            log.info("[TaskTemplate] 模板启停切换成功: templateId={}, sanaId={}, operatorUserId={}, enabled={}, version={}, nextTriggerTime={}",
                    template.getId(),
                    template.getSanaId(),
                    securityHelper.getCurrentUserId(),
                    template.getEnabled(),
                    template.getVersion(),
                    template.getNextTriggerTime());
        }
        return toggled;
    }

    /**
     * 方法：remove
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean remove(Long id) {
        requireResource("template.remove", TEMPLATE_REMOVE_PATH);
        ensureOrgManagerRole("template.remove");
        NursingTaskTemplate template = templateMapper.selectById(id);
        if (template == null) {
            log.warn("[TaskTemplate] 模板删除失败: templateId={} 不存在, operatorUserId={}", id, securityHelper.getCurrentUserId());
            return false;
        }
        ensureTemplateScope(template);
        boolean removed = templateMapper.deleteById(id) > 0;
        if (removed) {
            log.info("[TaskTemplate] 模板删除成功: templateId={}, sanaId={}, operatorUserId={}",
                    template.getId(),
                    template.getSanaId(),
                    securityHelper.getCurrentUserId());
        }
        return removed;
    }

    /**
     * 方法：generateTasksForTemplate
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public int generateTasksForTemplate(Long templateId) {
        requireResource("template.generate", TEMPLATE_GENERATE_PATH);
        ensureOrgManagerRole("template.generate");
        NursingTaskTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            log.warn("[TaskTemplate] 手动生成失败: templateId={} 不存在, operatorUserId={}", templateId, securityHelper.getCurrentUserId());
            throw new ProjectException(404, "模板不存在");
        }
        ensureTemplateScope(template);
        if (template.getAssigneeUserId() == null) {
            log.warn("[TaskTemplate] 手动生成失败: templateId={} 未指定执行人, operatorUserId={}", template.getId(), securityHelper.getCurrentUserId());
            throw new ProjectException(400, "模板未指定执行人");
        }

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        insertTaskFromTemplate(template, now);
        log.info("[TaskTemplate] 手动生成成功: templateId={}, sanaId={}, operatorUserId={}, executionTime={}, nextTriggerTimeUnchanged={}",
                template.getId(),
                template.getSanaId(),
                securityHelper.getCurrentUserId(),
                now,
                template.getNextTriggerTime());
        return 1;
    }

    /**
     * 方法：generateAllDueTasks
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public int generateAllDueTasks() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        List<NursingTaskTemplate> dueTemplates = templateMapper.selectList(
                new LambdaQueryWrapper<NursingTaskTemplate>()
                        .eq(NursingTaskTemplate::getEnabled, 1)
                        .eq(NursingTaskTemplate::getStatus, 0)
                        .isNotNull(NursingTaskTemplate::getNextTriggerTime)
                        .le(NursingTaskTemplate::getNextTriggerTime, now)
                        .and(w -> w.isNull(NursingTaskTemplate::getStartDate).or().le(NursingTaskTemplate::getStartDate, now.toLocalDate()))
                        .and(w -> w.isNull(NursingTaskTemplate::getEndDate).or().ge(NursingTaskTemplate::getEndDate, now.toLocalDate()))
                        .orderByAsc(NursingTaskTemplate::getNextTriggerTime)
        );

        if (!dueTemplates.isEmpty()) {
            log.info("[TaskTemplate] 自动调度扫描命中到期模板: dueTemplateCount={}, scanTime={}", dueTemplates.size(), now);
        }

        int generatedCount = 0;
        for (NursingTaskTemplate template : dueTemplates) {
            try {
                generatedCount += generateDueTasks(template, now);
            } catch (Exception e) {
                log.error("[TaskTemplate] 模板 {} 自动生成失败: {}", template.getId(), e.getMessage(), e);
            }
        }
        return generatedCount;
    }

    /**
     * 方法：listActiveTemplates
     *
     * @author zhanghongyu
     */
    @Override
    public List<NursingTaskTemplate> listActiveTemplates() {
        LocalDate today = LocalDate.now();
        return templateMapper.selectList(
                new LambdaQueryWrapper<NursingTaskTemplate>()
                        .eq(NursingTaskTemplate::getEnabled, 1)
                        .eq(NursingTaskTemplate::getStatus, 0)
                        .and(w -> w.isNull(NursingTaskTemplate::getStartDate).or().le(NursingTaskTemplate::getStartDate, today))
                        .and(w -> w.isNull(NursingTaskTemplate::getEndDate).or().ge(NursingTaskTemplate::getEndDate, today))
        );
    }

    /**
     * 方法：generateDueTasks
     *
     * @author zhanghongyu
     */
    private int generateDueTasks(NursingTaskTemplate template, LocalDateTime now) {
        if (template.getAssigneeUserId() == null) {
            log.warn("[TaskTemplate] 模板 {} 未指定执行人，跳过自动生成", template.getId());
            return 0;
        }

        TaskTemplateScheduleConfigDto config = readScheduleConfig(template);
        LocalDateTime nextTrigger = template.getNextTriggerTime();
        LocalDateTime lastTriggered = template.getLastTriggerTime();
        int count = 0;
        int guard = 0;

        while (nextTrigger != null && !nextTrigger.isAfter(now) && guard < MAX_CATCH_UP_COUNT) {
            insertTaskFromTemplate(template, nextTrigger);
            lastTriggered = nextTrigger;
            nextTrigger = TaskTemplateScheduleSupport.calculateNextTrigger(
                    config,
                    template.getStartDate(),
                    template.getEndDate(),
                    template.getTimezone(),
                    nextTrigger
            );
            count++;
            guard++;
        }

        if (guard >= MAX_CATCH_UP_COUNT) {
            log.warn("[TaskTemplate] 模板 {} 补偿生成达到上限，已中断本轮推进", template.getId());
        }

        if (count > 0) {
            template.setLastTriggerTime(lastTriggered);
            template.setNextTriggerTime(nextTrigger);
            template.setVersion(template.getVersion() == null ? 1 : template.getVersion() + 1);
            template.setUpdateTime(LocalDateTime.now());
            templateMapper.updateById(template);
            log.info("[TaskTemplate] 模板自动推进成功: templateId={}, generatedCount={}, lastTriggerTime={}, nextTriggerTime={}, version={}",
                    template.getId(),
                    count,
                    template.getLastTriggerTime(),
                    template.getNextTriggerTime(),
                    template.getVersion());
        }
        return count;
    }

    /**
     * 方法：insertTaskFromTemplate
     *
     * @author zhanghongyu
     */
    private void insertTaskFromTemplate(NursingTaskTemplate template, LocalDateTime executionTime) {
        LocalDateTime startTime = executionTime.withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusMinutes(resolvePlannedDuration(template.getPlannedDuration()));

        NursingTask task = new NursingTask();
        task.setSanaId(template.getSanaId());
        task.setTemplateId(template.getId());
        task.setElderId(template.getElderId());
        task.setTaskTitle(template.getTaskTitle());
        task.setTaskContent(template.getTaskContent());
        task.setTaskType(template.getTaskType());
        task.setPriority(template.getPriority());
        task.setAssigneeUserId(template.getAssigneeUserId());
        task.setAssignerUserId(null);
        task.setPlannedStartTime(startTime);
        task.setPlannedEndTime(endTime);
        task.setStatus(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        boolean inserted = nursingTaskRepository.insert(task);
        if (inserted) {
            log.info("[TaskTemplate] 护理任务生成成功: taskId={}, templateId={}, sanaId={}, assigneeUserId={}, executionTime={}, plannedStartTime={}, plannedEndTime={}",
                    task.getId(),
                    template.getId(),
                    template.getSanaId(),
                    template.getAssigneeUserId(),
                    executionTime,
                    task.getPlannedStartTime(),
                    task.getPlannedEndTime());
        } else {
            log.error("[TaskTemplate] 护理任务生成失败: templateId={}, sanaId={}, assigneeUserId={}, executionTime={}",
                    template.getId(),
                    template.getSanaId(),
                    template.getAssigneeUserId(),
                    executionTime);
        }
    }

    /**
     * 方法：hydrateScheduleView
     *
     * @author zhanghongyu
     */
    private void hydrateScheduleView(NursingTaskTemplate template) {
        try {
            TaskTemplateScheduleConfigDto config = readScheduleConfig(template);
            template.setScheduleConfig(config);
            template.setScheduleDescription(TaskTemplateScheduleSupport.buildScheduleDescription(config));
        } catch (Exception e) {
            log.warn("[TaskTemplate] 模板 {} 调度配置解析失败: {}", template.getId(), e.getMessage());
            template.setScheduleDescription("调度配置解析失败");
        }
        template.setNextExecuteTime(template.getNextTriggerTime());
    }

    /**
     * 方法：validateAndPrepareTemplate
     *
     * @author zhanghongyu
     */
    private PreparedSchedule validateAndPrepareTemplate(TaskTemplateCreateDto dto) {
        if (!StringUtils.hasText(dto.getTaskTitle())) {
            throw new ProjectException(400, "任务标题不能为空");
        }
        TaskTemplateScheduleSupport.validateDateRange(dto.getStartDate(), dto.getEndDate());

        TaskTemplateScheduleConfigDto config = TaskTemplateScheduleSupport.normalizeAndValidate(dto.getScheduleConfig());
        String timezone = TaskTemplateScheduleSupport.normalizeTimezone(dto.getTimezone());
        LocalDateTime nextTriggerTime = TaskTemplateScheduleSupport.calculateNextTrigger(
                config,
                dto.getStartDate(),
                dto.getEndDate(),
                timezone,
                LocalDateTime.now()
        );

        try {
            String configJson = objectMapper.writeValueAsString(config);
            return new PreparedSchedule(config, configJson, timezone, nextTriggerTime);
        } catch (JsonProcessingException e) {
            throw new ProjectException(500, "调度规则序列化失败");
        }
    }

    /**
     * 方法：readScheduleConfig
     *
     * @author zhanghongyu
     */
    private TaskTemplateScheduleConfigDto readScheduleConfig(NursingTaskTemplate template) {
        if (!StringUtils.hasText(template.getScheduleConfigJson())) {
            throw new ProjectException(500, "模板缺少结构化调度配置");
        }
        try {
            TaskTemplateScheduleConfigDto parsed = objectMapper.readValue(
                    template.getScheduleConfigJson(),
                    TaskTemplateScheduleConfigDto.class
            );
            return TaskTemplateScheduleSupport.normalizeAndValidate(parsed);
        } catch (JsonProcessingException e) {
            throw new ProjectException(500, "模板调度配置解析失败");
        }
    }

    private void fillTemplate(NursingTaskTemplate template,
                              TaskTemplateCreateDto dto,
                              Long targetSanaId,
                              PreparedSchedule prepared) {
        template.setSanaId(targetSanaId);
        template.setTaskTitle(dto.getTaskTitle().trim());
        template.setTaskContent(dto.getTaskContent());
        template.setTaskType(dto.getTaskType() != null ? dto.getTaskType() : 0);
        template.setPriority(dto.getPriority() != null ? dto.getPriority() : 2);
        template.setElderId(dto.getElderId());
        template.setAssigneeUserId(dto.getAssigneeUserId());
        template.setScheduleType(prepared.config().getScheduleType().name());
        template.setTimeMode(prepared.config().getTimeMode().name());
        template.setScheduleConfigJson(prepared.configJson());
        template.setTimezone(prepared.timezone());
        template.setNextTriggerTime(prepared.nextTriggerTime());
        template.setPlannedDuration(resolvePlannedDuration(dto.getPlannedDuration()));
        template.setStartDate(dto.getStartDate());
        template.setEndDate(dto.getEndDate());
        template.setRemark(dto.getRemark());
    }

    /**
     * 方法：resolvePlannedDuration
     *
     * @author zhanghongyu
     */
    private int resolvePlannedDuration(Integer plannedDuration) {
        return plannedDuration != null ? plannedDuration : DEFAULT_PLANNED_DURATION;
    }

    /**
     * 方法：resolveTargetSanaId
     *
     * @author zhanghongyu
     */
    private Long resolveTargetSanaId(Long requestedSanaId, Set<Long> scopeIds) {
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            if (requestedSanaId == null) {
                throw new ProjectException(400, "请指定目标机构");
            }
            return requestedSanaId;
        }
        if (requestedSanaId != null) {
            if (!scopeIds.contains(requestedSanaId)) {
                throw new ProjectException(403, "无权操作其他机构数据");
            }
            return requestedSanaId;
        }
        if (scopeIds.size() == 1) {
            return scopeIds.iterator().next();
        }
        throw new ProjectException(400, "当前账号已授权多个机构，请指定目标机构");
    }

    /**
     * 方法：ensureTemplateScope
     *
     * @author zhanghongyu
     */
    private void ensureTemplateScope(NursingTaskTemplate template) {
        if (template == null || securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            return;
        }
        Set<Long> scopeIds = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
        if (!scopeIds.contains(template.getSanaId())) {
            throw new ProjectException(403, "无权操作其他机构数据");
        }
    }

    /**
     * 方法：requireResource
     *
     * @author zhanghongyu
     */
    private void requireResource(String scene, String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            log.warn("{} forbidden: missing resource permission, userId={}, resourcePath={}",
                    scene, securityHelper.getCurrentUserId(), resourcePath);
            throw new ProjectException(403, "无权限执行该操作");
        }
    }

    /**
     * 方法：ensureOrgManagerRole
     *
     * @author zhanghongyu
     */
    private void ensureOrgManagerRole(String scene) {
        boolean hasRole = securityHelper.hasAnyRoleForSensitiveOperation(
                RoleConstant.GOV_ADMIN,
                RoleConstant.PARENT_ORG_ADMIN,
                RoleConstant.ORG_ADMIN
        );
        if (!hasRole) {
            log.warn("{} forbidden: insufficient role, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "无权操作护理任务模板");
        }
    }

    private record PreparedSchedule(
            TaskTemplateScheduleConfigDto config,
            String configJson,
            String timezone,
            LocalDateTime nextTriggerTime
    ) {
    }
}
