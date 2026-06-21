package com.zhiling.agent.infrastructure.memory;

import com.zhiling.framework.llm.core.model.MemoryMessage;
import com.zhiling.framework.llm.core.service.MemoryPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 JVM 内存的记忆实现。
 *
 * @author zhanghongyu
 */
public class InMemoryMemoryService implements MemoryPort {

    private final Map<String, List<MemoryMessage>> store = new ConcurrentHashMap<>();

    /**
     * 方法：load
     *
     * @author zhanghongyu
     */
    @Override
    public List<MemoryMessage> load(String chatId, int limit) {
        if (chatId == null || chatId.isBlank() || limit <= 0) {
            return List.of();
        }
        List<MemoryMessage> messages = store.get(chatId);
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(messages.size() - limit, 0);
        return new ArrayList<>(messages.subList(fromIndex, messages.size()));
    }

    /**
     * 方法：append
     *
     * @author zhanghongyu
     */
    @Override
    public void append(String chatId, MemoryMessage message) {
        if (chatId == null || chatId.isBlank() || message == null) {
            return;
        }
        store.computeIfAbsent(chatId, key -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }
}
