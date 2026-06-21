package com.zhiling.agent.infrastructure.memory;

import com.zhiling.framework.llm.core.model.MemoryMessage;
import com.zhiling.framework.llm.core.service.MemoryPort;

import java.util.List;

/**
 * 记忆 NoOp 降级实现。
 *
 * @author zhanghongyu
 */
public class NoOpMemoryService implements MemoryPort {

    /**
     * 方法：load
     *
     * @author zhanghongyu
     */
    @Override
    public List<MemoryMessage> load(String chatId, int limit) {
        return List.of();
    }

    /**
     * 方法：append
     *
     * @author zhanghongyu
     */
    @Override
    public void append(String chatId, MemoryMessage message) {
        // no-op
    }
}
