package com.zhiling.system.application.service.impl;

import com.zhiling.common.constant.RoleConstant;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.framework.security.CurrentUserProvider;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.security.model.AccessScope;
import com.zhiling.model.dto.NursingLogAddDto;
import com.zhiling.model.dto.NursingLogUpdateDto;
import com.zhiling.model.entity.NursingLog;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.system.application.repository.ElderRepository;
import com.zhiling.system.application.repository.NursingLogRepository;
import com.zhiling.system.application.repository.NursingTaskRepository;
import com.zhiling.system.application.service.CommonFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 护理日志服务实现单元测试
 *
 * @author zhanghongyu
 */
@ExtendWith(MockitoExtension.class)
class NursingLogServiceImplTest {

    @Mock
    private NursingLogRepository nursingLogRepository;

    @Mock
    private NursingTaskRepository nursingTaskRepository;

    @Mock
    private ElderRepository elderRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private CommonFileService commonFileService;

    private NursingLogServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityHelper securityHelper = new SecurityHelper(currentUserProvider);
        service = new NursingLogServiceImpl(
                nursingLogRepository,
                nursingTaskRepository,
                elderRepository,
                securityHelper,
                commonFileService
        );

        when(currentUserProvider.currentAccessScope()).thenReturn(Optional.of(
                AccessScope.builder()
                        .userId(17L)
                        .sanaId(149L)
                        .sanaScopeIds(Set.of(149L))
                        .roleLabels(Set.of(RoleConstant.NURSE))
                        .build()
        ));
    }

    @Test
    void shouldRejectAddWhenCurrentNurseAlreadyHasLogForTask() {
        NursingTask task = buildTask(100L);
        NursingLog existingLog = NursingLog.builder().id(200L).taskId(100L).nurseUserId(17L).build();
        NursingLogAddDto addDto = NursingLogAddDto.builder()
                .taskId(100L)
                .content("完成翻身护理")
                .logTime(LocalDateTime.of(2026, 4, 17, 14, 0))
                .build();

        when(nursingTaskRepository.selectById(100L)).thenReturn(task);
        when(nursingLogRepository.selectByTaskIdAndNurseUserId(100L, 17L)).thenReturn(existingLog);

        assertThatThrownBy(() -> service.add(addDto))
                .isInstanceOf(ProjectException.class)
                .satisfies(ex -> {
                    ProjectException projectException = (ProjectException) ex;
                    assertThat(projectException.getCode()).isEqualTo(409);
                    assertThat(projectException.getMessage()).isEqualTo("该任务已存在护理日志，请直接编辑原日志");
                });

        verify(nursingLogRepository, never()).insert(any(NursingLog.class));
    }

    @Test
    void shouldRejectUpdateWhenTargetTaskAlreadyHasAnotherLog() {
        NursingLog existingLog = NursingLog.builder()
                .id(1L)
                .sanaId(149L)
                .taskId(100L)
                .nurseUserId(17L)
                .content("原日志")
                .build();
        NursingTask targetTask = buildTask(101L);
        NursingLog duplicatedLog = NursingLog.builder().id(2L).taskId(101L).nurseUserId(17L).build();
        NursingLogUpdateDto updateDto = NursingLogUpdateDto.builder()
                .taskId(101L)
                .content("改成另一条任务日志")
                .build();

        when(nursingLogRepository.selectById(1L)).thenReturn(existingLog);
        when(nursingTaskRepository.selectById(101L)).thenReturn(targetTask);
        when(nursingLogRepository.selectByTaskIdAndNurseUserId(101L, 17L)).thenReturn(duplicatedLog);

        assertThatThrownBy(() -> service.update(1L, updateDto))
                .isInstanceOf(ProjectException.class)
                .satisfies(ex -> {
                    ProjectException projectException = (ProjectException) ex;
                    assertThat(projectException.getCode()).isEqualTo(409);
                    assertThat(projectException.getMessage()).isEqualTo("该任务已存在护理日志，请直接编辑原日志");
                });

        verify(nursingLogRepository, never()).updateById(any(NursingLog.class));
    }

    private NursingTask buildTask(Long taskId) {
        NursingTask task = new NursingTask();
        task.setId(taskId);
        task.setSanaId(149L);
        task.setAssigneeUserId(17L);
        task.setTaskTitle("翻身护理");
        return task;
    }
}
