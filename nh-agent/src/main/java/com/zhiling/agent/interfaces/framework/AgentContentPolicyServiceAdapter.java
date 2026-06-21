package com.zhiling.agent.interfaces.framework;

import com.zhiling.agent.application.AgentContentPolicyService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Agent 输入输出策略适配器。
 *
 * @author zhanghongyu
 */
@Component
public class AgentContentPolicyServiceAdapter implements AgentContentPolicyService {

    private static final String USER_QUESTION_MARKER = "用户问题：";
    private static final String IDENTITY_FIXED_REPLY = "我是养护智能体。";
    private static final String DEBUG_PROBE_REPLY = "请直接描述康养业务问题或数据查询需求，我会基于当前权限为你处理。";
    private static final String ROLE_OVERRIDE_REPLY = "我不能切换或扮演其他角色，但可以继续在当前能力边界内为你提供帮助。";
    private static final Pattern IDENTITY_QUERY_PATTERN = Pattern.compile(
            "(你是谁|你是.?什么|你的身份|身份是什么|你是什么模型|你用的什么模型|模型名称|model|llm)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DEBUG_PROBE_PATTERN = Pattern.compile(
            "^\\s*/[a-zA-Z0-9_\\-]{1,32}\\s*$|(?:^|\\b)(?:debug|test|ping|health|trace|stack|console|printenv|日志|调试|探针|压测)(?:\\b|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ROLE_OVERRIDE_PATTERN = Pattern.compile(
            "(忽略(以上|之前|前面).{0,20}(规则|指令|设定)|你来扮演|你现在是|从现在开始你是|请扮演|切换到.*角色|进入.*模式)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
            "[\\x{1F300}-\\x{1F5FF}" +
                    "\\x{1F600}-\\x{1F64F}" +
                    "\\x{1F680}-\\x{1F6FF}" +
                    "\\x{1F700}-\\x{1F77F}" +
                    "\\x{1F780}-\\x{1F7FF}" +
                    "\\x{1F800}-\\x{1F8FF}" +
                    "\\x{1F900}-\\x{1F9FF}" +
                    "\\x{1FA00}-\\x{1FA6F}" +
                    "\\x{1FA70}-\\x{1FAFF}" +
                    "\\x{2600}-\\x{26FF}" +
                    "\\x{2700}-\\x{27BF}]");
    private static final List<String> BANNED_IDENTITY_PHRASES = List.of(
            "本系统唯一对外可见的智能助手名称",
            "本系统唯一助手",
            "唯一对外可见的智能助手"
    );

    /**
     * 方法：applyInputPreIntercept
     *
     * @author zhanghongyu
     */
    @Override
    public Optional<String> applyInputPreIntercept(String userPrompt) {
        String normalizedPrompt = normalizePromptForPolicy(userPrompt);
        if (isLikelyDebugProbe(normalizedPrompt)) {
            return Optional.of(DEBUG_PROBE_REPLY);
        }
        if (isRoleOverridePrompt(normalizedPrompt)) {
            return Optional.of(ROLE_OVERRIDE_REPLY);
        }
        if (isIdentityQuery(normalizedPrompt)) {
            return Optional.of(IDENTITY_FIXED_REPLY);
        }
        return Optional.empty();
    }

    /**
     * 方法：applyOutputPolicy
     *
     * @author zhanghongyu
     */
    @Override
    public String applyOutputPolicy(String userPrompt, String rawReply) {
        String normalizedPrompt = normalizePromptForPolicy(userPrompt);
        if (isLikelyDebugProbe(normalizedPrompt)) {
            return DEBUG_PROBE_REPLY;
        }
        if (isRoleOverridePrompt(normalizedPrompt)) {
            return ROLE_OVERRIDE_REPLY;
        }
        if (isIdentityQuery(normalizedPrompt)) {
            return IDENTITY_FIXED_REPLY;
        }
        return sanitizeIdentityPhrases(rawReply);
    }

    /**
     * 方法：normalizePromptForPolicy
     *
     * @author zhanghongyu
     */
    private String normalizePromptForPolicy(String prompt) {
        if (prompt == null) {
            return "";
        }
        int questionIndex = prompt.lastIndexOf(USER_QUESTION_MARKER);
        if (questionIndex >= 0) {
            return prompt.substring(questionIndex + USER_QUESTION_MARKER.length()).trim();
        }
        return prompt.trim();
    }

    /**
     * 方法：isIdentityQuery
     *
     * @author zhanghongyu
     */
    private boolean isIdentityQuery(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }
        return IDENTITY_QUERY_PATTERN.matcher(prompt).find();
    }

    /**
     * 方法：isLikelyDebugProbe
     *
     * @author zhanghongyu
     */
    private boolean isLikelyDebugProbe(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }
        return DEBUG_PROBE_PATTERN.matcher(prompt.trim()).find();
    }

    /**
     * 方法：isRoleOverridePrompt
     *
     * @author zhanghongyu
     */
    private boolean isRoleOverridePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return false;
        }
        return ROLE_OVERRIDE_PATTERN.matcher(prompt).find();
    }

    /**
     * 方法：sanitizeIdentityPhrases
     *
     * @author zhanghongyu
     */
    private String sanitizeIdentityPhrases(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String sanitized = text;
        for (String phrase : BANNED_IDENTITY_PHRASES) {
            sanitized = sanitized.replace(phrase, "");
        }
        sanitized = sanitized.trim();
        if (sanitized.isEmpty()) {
            return IDENTITY_FIXED_REPLY;
        }
        return removeEmoji(sanitized);
    }

    /**
     * 方法：removeEmoji
     *
     * @author zhanghongyu
     */
    private String removeEmoji(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return EMOJI_PATTERN.matcher(text).replaceAll("").trim();
    }
}

