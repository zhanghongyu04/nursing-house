package com.zhiling.framework.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 护理日志查询结果稳定模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NursingLogRecord {

    private Long id;
    private Long sanaId;
    private String sanaName;
    private Long taskId;
    private String taskTitle;
    private Long elderId;
    private String elderName;
    private Long nurseUserId;
    private String nurseUsername;
    private LocalDateTime logTime;
    private String content;
    private Integer abnormalFlag;
    private String abnormalName;
    private Boolean hasAttachment;
    private LocalDateTime createTime;
}
