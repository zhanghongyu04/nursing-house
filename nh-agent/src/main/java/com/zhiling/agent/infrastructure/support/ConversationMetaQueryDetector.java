package com.zhiling.agent.infrastructure.support;

import org.springframework.util.StringUtils;

/**
 * 会话元问题识别。
 *
 * 这类问题应基于当前聊天历史回答，不应再触发知识库 RAG，否则会把“总结本次对话”
 * 误路由成护理知识问答。
 *
 * @author zhanghongyu
 */
public final class ConversationMetaQueryDetector {

    private ConversationMetaQueryDetector() {
    }

    public static boolean isConversationMetaQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return false;
        }
        String compact = query.replaceAll("\\s+", "");
        boolean summaryIntent = containsAny(compact,
                "总结", "概括", "归纳", "回顾", "梳理", "复盘", "整理");
        boolean conversationScope = containsAny(compact,
                "本次对话", "这次对话", "当前对话", "刚才", "前面", "上面", "以上", "上述",
                "我们聊", "聊天内容", "对话内容", "上下文", "上文");
        if (summaryIntent && conversationScope) {
            return true;
        }

        return containsAny(compact,
                "总结一下本次对话",
                "总结本次对话",
                "总结一下刚才",
                "总结一下上文",
                "概括一下本次对话",
                "梳理一下本次对话",
                "回顾一下本次对话");
    }

    private static boolean containsAny(String text, String... words) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String word : words) {
            if (StringUtils.hasText(word) && text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
