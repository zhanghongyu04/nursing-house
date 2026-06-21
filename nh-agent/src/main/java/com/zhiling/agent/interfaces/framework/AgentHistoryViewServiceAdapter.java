package com.zhiling.agent.interfaces.framework;

import com.zhiling.framework.llm.model.AgentAttachment;
import com.zhiling.framework.llm.model.AgentHistoryMessage;
import com.zhiling.framework.llm.model.AgentHistoryView;
import com.zhiling.framework.llm.service.AgentHistoryViewService;
import com.zhiling.framework.llm.service.AgentSessionHistoryService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 历史消息展示适配器。
 *
 * @author zhanghongyu
 */
@Component
public class AgentHistoryViewServiceAdapter implements AgentHistoryViewService {

    private final AgentSessionHistoryService agentSessionHistoryService;

    /**
     * 构造器：AgentHistoryViewServiceAdapter
     *
     * @author zhanghongyu
     */
    public AgentHistoryViewServiceAdapter(AgentSessionHistoryService agentSessionHistoryService) {
        this.agentSessionHistoryService = agentSessionHistoryService;
    }

    /**
     * 方法：getChatHistoryView
     *
     * @author zhanghongyu
     */
    @Override
    public List<AgentHistoryView> getChatHistoryView(String type, String chatId) {
        List<AgentHistoryMessage> messages = agentSessionHistoryService.getChatHistory(type, chatId);
        return messages.stream().map(item -> {
            AgentHistoryView view = new AgentHistoryView();
            view.setRole(item.getRole());
            view.setContent(item.getContent());
            view.setAttachments(convertAttachments(item.getAttachments()));
            return view;
        }).toList();
    }

    /**
     * 方法：convertAttachments
     *
     * @author zhanghongyu
     */
    private List<AgentHistoryView.AttachmentView> convertAttachments(List<AgentAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }
        return attachments.stream()
                .map(item -> new AgentHistoryView.AttachmentView(item.getFileName(), item.getFileType(), item.getFileUrl()))
                .toList();
    }
}


