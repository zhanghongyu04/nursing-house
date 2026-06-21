package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import com.zhiling.model.dto.TaskTemplateScheduleConfigDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 护理任务定时模板
 *
 * @author zhanghongyu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("tb_nursing_task_template")
public class NursingTaskTemplate extends BaseEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属机构ID */
    @TableField("sana_id")
    private Long sanaId;

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

    /** 关联老人ID */
    @TableField("elder_id")
    private Long elderId;

    /** 执行人ID */
    @TableField("assignee_user_id")
    private Long assigneeUserId;

    /** 调度类型：DAILY / WEEKLY / MONTHLY */
    @TableField("schedule_type")
    private String scheduleType;

    /** 时间模式：POINT / INTERVAL */
    @TableField("time_mode")
    private String timeMode;

    /** 结构化调度配置 JSON */
    @TableField("schedule_config_json")
    private String scheduleConfigJson;

    /** 时区 */
    @TableField("timezone")
    private String timezone;

    /** 下次触发时间 */
    @TableField("next_trigger_time")
    private LocalDateTime nextTriggerTime;

    /** 上次触发时间 */
    @TableField("last_trigger_time")
    private LocalDateTime lastTriggerTime;

    /** 版本号，用于调度推进 */
    @TableField("version")
    private Integer version;

    /** 计划时长（分钟） */
    @TableField("planned_duration")
    private Integer plannedDuration;

    /** 是否启用 */
    @TableField("enabled")
    private Integer enabled;

    /** 生效开始日期 */
    @TableField("start_date")
    private LocalDate startDate;

    /** 生效结束日期 */
    @TableField("end_date")
    private LocalDate endDate;

    /** 所属机构名称（非数据库字段） */
    @TableField(exist = false)
    private String sanaName;

    /** 老人姓名（非数据库字段） */
    @TableField(exist = false)
    private String elderName;

    /** 执行人名称（非数据库字段） */
    @TableField(exist = false)
    private String assigneeUsername;

    /** 结构化调度配置（非数据库字段，前端展示用） */
    @TableField(exist = false)
    private TaskTemplateScheduleConfigDto scheduleConfig;

    /** 可读调度描述（非数据库字段，前端展示用） */
    @TableField(exist = false)
    private String scheduleDescription;

    /** 下次执行时间（非数据库字段，前端展示用） */
    @TableField(exist = false)
    private LocalDateTime nextExecuteTime;
}
