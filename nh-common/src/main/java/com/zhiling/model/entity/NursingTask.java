package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 护理任务实体
 *
 * @author zhanghongyu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("tb_nursing_task")
public class NursingTask extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属机构ID */
    @TableField("sana_id")
    private Long sanaId;

    /** 来源模板ID */
    @TableField("template_id")
    private Long templateId;

    /** 关联老人ID */
    @TableField("elder_id")
    private Long elderId;

    /** 任务标题 */
    @TableField("task_title")
    private String taskTitle;

    /** 任务内容 */
    @TableField("task_content")
    private String taskContent;

    /** 任务类型（字典：NURSING_TASK_TYPE） */
    @TableField("task_type")
    private Integer taskType;

    /** 优先级（字典：NURSING_TASK_PRIORITY） */
    @TableField("priority")
    private Integer priority;

    /** 执行人ID */
    @TableField("assignee_user_id")
    private Long assigneeUserId;

    /** 下发人ID，系统自动生成时为空 */
    @TableField("assigner_user_id")
    private Long assignerUserId;

    /** 计划开始时间 */
    @TableField("planned_start_time")
    private LocalDateTime plannedStartTime;

    /** 计划结束时间 */
    @TableField("planned_end_time")
    private LocalDateTime plannedEndTime;

    /** 完成时间 */
    @TableField("completion_time")
    private LocalDateTime completionTime;

    /** 软删除标记：0-未删除，1-已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 所属机构名称（非数据库字段） */
    @TableField(exist = false)
    private String sanaName;

    /** 老人姓名（非数据库字段） */
    @TableField(exist = false)
    private String elderName;

    /** 执行人名称（非数据库字段） */
    @TableField(exist = false)
    private String assigneeUsername;

    /** 下发人名称（非数据库字段） */
    @TableField(exist = false)
    private String assignerUsername;
}
