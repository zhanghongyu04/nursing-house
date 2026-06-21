package com.zhiling.framework.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新智能体会话 DTO。
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentSessionUpdateDto {

    /**
     * 会话ID。
     */
    private String conversationId;

    /**
     * 会话标题（可选）。
     */
    private String title;

    /**
     * 会话状态（0-正常，1-已删除，2-已禁用）。
     */
    private Integer status;
}
