package com.zhiling.agent.infrastructure.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationQueryClassifierTest {

    @Test
    void shouldSkipConnectivityTestQueries() {
        assertEquals(ConversationQueryType.SMALL_TALK, ConversationQueryClassifier.classify("Hello World"));
        assertEquals(ConversationQueryType.SMALL_TALK, ConversationQueryClassifier.classify("test"));
        assertEquals(ConversationQueryType.SMALL_TALK, ConversationQueryClassifier.classify("测试一下"));

        assertTrue(ConversationQueryClassifier.shouldSkipKnowledgeRetrieval("Hello World"));
    }

    @Test
    void shouldKeepKnowledgeQueriesRoutedToRag() {
        assertEquals(ConversationQueryType.KNOWLEDGE, ConversationQueryClassifier.classify("卧床老人如何预防压疮"));

        assertFalse(ConversationQueryClassifier.shouldSkipKnowledgeRetrieval("卧床老人如何预防压疮"));
    }

    @Test
    void shouldRouteNursingCareQueriesToInternalData() {
        assertEquals(ConversationQueryType.INTERNAL_DATA,
                ConversationQueryClassifier.classify("请查询当前授权机构范围普通护理日志"));
        assertEquals(ConversationQueryType.INTERNAL_DATA,
                ConversationQueryClassifier.classify("请查询当前授权机构范围正常护理任务列表"));

        assertTrue(ConversationQueryClassifier.shouldSkipKnowledgeRetrieval("请查询当前授权机构范围普通护理日志"));
    }
}
