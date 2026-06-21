package com.zhiling.agent.infrastructure.support;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 对话查询分类器。
 *
 * @author zhanghongyu
 */
public final class ConversationQueryClassifier {

    private static final int MIN_KNOWLEDGE_QUERY_LENGTH = 4;

    private static final String[] SMALL_TALK_QUERIES = {
            "你好", "您好", "嗨", "hi", "hello", "helloworld", "哈喽", "在吗", "早上好", "中午好", "下午好", "晚上好", "谢谢", "感谢",
            "test", "testing", "测试", "测试一下", "试一下"
    };

    private static final String[] INTERNAL_DATA_WORDS = {
            "系统概览", "概览统计", "平台概览", "系统统计", "平台统计", "总体情况", "整体情况", "总体统计",
            "区县养老机构数量分布", "区域养老机构数量分布", "各区县养老机构数量分布", "区域机构分布", "机构区域分布",
            "区县分布", "机构详情", "详情统计", "机构概况", "概况统计", "机构信息", "机构画像",
            "床位使用率", "床位统计", "入住情况", "统一社会信用代码", "社会信用代码", "信用代码",
            "法人姓名", "法人联系方式", "法人电话", "自理能力分布", "失能等级分布", "老人数量", "机构数量",
            "当前授权范围", "当前授权机构范围", "授权机构范围", "授权范围", "当前机构", "本机构",
            "护理任务", "正常护理任务", "异常护理任务", "正常任务", "异常任务", "待执行", "执行中",
            "已超时", "超时任务", "逾期任务", "护理日志", "护理记录", "正常日志", "普通日志",
            "普通护理日志", "异常日志", "异常记录"
    };

    private static final String[] MEDIA_REFERENCE_WORDS = {
            "图片", "这张图", "那张图", "图里", "截图", "照片", "媒体", "视频", "音频",
            "重新分析", "继续分析", "再讲", "再说"
    };

    private static final String[] KNOWLEDGE_DOMAIN_WORDS = {
            "护理", "照护", "老人", "长者", "养老", "压疮", "褥疮", "翻身", "卧床", "生命体征",
            "体温", "脉搏", "呼吸", "血压", "口腔", "晨晚间", "清洁", "给药", "服药",
            "药物", "过敏", "注射", "饮食", "鼻饲", "营养", "出入液量", "排泄", "排尿",
            "排便", "便秘", "尿潴留", "失能", "自理能力", "康复", "护理记录", "护理任务"
    };

    private static final String[] KNOWLEDGE_INTENT_WORDS = {
            "怎么", "如何", "为什么", "原因", "方法", "步骤", "流程", "注意事项", "处理", "预防",
            "评估", "诊断", "措施", "规范", "原则", "区别", "解释", "介绍", "说明"
    };

    private ConversationQueryClassifier() {
    }

    public static ConversationQueryType classify(String query) {
        if (!StringUtils.hasText(query)) {
            return ConversationQueryType.GENERAL;
        }
        if (ConversationMetaQueryDetector.isConversationMetaQuery(query)) {
            return ConversationQueryType.CONVERSATION_META;
        }
        String compact = query.replaceAll("\\s+", "").trim();
        String normalized = compact.toLowerCase(Locale.ROOT);
        if (isSmallTalk(normalized, compact)) {
            return ConversationQueryType.SMALL_TALK;
        }
        if (containsAny(compact, MEDIA_REFERENCE_WORDS)) {
            return ConversationQueryType.MEDIA_REFERENCE;
        }
        if (containsAny(compact, INTERNAL_DATA_WORDS)) {
            return ConversationQueryType.INTERNAL_DATA;
        }
        if (containsAny(compact, KNOWLEDGE_DOMAIN_WORDS)
                && (compact.length() >= MIN_KNOWLEDGE_QUERY_LENGTH || containsAny(compact, KNOWLEDGE_INTENT_WORDS))) {
            return ConversationQueryType.KNOWLEDGE;
        }
        if (containsAny(compact, KNOWLEDGE_INTENT_WORDS) && containsAny(compact, KNOWLEDGE_DOMAIN_WORDS)) {
            return ConversationQueryType.KNOWLEDGE;
        }
        return ConversationQueryType.GENERAL;
    }

    public static boolean shouldUseKnowledgeRetrieval(String query) {
        return classify(query) == ConversationQueryType.KNOWLEDGE;
    }

    public static boolean shouldSkipKnowledgeRetrieval(String query) {
        return !shouldUseKnowledgeRetrieval(query);
    }

    private static boolean isSmallTalk(String normalized, String compact) {
        for (String smallTalk : SMALL_TALK_QUERIES) {
            if (normalized.equals(smallTalk.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return (compact.length() < MIN_KNOWLEDGE_QUERY_LENGTH || isConnectivityTest(normalized, compact))
                && !containsAny(compact, KNOWLEDGE_DOMAIN_WORDS)
                && !containsAny(compact, INTERNAL_DATA_WORDS);
    }

    private static boolean isConnectivityTest(String normalized, String compact) {
        return normalized.startsWith("hello")
                || normalized.startsWith("test")
                || compact.startsWith("测试")
                || compact.startsWith("试一下");
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
