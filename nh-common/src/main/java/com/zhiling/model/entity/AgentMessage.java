package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体会话消息明细。
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tb_agent_message")
public class AgentMessage extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("conversation_id")
    private String conversationId;

    @TableField("role")
    private String role;

    @TableField("content")
    private String content;

    @TableField("attachments_json")
    private String attachmentsJson;

    @TableField("message_type")
    private String messageType;

    @TableField("seq_no")
    private Integer seqNo;

    @TableField(exist = false)
    private String remark;
}
