package com.zhiling.model.dto;

import com.zhiling.model.enums.TaskTemplateScheduleType;
import com.zhiling.model.enums.TaskTemplateTimeMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 护理任务模板结构化调度配置
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskTemplateScheduleConfigDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 调度类型：每天 / 每周 / 每月 */
    private TaskTemplateScheduleType scheduleType;

    /** 时间模式：定点 / 区间 */
    private TaskTemplateTimeMode timeMode;

    /** 周模式下的周几集合，1-7 表示周一到周日 */
    private List<Integer> weekdays;

    /** 月模式下的日期集合，1-31 */
    private List<Integer> monthDays;

    /** 定点执行时间集合，格式 HH:mm */
    private List<String> timePoints;

    /** 区间开始时间，格式 HH:mm */
    private String startTime;

    /** 区间结束时间，格式 HH:mm */
    private String endTime;

    /** 区间执行步长，单位分钟，仅允许 30 / 60 / 120 */
    private Integer intervalMinutes;
}
