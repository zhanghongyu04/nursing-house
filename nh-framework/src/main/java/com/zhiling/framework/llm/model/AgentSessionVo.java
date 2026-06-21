package com.zhiling.framework.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 智能体会话信息 VO。
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentSessionVo {

    /**
     * 会话ID（主键）。
     */
    private Long id;

    /**
     * 用户ID。
     */
    private Long userId;

    /**
     * 会话ID（用于关联Redis）。
     */
    private String conversationId;

    /**
     * 会话标题。
     */
    private String title;

    /**
     * 会话类型。
     */
    private String sessionType;

    /**
     * 会话状态。
     */
    private Integer status;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    private LocalDateTime updateTime;
}
