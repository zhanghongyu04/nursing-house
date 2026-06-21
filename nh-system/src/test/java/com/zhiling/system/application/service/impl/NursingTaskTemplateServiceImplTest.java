package com.zhiling.system.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiling.common.constant.RoleConstant;
import com.zhiling.framework.security.CurrentUserProvider;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.security.model.AccessScope;
import com.zhiling.model.dto.TaskTemplateScheduleConfigDto;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.model.entity.NursingTaskTemplate;
import com.zhiling.model.enums.TaskTemplateScheduleType;
import com.zhiling.model.enums.TaskTemplateTimeMode;
import com.zhiling.system.application.repository.NursingTaskRepository;
import com.zhiling.system.infrastructure.persistence.mapper.NursingTaskTemplateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 护理任务模板服务实现单元测试
 *
 * @author zhanghongyu
 */
@ExtendWith(MockitoExtension.class)
class NursingTaskTemplateServiceImplTest {

    @Mock
    private NursingTaskTemplateMapper templateMapper;

    @Mock
    private NursingTaskRepository nursingTaskRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private NursingTaskTemplateServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SecurityHelper securityHelper = new SecurityHelper(currentUserProvider);
        objectMapper = new ObjectMapper();
        if (TableInfoHelper.getTableInfo(NursingTaskTemplate.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                    NursingTaskTemplate.class
            );
        }
        service = new NursingTaskTemplateServiceImpl(
                templateMapper,
                nursingTaskRepository,
                securityHelper,
                objectMapper
        );
    }

    @Test
    void shouldGenerateTaskManuallyWithoutUpdatingTemplateScheduleState() {
        NursingTaskTemplate template = buildTemplate();
        LocalDateTime originalNextTrigger = LocalDateTime.of(2026, 4, 19, 9, 0);
        LocalDateTime originalLastTrigger = LocalDateTime.of(2026, 4, 18, 9, 0);
        template.setNextTriggerTime(originalNextTrigger);
        template.setLastTriggerTime(originalLastTrigger);

        when(currentUserProvider.currentAccessScope()).thenReturn(Optional.of(
                AccessScope.builder()
                        .roleLabels(Set.of(RoleConstant.GOV_ADMIN))
                        .build()
        ));
        when(templateMapper.selectById(1L)).thenReturn(template);
        when(nursingTaskRepository.insert(any(NursingTask.class))).thenReturn(true);

        int count = service.generateTasksForTemplate(1L);

        ArgumentCaptor<NursingTask> taskCaptor = ArgumentCaptor.forClass(NursingTask.class);
        verify(nursingTaskRepository).insert(taskCaptor.capture());
        verify(templateMapper, never()).updateById(any(NursingTaskTemplate.class));

        NursingTask insertedTask = taskCaptor.getValue();
        assertThat(count).isEqualTo(1);
        assertThat(insertedTask.getTemplateId()).isEqualTo(1L);
        assertThat(insertedTask.getSanaId()).isEqualTo(100L);
        assertThat(insertedTask.getTaskTitle()).isEqualTo("翻身护理");
        assertThat(insertedTask.getAssigneeUserId()).isEqualTo(9001L);
        assertThat(insertedTask.getPlannedEndTime())
                .isEqualTo(insertedTask.getPlannedStartTime().plusMinutes(90));
        assertThat(insertedTask.getPlannedStartTime().getSecond()).isZero();
        assertThat(insertedTask.getPlannedStartTime().getNano()).isZero();
        assertThat(template.getNextTriggerTime()).isEqualTo(originalNextTrigger);
        assertThat(template.getLastTriggerTime()).isEqualTo(originalLastTrigger);
    }

    @Test
    void shouldAdvanceNextTriggerAfterGeneratingDueTasksAutomatically() throws Exception {
        LocalDateTime dueTime = LocalDateTime.now().withSecond(0).withNano(0).minusMinutes(1);
        TaskTemplateScheduleConfigDto scheduleConfig = TaskTemplateScheduleConfigDto.builder()
                .scheduleType(TaskTemplateScheduleType.DAILY)
                .timeMode(TaskTemplateTimeMode.POINT)
                .timePoints(List.of(dueTime.toLocalTime().toString().substring(0, 5)))
                .build();

        NursingTaskTemplate template = buildTemplate();
        template.setScheduleConfigJson(objectMapper.writeValueAsString(scheduleConfig));
        template.setTimezone("Asia/Shanghai");
        template.setNextTriggerTime(dueTime);
        template.setLastTriggerTime(null);
        template.setVersion(0);
        template.setEnabled(1);
        template.setStatus(0);

        when(templateMapper.selectList(any())).thenReturn(List.of(template));
        when(nursingTaskRepository.insert(any(NursingTask.class))).thenReturn(true);
        when(templateMapper.updateById(any(NursingTaskTemplate.class))).thenReturn(1);

        int count = service.generateAllDueTasks();

        ArgumentCaptor<NursingTaskTemplate> templateCaptor = ArgumentCaptor.forClass(NursingTaskTemplate.class);
        ArgumentCaptor<NursingTask> taskCaptor = ArgumentCaptor.forClass(NursingTask.class);
        verify(templateMapper).updateById(templateCaptor.capture());
        verify(nursingTaskRepository).insert(taskCaptor.capture());

        NursingTaskTemplate updatedTemplate = templateCaptor.getValue();
        NursingTask insertedTask = taskCaptor.getValue();
        assertThat(count).isEqualTo(1);
        assertThat(insertedTask.getAssignerUserId()).isNull();
        assertThat(updatedTemplate.getLastTriggerTime()).isEqualTo(dueTime);
        assertThat(updatedTemplate.getNextTriggerTime()).isEqualTo(dueTime.plusDays(1));
        assertThat(updatedTemplate.getVersion()).isEqualTo(1);
    }

    @Test
    void shouldUseOrConditionForTemplateDateRangeWhenSelectingDueTasks() {
        when(templateMapper.selectList(any())).thenReturn(List.of());

        service.generateAllDueTasks();

        ArgumentCaptor<LambdaQueryWrapper<NursingTaskTemplate>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(templateMapper).selectList(wrapperCaptor.capture());

        String sqlSegment = wrapperCaptor.getValue().getSqlSegment().replaceAll("\\s+", " ").toUpperCase();
        assertThat(sqlSegment).contains("START_DATE IS NULL OR START_DATE <=");
        assertThat(sqlSegment).contains("END_DATE IS NULL OR END_DATE >=");
        assertThat(sqlSegment).doesNotContain("START_DATE IS NULL AND START_DATE <=");
        assertThat(sqlSegment).doesNotContain("END_DATE IS NULL AND END_DATE >=");
    }

    @Test
    void shouldUseOrConditionForTemplateDateRangeWhenListingActiveTemplates() {
        when(templateMapper.selectList(any())).thenReturn(List.of());

        service.listActiveTemplates();

        ArgumentCaptor<LambdaQueryWrapper<NursingTaskTemplate>> wrapperCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(templateMapper).selectList(wrapperCaptor.capture());

        String sqlSegment = wrapperCaptor.getValue().getSqlSegment().replaceAll("\\s+", " ").toUpperCase();
        assertThat(sqlSegment).contains("START_DATE IS NULL OR START_DATE <=");
        assertThat(sqlSegment).contains("END_DATE IS NULL OR END_DATE >=");
        assertThat(sqlSegment).doesNotContain("START_DATE IS NULL AND START_DATE <=");
        assertThat(sqlSegment).doesNotContain("END_DATE IS NULL AND END_DATE >=");
    }

    private NursingTaskTemplate buildTemplate() {
        NursingTaskTemplate template = new NursingTaskTemplate();
        template.setId(1L);
        template.setSanaId(100L);
        template.setElderId(500L);
        template.setTaskTitle("翻身护理");
        template.setTaskContent("每次协助翻身并记录");
        template.setTaskType(2);
        template.setPriority(3);
        template.setAssigneeUserId(9001L);
        template.setPlannedDuration(90);
        return template;
    }
}
