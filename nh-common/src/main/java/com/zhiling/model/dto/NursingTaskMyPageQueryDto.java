package com.zhiling.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 我的护理任务分页查询DTO（护理端）
 *
 * @author zhanghongyu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NursingTaskMyPageQueryDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer page;
    private Integer pageSize;

    private Long sanaId;
    private Long elderId;
    private Long assigneeUserId;

    private String taskTitle;
    private Integer taskType;
    private Integer priority;
    private Integer status;
    private Set<Integer> statuses;

    private LocalDateTime plannedStartBegin;
    private LocalDateTime plannedStartEnd;

    private Set<Long> sanaScopeIds;
}
