package com.zhiling.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

/**
 * 护理任务模板分页查询DTO
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TaskTemplatePageQueryDto implements Serializable {
    private Integer page;
    private Integer pageSize;
    private String taskTitle;
    private Integer enabled;
    private Set<Long> sanaScopeIds;
}
