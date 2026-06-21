package com.zhiling.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 护理日志导出查询DTO（机构侧）
 *
 * @author zhanghongyu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NursingLogExportDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long sanaId;
    private Long taskId;
    private Long elderId;
    private Long nurseUserId;

    private Integer abnormalFlag;
    private String content;

    private LocalDateTime logTimeBegin;
    private LocalDateTime logTimeEnd;

    private Set<Long> sanaScopeIds;

    /**
     * 按日志ID定向导出（支持单选/多选）
     */
    private List<Long> logIds;

    /**
     * 导出报告格式：docx/pdf，默认 docx
     */
    private String reportFormat;

    /**
     * 是否包含附件，默认 true
     */
    private Boolean includeAttachments;
}
