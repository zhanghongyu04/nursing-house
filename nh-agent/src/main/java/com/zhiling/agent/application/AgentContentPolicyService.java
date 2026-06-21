package com.zhiling.agent.application;

import java.util.Optional;

/**
 * Agent 对话内容策略服务（输入预拦截与输出净化）。
 *
 * @author zhanghongyu
 */
public interface AgentContentPolicyService {

    /**
     * 输入预拦截：命中策略时直接返回固定回复；未命中返回 empty。
     */
    Optional<String> applyInputPreIntercept(String userPrompt);

    /**
     * 输出策略：根据用户输入语义进行回复约束与净化。
     */
    String applyOutputPolicy(String userPrompt, String rawReply);
}