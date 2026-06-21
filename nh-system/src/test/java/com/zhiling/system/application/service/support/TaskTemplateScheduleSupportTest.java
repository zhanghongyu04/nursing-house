package com.zhiling.system.application.service.support;

import com.zhiling.model.dto.TaskTemplateScheduleConfigDto;
import com.zhiling.model.enums.TaskTemplateScheduleType;
import com.zhiling.model.enums.TaskTemplateTimeMode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 护理任务模板调度支持工具单元测试
 *
 * @author zhanghongyu
 */
class TaskTemplateScheduleSupportTest {

    @Test
    void shouldCalculateNearestDailyPointTrigger() {
        TaskTemplateScheduleConfigDto config = TaskTemplateScheduleSupport.normalizeAndValidate(
                TaskTemplateScheduleConfigDto.builder()
                        .scheduleType(TaskTemplateScheduleType.DAILY)
                        .timeMode(TaskTemplateTimeMode.POINT)
                        .timePoints(List.of("13:30", "09:00", "09:00"))
                        .build()
        );

        LocalDateTime nextTrigger = TaskTemplateScheduleSupport.calculateNextTrigger(
                config,
                null,
                null,
                TaskTemplateScheduleSupport.DEFAULT_TIMEZONE,
                LocalDateTime.of(2026, 4, 18, 8, 15)
        );

        assertThat(nextTrigger).isEqualTo(LocalDateTime.of(2026, 4, 18, 9, 0));
    }

    @Test
    void shouldCalculateNearestWeeklyIntervalTriggerOnMatchingWeekday() {
        TaskTemplateScheduleConfigDto config = TaskTemplateScheduleSupport.normalizeAndValidate(
                TaskTemplateScheduleConfigDto.builder()
                        .scheduleType(TaskTemplateScheduleType.WEEKLY)
                        .timeMode(TaskTemplateTimeMode.INTERVAL)
                        .weekdays(List.of(1, 3))
                        .startTime("08:00")
                        .endTime("10:00")
                        .intervalMinutes(30)
                        .build()
        );

        LocalDateTime nextTrigger = TaskTemplateScheduleSupport.calculateNextTrigger(
                config,
                null,
                null,
                TaskTemplateScheduleSupport.DEFAULT_TIMEZONE,
                LocalDateTime.of(2026, 4, 20, 8, 20)
        );

        assertThat(nextTrigger).isEqualTo(LocalDateTime.of(2026, 4, 20, 8, 30));
    }

    @Test
    void shouldRollToNextMatchingMonthForMonthlyPointTrigger() {
        TaskTemplateScheduleConfigDto config = TaskTemplateScheduleSupport.normalizeAndValidate(
                TaskTemplateScheduleConfigDto.builder()
                        .scheduleType(TaskTemplateScheduleType.MONTHLY)
                        .timeMode(TaskTemplateTimeMode.POINT)
                        .monthDays(List.of(5, 20))
                        .timePoints(List.of("09:00"))
                        .build()
        );

        LocalDateTime nextTrigger = TaskTemplateScheduleSupport.calculateNextTrigger(
                config,
                null,
                null,
                TaskTemplateScheduleSupport.DEFAULT_TIMEZONE,
                LocalDateTime.of(2026, 4, 21, 0, 0)
        );

        assertThat(nextTrigger).isEqualTo(LocalDateTime.of(2026, 5, 5, 9, 0));
    }

    @Test
    void shouldReturnNullWhenEndDateBlocksFutureCandidates() {
        TaskTemplateScheduleConfigDto config = TaskTemplateScheduleSupport.normalizeAndValidate(
                TaskTemplateScheduleConfigDto.builder()
                        .scheduleType(TaskTemplateScheduleType.DAILY)
                        .timeMode(TaskTemplateTimeMode.POINT)
                        .timePoints(List.of("09:00"))
                        .build()
        );

        LocalDateTime nextTrigger = TaskTemplateScheduleSupport.calculateNextTrigger(
                config,
                null,
                LocalDate.of(2026, 4, 18),
                TaskTemplateScheduleSupport.DEFAULT_TIMEZONE,
                LocalDateTime.of(2026, 4, 18, 10, 0)
        );

        assertThat(nextTrigger).isNull();
    }
}
