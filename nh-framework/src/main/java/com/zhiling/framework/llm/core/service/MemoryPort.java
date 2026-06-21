package com.zhiling.framework.llm.core.service;

import com.zhiling.framework.llm.core.model.MemoryMessage;

import java.util.List;

/**
 * 对话记忆端口。
 *
 * <h3>职责边界</h3>
 * 负责 <b>模型上下文记忆</b>：维护 LLM 每轮对话时需要携带的上下文消息，
 * 即“模型看到了什么”。用于 Spring AI ChatClient 的 advisor 链路。
 *
 * 与 {@code RedisChatMemory}（历史展示记忆）的分工：
 * <ul>
 *   <li>{@code MemoryPort} → 模型上下文：存储结构化 {@link MemoryMessage}，
 *       供 LLM 读取，限制条数，关注"模型需要知道什么"</li>
 *   <li>{@code RedisChatMemory} → 历史展示：存储完整用户/助手对话记录，
 *       供前端历史列表展示，关注"用户看到了什么"</li>
 * </ul>
 *
 * 会话删除时，两套记忆必须同步清理。
 *
 * @author zhanghongyu
 */
public interface MemoryPort {

    /**
     * 加载会话历史消息（按时间正序：旧 -> 新）。
     *
     * @param chatId 会话ID
     * @param limit 最大消息条数
     * @return 历史消息
     */
    List<MemoryMessage> load(String chatId, int limit);

    /**
     * 加载会话历史消息（按时间正序：旧 -> 新），优先使用显式 userId 进行隔离。
     *
     * @param chatId 会话ID
     * @param limit 最大消息条数
     * @param userId 当前用户ID
     * @return 历史消息
     */
    default List<MemoryMessage> load(String chatId, int limit, Long userId) {
        return load(chatId, limit);
    }

    /**
     * 追加一条消息到会话记忆。
     *
     * @param chatId 会话ID
     * @param message 消息
     */
    void append(String chatId, MemoryMessage message);

    /**
     * 追加一条消息到会话记忆，优先使用显式 userId 进行隔离。
     *
     * @param chatId 会话ID
     * @param message 消息
     * @param userId 当前用户ID
     */
    default void append(String chatId, MemoryMessage message, Long userId) {
        append(chatId, message);
    }
}
