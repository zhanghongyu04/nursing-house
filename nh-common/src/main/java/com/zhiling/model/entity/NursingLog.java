package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 护理日志实体
 *
 * @author zhanghongyu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("tb_nursing_log")
public class NursingLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属机构ID */
    @TableField("sana_id")
    private Long sanaId;

    /** 关联任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 关联老人ID */
    @TableField("elder_id")
    private Long elderId;

    /** 护理人员ID */
    @TableField("nurse_user_id")
    private Long nurseUserId;

    /** 日志时间 */
    @TableField("log_time")
    private LocalDateTime logTime;

    /** 日志内容 */
    @TableField("content")
    private String content;

    /** 是否异常（0否 1是） */
    @TableField("abnormal_flag")
    private Integer abnormalFlag;

    /** 附件地址（JSON数组或逗号分隔） */
    @TableField("attachment_urls")
    private String attachmentUrls;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 所属机构名称（非数据库字段） */
    @TableField(exist = false)
    private String sanaName;

    /** 老人姓名（非数据库字段） */
    @TableField(exist = false)
    private String elderName;

    /** 护理人员名称（非数据库字段） */
    @TableField(exist = false)
    private String nurseUsername;

    /** 任务标题（非数据库字段） */
    @TableField(exist = false)
    private String taskTitle;
}
