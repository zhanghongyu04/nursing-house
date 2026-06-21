package com.zhiling.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Agent 提示词配置实体
 *
 * @author zhanghongyu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_agent_prompt_template")
public class AgentPromptTemplate extends BaseEntity {

    /**
     * 提示词名称（例如 CHAT_ROLE）
     */
    @TableField("prompt_name")
    private String promptName;

    /**
     * 片段顺序索引
     */
    @TableField("prompt_index")
    private Integer promptIndex;

    /**
     * 提示词内容
     */
    @TableField("prompt_content")
    private String promptContent;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 聚合查询使用：片段数量。
     */
    @TableField(value = "segment_count", exist = false)
    private Long segmentCount;

    /**
     * 重写字段：排除 remark（表中不存在）
     */
    @TableField(exist = false)
    private String remark;
}


