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
 * 护理日志分页查询DTO（机构侧）
 *
 * @author zhanghongyu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NursingLogPageQueryDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer page;
    private Integer pageSize;

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
     * 按日志ID查询（导出单选/多选场景）
     */
    private List<Long> logIds;
}
