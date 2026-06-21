package com.zhiling.framework.llm.core.service;

import java.util.Optional;

/**
 * 输入输出防护端口。
 *
 * @author zhanghongyu
 */
public interface GuardPort {

    /**
     * 输入预拦截，命中时返回固定回复；未命中返回 empty。
     */
    Optional<String> interceptInput(String prompt);

    /**
     * 输出清洗与兜底。
     */
    String sanitizeOutput(String prompt, String rawReply);
}
