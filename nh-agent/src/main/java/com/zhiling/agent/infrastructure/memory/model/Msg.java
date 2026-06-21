package com.zhiling.agent.infrastructure.memory.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.content.Media;
import java.util.List;
import java.util.Map;


/**
 * 消息序列化模型
 *
 * 用于 Redis 持久化存储，支持 Spring AI Message 类型的序列化与反序列化。
 *
 * @author zhanghongyu
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Msg {
    MessageType messageType;
    String text;
    Map<String, Object> metadata;
    List<AssistantMessage.ToolCall> toolCalls;

    /**
     * 构造器：Msg
     *
     * @author zhanghongyu
     */
    public Msg(Message message) {
        this.messageType = message.getMessageType();
        this.text = message.getText();
        this.metadata = message.getMetadata();
        if(message instanceof AssistantMessage am) {
            this.toolCalls = am.getToolCalls();
        }
    }

    /**
     * 方法：toMessage
     *
     * @author zhanghongyu
     */
    public Message toMessage() {
        return switch (messageType) {
            case SYSTEM -> new SystemMessage(text);
            case USER -> UserMessage.builder()
                    .text(text)
                    .media(List.<Media>of())
                    .metadata(metadata == null ? Map.of() : metadata)
                    .build();
            case ASSISTANT -> AssistantMessage.builder()
                    .content(text)
                    .properties(metadata == null ? Map.of() : metadata)
                    .toolCalls(toolCalls == null ? List.of() : toolCalls)
                    .media(List.of())
                    .build();
            default -> throw new IllegalArgumentException("Unsupported message type: " + messageType);
        };
    }
}