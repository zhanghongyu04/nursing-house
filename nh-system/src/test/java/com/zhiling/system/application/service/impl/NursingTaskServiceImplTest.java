package com.zhiling.system.application.service.impl;

import com.zhiling.common.constant.RoleConstant;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.framework.security.CurrentUserProvider;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.security.model.AccessScope;
import com.zhiling.model.dto.NursingTaskUpdateDto;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.system.application.repository.ElderRepository;
import com.zhiling.system.application.repository.NursingTaskRepository;
import com.zhiling.system.application.repository.UserRepository;
import com.zhiling.system.application.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 护理任务服务实现单元测试
 *
 * @author zhanghongyu
 */
@ExtendWith(MockitoExtension.class)
class NursingTaskServiceImplTest {

    @Mock
    private NursingTaskRepository nursingTaskRepository;

    @Mock
    private ElderRepository elderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private NursingTaskServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityHelper securityHelper = new SecurityHelper(currentUserProvider);
        service = new NursingTaskServiceImpl(
                nursingTaskRepository,
                elderRepository,
                userRepository,
                roleService,
                securityHelper
        );
    }

    @Test
    void shouldMarkOverdueTasksSuccessfully() {
        when(nursingTaskRepository.markOverdueTasks(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(4)))
                .thenReturn(3);

        int count = service.markOverdueTasks();

        assertThat(count).isEqualTo(3);
        verify(nursingTaskRepository).markOverdueTasks(org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.eq(4));
    }

    @Test
    void shouldCompleteOverdueTaskForCurrentNurse() {
        NursingTask task = new NursingTask();
        task.setId(100L);
        task.setSanaId(149L);
        task.setAssigneeUserId(17L);
        task.setStatus(4);

        when(currentUserProvider.currentAccessScope()).thenReturn(Optional.of(
                AccessScope.builder()
                        .userId(17L)
                        .sanaId(149L)
                        .sanaScopeIds(Set.of(149L))
                        .roleLabels(Set.of(RoleConstant.NURSE))
                        .build()
        ));
        when(nursingTaskRepository.selectById(100L)).thenReturn(task);
        when(nursingTaskRepository.updateById(org.mockito.ArgumentMatchers.any(NursingTask.class))).thenReturn(true);

        boolean result = service.complete(100L);

        ArgumentCaptor<NursingTask> patchCaptor = ArgumentCaptor.forClass(NursingTask.class);
        verify(nursingTaskRepository).updateById(patchCaptor.capture());
        assertThat(result).isTrue();
        assertThat(patchCaptor.getValue().getStatus()).isEqualTo(2);
        assertThat(patchCaptor.getValue().getCompletionTime()).isNotNull();
    }

    @Test
    void shouldResetOverdueTaskToPendingWhenPlannedEndTimeExtended() {
        NursingTask existing = new NursingTask();
        existing.setId(200L);
        existing.setSanaId(149L);
        existing.setStatus(4);
        existing.setPlannedEndTime(LocalDateTime.now().minusHours(1));

        NursingTaskUpdateDto updateDto = NursingTaskUpdateDto.builder()
                .id(200L)
                .plannedEndTime(LocalDateTime.now().plusHours(2))
                .build();

        when(currentUserProvider.currentAccessScope()).thenReturn(Optional.of(
                AccessScope.builder()
                        .userId(18L)
                        .sanaId(149L)
                        .sanaScopeIds(Set.of(149L))
                        .roleLabels(Set.of(RoleConstant.ORG_ADMIN))
                        .build()
        ));
        when(nursingTaskRepository.selectById(200L)).thenReturn(existing);
        when(nursingTaskRepository.updateById(org.mockito.ArgumentMatchers.any(NursingTask.class))).thenReturn(true);

        boolean result = service.update(updateDto);

        ArgumentCaptor<NursingTask> patchCaptor = ArgumentCaptor.forClass(NursingTask.class);
        verify(nursingTaskRepository).updateById(patchCaptor.capture());
        assertThat(result).isTrue();
        assertThat(patchCaptor.getValue().getStatus()).isEqualTo(0);
        assertThat(patchCaptor.getValue().getCompletionTime()).isNull();
    }

    @Test
    void shouldRejectManualOverdueStatusOnUpdate() {
        NursingTask existing = new NursingTask();
        existing.setId(201L);
        existing.setSanaId(149L);
        existing.setStatus(0);

        NursingTaskUpdateDto updateDto = NursingTaskUpdateDto.builder()
                .id(201L)
                .status(4)
                .build();

        when(currentUserProvider.currentAccessScope()).thenReturn(Optional.of(
                AccessScope.builder()
                        .userId(18L)
                        .sanaId(149L)
                        .sanaScopeIds(Set.of(149L))
                        .roleLabels(Set.of(RoleConstant.ORG_ADMIN))
                        .build()
        ));
        when(nursingTaskRepository.selectById(201L)).thenReturn(existing);

        assertThatThrownBy(() -> service.update(updateDto))
                .isInstanceOf(ProjectException.class)
                .satisfies(ex -> {
                    ProjectException projectException = (ProjectException) ex;
                    assertThat(projectException.getCode()).isEqualTo(400);
                    assertThat(projectException.getMessage()).isEqualTo("任务状态非法，仅支持0/1/2/3，超时状态由系统自动维护");
                });

        verify(nursingTaskRepository, never()).updateById(org.mockito.ArgumentMatchers.any(NursingTask.class));
    }

    @Test
    void shouldSoftDeletePendingTaskForOrgAdmin() {
        NursingTask existing = new NursingTask();
        existing.setId(300L);
        existing.setSanaId(149L);
        existing.setStatus(0);
        existing.setDeleted(0);

        when(currentUserProvider.currentAccessScope()).thenReturn(Optional.of(
                AccessScope.builder()
                        .userId(18L)
                        .sanaId(149L)
                        .sanaScopeIds(Set.of(149L))
                        .roleLabels(Set.of(RoleConstant.ORG_ADMIN))
                        .build()
        ));
        when(nursingTaskRepository.selectById(300L)).thenReturn(existing);
        when(nursingTaskRepository.deleteById(300L)).thenReturn(true);

        boolean result = service.remove(300L);

        assertThat(result).isTrue();
        verify(nursingTaskRepository).deleteById(300L);
    }
}
