package com.zhiling.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 护理任务下发DTO
 *
 * @author zhanghongyu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NursingTaskDispatchDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long sanaId;
    private Long elderId;

    private String taskTitle;
    private String taskContent;

    private Integer taskType;
    private Integer priority;
    private Integer status;

    private Long assigneeUserId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime plannedStartTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime plannedEndTime;

    private String remark;
}
