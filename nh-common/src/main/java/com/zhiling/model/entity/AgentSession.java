package com.zhiling.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiling.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体会话实体类
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tb_agent_session")
public class AgentSession extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话ID（用于关联Redis中的聊天记录）
     * 使用UUID生成，存储在数据库中便于追踪
     */
    @TableField("conversation_id")
    private String conversationId;

    /**
     * 会话标题（可选，首次对话内容的前50个字符）
     */
    @TableField("title")
    private String title;

    /**
     * 会话类型（chat-智能对话，pdf-文档问答，含所有文档类型）
     */
    @TableField("session_type")
    private String sessionType;

    /**
     * 重写字段：排除remark（表中不存在）
     */
    @TableField(exist = false)
    private String remark;
}
