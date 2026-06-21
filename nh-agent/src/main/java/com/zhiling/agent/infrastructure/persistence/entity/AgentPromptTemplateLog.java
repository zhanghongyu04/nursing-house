package com.zhiling.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent 提示词变更日志实体
 *
 * @author zhanghongyu
 */
@Data
@TableName("tb_agent_prompt_template_log")
public class AgentPromptTemplateLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("prompt_id")
    private Long promptId;

    @TableField("prompt_name")
    private String promptName;

    @TableField("prompt_index")
    private Integer promptIndex;

    @TableField("old_version")
    private Integer oldVersion;

    @TableField("new_version")
    private Integer newVersion;

    @TableField("old_content")
    private String oldContent;

    @TableField("new_content")
    private String newContent;

    @TableField("operation_type")
    private String operationType;

    @TableField("operator")
    private String operator;

    @TableField("remark")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
