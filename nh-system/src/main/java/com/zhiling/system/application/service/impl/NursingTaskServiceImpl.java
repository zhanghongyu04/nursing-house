package com.zhiling.system.application.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.common.constant.RoleConstant;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.common.result.PageResult;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.dto.NursingTaskDispatchDto;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.dto.NursingTaskUpdateDto;
import com.zhiling.model.entity.Elder;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.model.entity.Role;
import com.zhiling.model.entity.User;
import com.zhiling.system.application.repository.ElderRepository;
import com.zhiling.system.application.repository.NursingTaskRepository;
import com.zhiling.system.application.repository.UserRepository;
import com.zhiling.system.application.service.NursingTaskService;
import com.zhiling.system.application.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 护理任务服务实现
 *
 * @author zhanghongyu
 */
@Service
public class NursingTaskServiceImpl implements NursingTaskService {
    private static final Logger log = LoggerFactory.getLogger(NursingTaskServiceImpl.class);

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_COMPLETED = 2;
    private static final int STATUS_CANCELED = 3;
    private static final int STATUS_OVERDUE = 4;
    private static final int DELETED_NO = 0;

    private static final int DEFAULT_TASK_TYPE = 0;
    private static final int DEFAULT_PRIORITY = 1;
    private static final String TASK_PAGE_PATH = "/web/nursing-task/page";
    private static final String TASK_DISPATCH_PATH = "/web/nursing-task/dispatch";
    private static final String TASK_UPDATE_PATH = "/web/nursing-task/update";
    private static final String TASK_CANCEL_PATH = "/web/nursing-task/cancel/**";
    private static final String TASK_REACTIVATE_PATH = "/web/nursing-task/reactivate/**";
    private static final String TASK_REMOVE_PATH = "/web/nursing-task/remove/**";
    private static final String TASK_MY_PAGE_PATH = "/web/nursing-task/my/page";
    private static final String TASK_COMPLETE_PATH = "/web/nursing-task/complete/**";

    private final NursingTaskRepository nursingTaskRepository;
    private final ElderRepository elderRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final SecurityHelper securityHelper;

    public NursingTaskServiceImpl(NursingTaskRepository nursingTaskRepository,
                                  ElderRepository elderRepository,
                                  UserRepository userRepository,
                                  RoleService roleService,
                                  SecurityHelper securityHelper) {
        this.nursingTaskRepository = nursingTaskRepository;
        this.elderRepository = elderRepository;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.securityHelper = securityHelper;
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult page(NursingTaskPageQueryDto queryDto) {
        try {
            requireResource("task.page", TASK_PAGE_PATH);
            ensureOrgManagerRole("task.page");
            Set<Long> scopeIds = requireScopeIds("task.page");

            NursingTaskPageQueryDto safeDto = queryDto == null ? new NursingTaskPageQueryDto() : queryDto;
            if (safeDto.getSanaId() != null && !scopeIds.contains(safeDto.getSanaId())) {
                log.warn("task.page forbidden: userId={}, requestSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), safeDto.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权查询其他机构护理任务");
            }
            safeDto.setSanaScopeIds(scopeIds);

            Page<NursingTask> page = new Page<>(resolvePageNo(safeDto.getPage()), resolvePageSize(safeDto.getPageSize()));
            IPage<NursingTask> result = nursingTaskRepository.page(page, safeDto);
            return new PageResult(result.getTotal(), result.getRecords());
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("task.page failed, query={}", queryDto, e);
            throw new ProjectException(500, "护理任务分页查询失败");
        }
    }

    /**
     * 方法：dispatch
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean dispatch(NursingTaskDispatchDto dispatchDto) {
        try {
            requireResource("task.dispatch", TASK_DISPATCH_PATH);
            ensureOrgManagerRole("task.dispatch");
            if (dispatchDto == null) {
                throw new ProjectException(400, "请求参数不能为空");
            }
            if (!StringUtils.hasText(dispatchDto.getTaskTitle())) {
                throw new ProjectException(400, "任务标题不能为空");
            }

            Set<Long> scopeIds = requireScopeIds("task.dispatch");
            Long targetSanaId = resolveTargetSanaId(dispatchDto.getSanaId(), scopeIds, "task.dispatch");
            validateTaskTimeRange(dispatchDto.getPlannedStartTime(), dispatchDto.getPlannedEndTime(), "task.dispatch");
            validateElderInSana(dispatchDto.getElderId(), targetSanaId, "task.dispatch");
            validateAssignee(dispatchDto.getAssigneeUserId(), targetSanaId, "task.dispatch");

            Integer status = dispatchDto.getStatus() == null ? STATUS_PENDING : dispatchDto.getStatus();
            validateManualStatus(status, "task.dispatch");

            Long currentUserId = securityHelper.requireCurrentUserId();
            NursingTask nursingTask = new NursingTask();
            nursingTask.setSanaId(targetSanaId);
            nursingTask.setElderId(dispatchDto.getElderId());
            nursingTask.setTaskTitle(dispatchDto.getTaskTitle());
            nursingTask.setTaskContent(dispatchDto.getTaskContent());
            nursingTask.setTaskType(dispatchDto.getTaskType() == null ? DEFAULT_TASK_TYPE : dispatchDto.getTaskType());
            nursingTask.setPriority(dispatchDto.getPriority() == null ? DEFAULT_PRIORITY : dispatchDto.getPriority());
            nursingTask.setAssigneeUserId(dispatchDto.getAssigneeUserId());
            nursingTask.setAssignerUserId(currentUserId);
            nursingTask.setPlannedStartTime(dispatchDto.getPlannedStartTime());
            nursingTask.setPlannedEndTime(dispatchDto.getPlannedEndTime());
            nursingTask.setStatus(status);
            nursingTask.setDeleted(DELETED_NO);
            nursingTask.setRemark(dispatchDto.getRemark());
            nursingTask.setCompletionTime(status == STATUS_COMPLETED ? LocalDateTime.now() : null);

            boolean ok = nursingTaskRepository.insert(nursingTask);
            if (!ok) {
                throw new ProjectException(500, "护理任务下发失败");
            }
            log.info("task.dispatch success: operatorUserId={}, taskId={}, sanaId={}, elderId={}, assigneeUserId={}, status={}, plannedStartTime={}, plannedEndTime={}",
                    currentUserId,
                    nursingTask.getId(),
                    nursingTask.getSanaId(),
                    nursingTask.getElderId(),
                    nursingTask.getAssigneeUserId(),
                    nursingTask.getStatus(),
                    nursingTask.getPlannedStartTime(),
                    nursingTask.getPlannedEndTime());
            return true;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("task.dispatch failed, dto={}", dispatchDto, e);
            throw new ProjectException(500, "护理任务下发失败");
        }
    }

    /**
     * 方法：update
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean update(NursingTaskUpdateDto updateDto) {
        try {
            requireResource("task.update", TASK_UPDATE_PATH);
            ensureOrgManagerRole("task.update");
            if (updateDto == null || updateDto.getId() == null) {
                throw new ProjectException(400, "任务ID不能为空");
            }

            NursingTask existing = nursingTaskRepository.selectById(updateDto.getId());
            if (existing == null) {
                throw new ProjectException(404, "护理任务不存在");
            }

            Set<Long> scopeIds = requireScopeIds("task.update");
            if (!scopeIds.contains(existing.getSanaId())) {
                log.warn("task.update forbidden: userId={}, taskId={}, taskSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), updateDto.getId(), existing.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权修改其他机构护理任务");
            }

            LocalDateTime plannedStart = updateDto.getPlannedStartTime() != null
                    ? updateDto.getPlannedStartTime() : existing.getPlannedStartTime();
            LocalDateTime plannedEnd = updateDto.getPlannedEndTime() != null
                    ? updateDto.getPlannedEndTime() : existing.getPlannedEndTime();
            validateTaskTimeRange(plannedStart, plannedEnd, "task.update");

            if (updateDto.getElderId() != null) {
                validateElderInSana(updateDto.getElderId(), existing.getSanaId(), "task.update");
            }
            if (updateDto.getAssigneeUserId() != null) {
                validateAssignee(updateDto.getAssigneeUserId(), existing.getSanaId(), "task.update");
            }
            if (updateDto.getStatus() != null) {
                validateManualStatus(updateDto.getStatus(), "task.update");
            }

            Integer resolvedStatus = resolveStatusForUpdate(existing, updateDto, plannedEnd);
            NursingTask patch = new NursingTask();
            patch.setId(updateDto.getId());
            patch.setElderId(updateDto.getElderId());
            patch.setAssigneeUserId(updateDto.getAssigneeUserId());
            patch.setTaskTitle(updateDto.getTaskTitle());
            patch.setTaskContent(updateDto.getTaskContent());
            patch.setTaskType(updateDto.getTaskType());
            patch.setPriority(updateDto.getPriority());
            patch.setPlannedStartTime(updateDto.getPlannedStartTime());
            patch.setPlannedEndTime(updateDto.getPlannedEndTime());
            patch.setStatus(resolvedStatus);
            patch.setRemark(updateDto.getRemark());
            patch.setCompletionTime(resolveCompletionTime(updateDto, resolvedStatus));

            boolean ok = nursingTaskRepository.updateById(patch);
            if (!ok) {
                throw new ProjectException(500, "护理任务更新失败");
            }
            log.info("task.update success: operatorUserId={}, taskId={}, fromStatus={}, toStatus={}, assigneeUserId={}, plannedStartTime={}, plannedEndTime={}",
                    securityHelper.getCurrentUserId(),
                    updateDto.getId(),
                    existing.getStatus(),
                    resolvedStatus,
                    patch.getAssigneeUserId() != null ? patch.getAssigneeUserId() : existing.getAssigneeUserId(),
                    patch.getPlannedStartTime() != null ? patch.getPlannedStartTime() : existing.getPlannedStartTime(),
                    patch.getPlannedEndTime() != null ? patch.getPlannedEndTime() : existing.getPlannedEndTime());
            return true;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("task.update failed, dto={}", updateDto, e);
            throw new ProjectException(500, "护理任务更新失败");
        }
    }

    /**
     * 方法：cancel
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean cancel(Long id) {
        try {
            requireResource("task.cancel", TASK_CANCEL_PATH);
            ensureOrgManagerRole("task.cancel");
            if (id == null) {
                throw new ProjectException(400, "任务ID不能为空");
            }

            NursingTask existing = nursingTaskRepository.selectById(id);
            if (existing == null) {
                throw new ProjectException(404, "护理任务不存在");
            }

            Set<Long> scopeIds = requireScopeIds("task.cancel");
            if (!scopeIds.contains(existing.getSanaId())) {
                log.warn("task.cancel forbidden: userId={}, taskId={}, taskSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), id, existing.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权取消其他机构护理任务");
            }
            if (Objects.equals(existing.getStatus(), STATUS_COMPLETED)) {
                throw new ProjectException(409, "已完成任务不可取消");
            }
            if (Objects.equals(existing.getStatus(), STATUS_CANCELED)) {
                log.info("task.cancel skipped: operatorUserId={}, taskId={}, status={}",
                        securityHelper.getCurrentUserId(), id, existing.getStatus());
                return true;
            }

            NursingTask patch = new NursingTask();
            patch.setId(id);
            patch.setStatus(STATUS_CANCELED);
            boolean ok = nursingTaskRepository.updateById(patch);
            if (!ok) {
                throw new ProjectException(500, "护理任务取消失败");
            }
            log.info("task.cancel success: operatorUserId={}, taskId={}, fromStatus={}, toStatus={}",
                    securityHelper.getCurrentUserId(), id, existing.getStatus(), STATUS_CANCELED);
            return true;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("task.cancel failed, id={}", id, e);
            throw new ProjectException(500, "护理任务取消失败");
        }
    }

    /**
     * 方法：reactivate
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean reactivate(Long id) {
        try {
            requireResource("task.reactivate", TASK_REACTIVATE_PATH);
            ensureOrgManagerRole("task.reactivate");
            if (id == null) {
                throw new ProjectException(400, "任务ID不能为空");
            }

            NursingTask existing = nursingTaskRepository.selectById(id);
            if (existing == null) {
                throw new ProjectException(404, "护理任务不存在");
            }

            Set<Long> scopeIds = requireScopeIds("task.reactivate");
            if (!scopeIds.contains(existing.getSanaId())) {
                log.warn("task.reactivate forbidden: userId={}, taskId={}, taskSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), id, existing.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权激活其他机构护理任务");
            }
            if (Objects.equals(existing.getStatus(), STATUS_COMPLETED)) {
                throw new ProjectException(409, "已完成任务不可激活");
            }
            if (!Objects.equals(existing.getStatus(), STATUS_CANCELED)) {
                throw new ProjectException(409, "仅已取消任务可重新激活");
            }

            Integer targetStatus = isTaskOverdue(existing.getPlannedEndTime(), LocalDateTime.now())
                    ? STATUS_OVERDUE : STATUS_PENDING;
            NursingTask patch = new NursingTask();
            patch.setId(id);
            patch.setStatus(targetStatus);
            patch.setCompletionTime(null);
            boolean ok = nursingTaskRepository.updateById(patch);
            if (!ok) {
                throw new ProjectException(500, "护理任务激活失败");
            }
            log.info("task.reactivate success: userId={}, taskId={}, fromStatus={}, toStatus={}",
                    securityHelper.getCurrentUserId(), id, existing.getStatus(), targetStatus);
            return true;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("task.reactivate failed, id={}", id, e);
            throw new ProjectException(500, "护理任务激活失败");
        }
    }

    /**
     * 方法：remove
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean remove(Long id) {
        try {
            requireResource("task.remove", TASK_REMOVE_PATH);
            ensureOrgManagerRole("task.remove");
            if (id == null) {
                throw new ProjectException(400, "任务ID不能为空");
            }

            NursingTask existing = nursingTaskRepository.selectById(id);
            if (existing == null) {
                throw new ProjectException(404, "护理任务不存在");
            }

            Set<Long> scopeIds = requireScopeIds("task.remove");
            if (!scopeIds.contains(existing.getSanaId())) {
                log.warn("task.remove forbidden: userId={}, taskId={}, taskSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), id, existing.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权删除其他机构护理任务");
            }
            if (!isRemovableStatus(existing.getStatus())) {
                throw new ProjectException(409, "仅待执行、已取消、已超时任务支持删除");
            }

            boolean ok = nursingTaskRepository.deleteById(id);
            if (!ok) {
                throw new ProjectException(500, "护理任务删除失败");
            }
            log.info("task.remove success: userId={}, taskId={}, status={}",
                    securityHelper.getCurrentUserId(), id, existing.getStatus());
            return true;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("task.remove failed, id={}", id, e);
            throw new ProjectException(500, "护理任务删除失败");
        }
    }

    /**
     * 方法：myPage
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult myPage(NursingTaskMyPageQueryDto queryDto) {
        try {
            requireResource("task.myPage", TASK_MY_PAGE_PATH);
            ensureNurseRole("task.myPage");
            Set<Long> scopeIds = requireScopeIds("task.myPage");

            NursingTaskMyPageQueryDto safeDto = queryDto == null ? new NursingTaskMyPageQueryDto() : queryDto;
            if (safeDto.getSanaId() != null && !scopeIds.contains(safeDto.getSanaId())) {
                log.warn("task.myPage forbidden: userId={}, requestSanaId={}, scopeIds={}",
                        securityHelper.getCurrentUserId(), safeDto.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权查询其他机构护理任务");
            }

            safeDto.setAssigneeUserId(securityHelper.requireCurrentUserId());
            safeDto.setSanaScopeIds(scopeIds);

            Page<NursingTask> page = new Page<>(resolvePageNo(safeDto.getPage()), resolvePageSize(safeDto.getPageSize()));
            IPage<NursingTask> result = nursingTaskRepository.myPage(page, safeDto);
            return new PageResult(result.getTotal(), result.getRecords());
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("task.myPage failed, query={}", queryDto, e);
            throw new ProjectException(500, "我的护理任务分页查询失败");
        }
    }

    /**
     * 方法：complete
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean complete(Long id) {
        try {
            requireResource("task.complete", TASK_COMPLETE_PATH);
            ensureNurseRole("task.complete");
            if (id == null) {
                throw new ProjectException(400, "任务ID不能为空");
            }

            Long currentUserId = securityHelper.requireCurrentUserId();
            Set<Long> scopeIds = requireScopeIds("task.complete");
            NursingTask existing = nursingTaskRepository.selectById(id);
            if (existing == null) {
                throw new ProjectException(404, "护理任务不存在");
            }
            if (!scopeIds.contains(existing.getSanaId())) {
                log.warn("task.complete forbidden: userId={}, taskId={}, taskSanaId={}, scopeIds={}",
                        currentUserId, id, existing.getSanaId(), scopeIds);
                throw new ProjectException(403, "无权操作其他机构护理任务");
            }
            if (!Objects.equals(currentUserId, existing.getAssigneeUserId())) {
                log.warn("task.complete forbidden: userId={}, taskId={}, assigneeUserId={}",
                        currentUserId, id, existing.getAssigneeUserId());
                throw new ProjectException(403, "仅可完成本人护理任务");
            }
            if (Objects.equals(existing.getStatus(), STATUS_CANCELED)) {
                throw new ProjectException(409, "已取消任务不可完成");
            }
            if (Objects.equals(existing.getStatus(), STATUS_COMPLETED)) {
                return true;
            }
            if (Objects.equals(existing.getStatus(), STATUS_OVERDUE)) {
                log.info("task.complete skip-overdue: userId={}, taskId={}, fromStatus={}, treatAsComplete=true",
                        currentUserId, id, existing.getStatus());
            }

            NursingTask patch = new NursingTask();
            patch.setId(id);
            patch.setStatus(STATUS_COMPLETED);
            patch.setCompletionTime(LocalDateTime.now());
            boolean ok = nursingTaskRepository.updateById(patch);
            if (!ok) {
                throw new ProjectException(500, "护理任务完成失败");
            }
            log.info("task.complete success: userId={}, taskId={}, fromStatus={}, toStatus={}, completionTime={}",
                    currentUserId, id, existing.getStatus(), STATUS_COMPLETED, patch.getCompletionTime());
            return true;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            log.error("task.complete failed, id={}", id, e);
            throw new ProjectException(500, "护理任务完成失败");
        }
    }

    /**
     * 方法：syncTaskExecutionStatus
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public int syncTaskExecutionStatus() {
        LocalDateTime now = LocalDateTime.now();
        try {
            int runningAffectedCount = nursingTaskRepository.markRunningTasks(now, STATUS_RUNNING);
            int overdueAffectedCount = nursingTaskRepository.markOverdueTasks(now, STATUS_OVERDUE);
            int totalAffected = runningAffectedCount + overdueAffectedCount;
            log.info("[TaskStatusSync] 状态扫描完成: runningAffectedCount={}, overdueAffectedCount={}, totalAffectedCount={}, scanTime={}",
                    runningAffectedCount, overdueAffectedCount, totalAffected, now);
            return totalAffected;
        } catch (Exception e) {
            log.error("[TaskStatusSync] 状态扫描失败: scanTime={}", now, e);
            throw new ProjectException(500, "护理任务状态扫描失败");
        }
    }

    /**
     * 方法：markOverdueTasks
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public int markOverdueTasks() {
        LocalDateTime now = LocalDateTime.now();
        try {
            int affectedCount = nursingTaskRepository.markOverdueTasks(now, STATUS_OVERDUE);
            if (affectedCount > 0) {
                log.info("[TaskOverdue] 超时任务扫描完成: affectedCount={}, scanTime={}", affectedCount, now);
            }
            return affectedCount;
        } catch (Exception e) {
            log.error("[TaskOverdue] 超时任务扫描失败: scanTime={}", now, e);
            throw new ProjectException(500, "护理任务超时扫描失败");
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
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            log.warn("{} forbidden: GOV_ADMIN, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "政府管理员不可访问护理任务模块");
        }
        if (!securityHelper.hasAnyRoleForSensitiveOperation(RoleConstant.ORG_ADMIN, RoleConstant.PARENT_ORG_ADMIN)) {
            log.warn("{} forbidden: role not allowed, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "无权限执行该操作");
        }
    }

    /**
     * 方法：ensureNurseRole
     *
     * @author zhanghongyu
     */
    private void ensureNurseRole(String scene) {
        if (securityHelper.hasGovAdminRoleForSensitiveOperation()) {
            log.warn("{} forbidden: GOV_ADMIN, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "政府管理员不可访问护理任务模块");
        }
        if (!securityHelper.hasAnyRoleForSensitiveOperation(RoleConstant.NURSE)) {
            log.warn("{} forbidden: nurse role required, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "仅护理人员可执行该操作");
        }
    }

    /**
     * 方法：requireScopeIds
     *
     * @author zhanghongyu
     */
    private Set<Long> requireScopeIds(String scene) {
        try {
            return securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
        } catch (Exception e) {
            log.warn("{} forbidden: sana scope missing, userId={}", scene, securityHelper.getCurrentUserId());
            throw new ProjectException(403, "当前用户未绑定机构或无机构范围");
        }
    }

    /**
     * 方法：resolveTargetSanaId
     *
     * @author zhanghongyu
     */
    private Long resolveTargetSanaId(Long requestedSanaId, Set<Long> scopeIds, String scene) {
        if (requestedSanaId != null) {
            if (!scopeIds.contains(requestedSanaId)) {
                log.warn("{} forbidden: request sana out of scope, userId={}, requestSanaId={}, scopeIds={}",
                        scene, securityHelper.getCurrentUserId(), requestedSanaId, scopeIds);
                throw new ProjectException(403, "无权操作其他机构数据");
            }
            return requestedSanaId;
        }
        if (scopeIds.size() == 1) {
            return scopeIds.iterator().next();
        }
        log.warn("{} bad request: multi-scope user without sanaId, userId={}, scopeIds={}",
                scene, securityHelper.getCurrentUserId(), scopeIds);
        throw new ProjectException(400, "当前账号已授权多个机构，请指定 sanaId");
    }

    /**
     * 方法：validateAssignee
     *
     * @author zhanghongyu
     */
    private void validateAssignee(Long assigneeUserId, Long targetSanaId, String scene) {
        if (assigneeUserId == null) {
            throw new ProjectException(400, "执行人不能为空");
        }
        User assignee = userRepository.selectById(assigneeUserId);
        if (assignee == null || assignee.getStatus() == null || assignee.getStatus() != 0) {
            log.warn("{} not found: assignee invalid, assigneeUserId={}", scene, assigneeUserId);
            throw new ProjectException(404, "执行人不存在或已停用");
        }

        List<Long> assigneeScopeIds = userRepository.listSanaScopeIdsByUserId(assigneeUserId);
        if (assigneeScopeIds != null && !assigneeScopeIds.isEmpty()) {
            if (!assigneeScopeIds.contains(targetSanaId)) {
                log.warn("{} forbidden: assignee out of scope, assigneeUserId={}, targetSanaId={}, assigneeScopeIds={}",
                        scene, assigneeUserId, targetSanaId, assigneeScopeIds);
                throw new ProjectException(403, "执行人不在目标机构范围内");
            }
        } else if (assignee.getSanaId() == null || !targetSanaId.equals(assignee.getSanaId())) {
            log.warn("{} forbidden: assignee sana mismatch, assigneeUserId={}, targetSanaId={}, assigneeSanaId={}",
                    scene, assigneeUserId, targetSanaId, assignee.getSanaId());
            throw new ProjectException(403, "执行人不在目标机构范围内");
        }

        List<Role> roleList = roleService.getRoleListByUserId(String.valueOf(assigneeUserId));
        boolean isNurse = roleList != null && roleList.stream()
                .anyMatch(role -> RoleConstant.NURSE.equals(role.getLabel()));
        if (!isNurse) {
            throw new ProjectException(400, "执行人必须是护理人员");
        }
    }

    /**
     * 方法：validateElderInSana
     *
     * @author zhanghongyu
     */
    private void validateElderInSana(Long elderId, Long targetSanaId, String scene) {
        if (elderId == null) {
            return;
        }
        Elder elder = elderRepository.selectById(elderId);
        if (elder == null) {
            log.warn("{} not found: elderId={}", scene, elderId);
            throw new ProjectException(404, "关联老人不存在");
        }
        if (elder.getSanaId() == null || !targetSanaId.equals(elder.getSanaId())) {
            log.warn("{} forbidden: elder sana mismatch, elderId={}, targetSanaId={}, elderSanaId={}",
                    scene, elderId, targetSanaId, elder.getSanaId());
            throw new ProjectException(403, "关联老人不属于目标机构");
        }
    }

    /**
     * 方法：validateTaskTimeRange
     *
     * @author zhanghongyu
     */
    private void validateTaskTimeRange(LocalDateTime plannedStartTime, LocalDateTime plannedEndTime, String scene) {
        if (plannedStartTime == null || plannedEndTime == null) {
            return;
        }
        if (plannedEndTime.isBefore(plannedStartTime)) {
            log.warn("{} bad request: invalid time range, plannedStartTime={}, plannedEndTime={}",
                    scene, plannedStartTime, plannedEndTime);
            throw new ProjectException(400, "计划结束时间不能早于计划开始时间");
        }
    }

    /**
     * 方法：validateManualStatus
     *
     * @author zhanghongyu
     */
    private void validateManualStatus(Integer status, String scene) {
        if (status == null) {
            return;
        }
        if (status == STATUS_PENDING || status == STATUS_RUNNING || status == STATUS_COMPLETED || status == STATUS_CANCELED) {
            return;
        }
        log.warn("{} bad request: invalid status={}", scene, status);
        throw new ProjectException(400, "任务状态非法，仅支持0/1/2/3，超时状态由系统自动维护");
    }

    /**
     * 方法：resolveStatusForUpdate
     *
     * @author zhanghongyu
     */
    private Integer resolveStatusForUpdate(NursingTask existing, NursingTaskUpdateDto updateDto, LocalDateTime plannedEndTime) {
        Integer requestedStatus = updateDto.getStatus();
        Integer baseStatus = requestedStatus != null ? requestedStatus : existing.getStatus();
        if (baseStatus == null) {
            return requestedStatus;
        }
        if (baseStatus == STATUS_COMPLETED || baseStatus == STATUS_CANCELED) {
            return baseStatus;
        }
        boolean overdue = isTaskOverdue(plannedEndTime, LocalDateTime.now());
        if (overdue) {
            return STATUS_OVERDUE;
        }
        if (existing.getStatus() != null && existing.getStatus() == STATUS_OVERDUE && requestedStatus == null) {
            return STATUS_PENDING;
        }
        return baseStatus;
    }

    /**
     * 方法：isTaskOverdue
     *
     * @author zhanghongyu
     */
    private boolean isTaskOverdue(LocalDateTime plannedEndTime, LocalDateTime now) {
        return plannedEndTime != null && !plannedEndTime.isAfter(now);
    }

    /**
     * 方法：resolveCompletionTime
     *
     * @author zhanghongyu
     */
    private LocalDateTime resolveCompletionTime(NursingTaskUpdateDto updateDto, Integer resolvedStatus) {
        if (resolvedStatus == null) {
            return updateDto.getCompletionTime();
        }
        if (resolvedStatus == STATUS_COMPLETED) {
            return updateDto.getCompletionTime() != null ? updateDto.getCompletionTime() : LocalDateTime.now();
        }
        if (resolvedStatus == STATUS_PENDING
                || resolvedStatus == STATUS_RUNNING
                || resolvedStatus == STATUS_CANCELED
                || resolvedStatus == STATUS_OVERDUE) {
            return null;
        }
        return updateDto.getCompletionTime();
    }

    /**
     * 方法：isRemovableStatus
     *
     * @author zhanghongyu
     */
    private boolean isRemovableStatus(Integer status) {
        return Objects.equals(status, STATUS_PENDING)
                || Objects.equals(status, STATUS_CANCELED)
                || Objects.equals(status, STATUS_OVERDUE);
    }

    /**
     * 方法：resolvePageNo
     *
     * @author zhanghongyu
     */
    private long resolvePageNo(Integer pageNo) {
        return pageNo == null || pageNo <= 0 ? 1L : pageNo.longValue();
    }

    /**
     * 方法：resolvePageSize
     *
     * @author zhanghongyu
     */
    private long resolvePageSize(Integer pageSize) {
        return pageSize == null || pageSize <= 0 ? 10L : pageSize.longValue();
    }
}
