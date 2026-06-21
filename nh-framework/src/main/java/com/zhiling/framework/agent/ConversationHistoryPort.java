package com.zhiling.framework.agent;

import java.util.List;

/**
 * 会话历史索引公开契约。
 *
 * @author zhanghongyu
 */
public interface ConversationHistoryPort {

    void save(String type, String conversationId);

    List<String> getConversationIds(String type);

    void remove(String type, String conversationId);
}
