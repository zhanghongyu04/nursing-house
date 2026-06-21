package com.zhiling.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 护理任务模板创建/更新DTO
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskTemplateCreateDto implements Serializable {
    private Long id;
    private Long sanaId;
    private String taskTitle;
    private String taskContent;
    private Integer taskType;
    private Integer priority;
    private Long elderId;
    private Long assigneeUserId;
    private String timezone;
    private TaskTemplateScheduleConfigDto scheduleConfig;
    private Integer plannedDuration;
    private LocalDate startDate;
    private LocalDate endDate;
    private String remark;
}
