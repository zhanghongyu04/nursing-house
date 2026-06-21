package com.zhiling.framework.llm.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建智能体会话 DTO。
 *
 * @author zhanghongyu
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentSessionCreateDto {

    /**
     * 会话类型（chat-智能对话，pdf-文档问答，含所有文档类型）。
     */
    private String sessionType;

    /**
     * 会话标题（可选）。
     */
    private String title;
}
