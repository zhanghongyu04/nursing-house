package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 护理任务查询结果稳定模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NursingTaskRecord {

    private Long id;
    private Long sanaId;
    private String sanaName;
    private Long elderId;
    private String elderName;
    private String taskTitle;
    private String taskContent;
    private Integer taskType;
    private String taskTypeName;
    private Integer priority;
    private String priorityName;
    private Integer status;
    private String statusName;
    private Long assigneeUserId;
    private String assigneeUsername;
    private Long assignerUserId;
    private String assignerUsername;
    private LocalDateTime plannedStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime completionTime;
    private LocalDateTime createTime;
}
