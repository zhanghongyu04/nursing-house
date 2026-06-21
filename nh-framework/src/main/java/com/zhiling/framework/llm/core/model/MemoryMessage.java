package com.zhiling.framework.llm.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标准化记忆消息模型。
 *
 * @author zhanghongyu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMessage {

    /**
     * 角色：user / assistant / system
     */
    private String role;

    /**
     * 消息文本
     */
    private String content;
}