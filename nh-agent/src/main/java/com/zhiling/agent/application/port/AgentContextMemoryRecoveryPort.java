package com.zhiling.agent.application.port;

import com.zhiling.framework.llm.core.model.MemoryMessage;

import java.util.List;

/**
 * 模型上下文记忆恢复端口。
 *
 * @author zhanghongyu
 */
public interface AgentContextMemoryRecoveryPort {

    /**
     * 从持久化消息明细恢复最近的模型上下文消息，按时间正序返回。
     *
     * @param userId 用户 ID
     * @param conversationId 会话 ID
     * @param limit 最大消息数
     * @return 模型上下文消息
     */
    List<MemoryMessage> recover(Long userId, String conversationId, int limit);

}
