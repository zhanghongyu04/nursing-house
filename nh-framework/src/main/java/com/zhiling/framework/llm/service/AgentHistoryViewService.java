package com.zhiling.framework.llm.service;

import com.zhiling.framework.llm.model.AgentHistoryView;

import java.util.List;

/**
 * Agent 历史消息展示服务。
 *
 * @author zhanghongyu
 */
public interface AgentHistoryViewService {

    List<AgentHistoryView> getChatHistoryView(String type, String chatId);
}
