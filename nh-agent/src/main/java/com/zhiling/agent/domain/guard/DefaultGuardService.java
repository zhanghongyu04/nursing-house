package com.zhiling.agent.domain.guard;

import com.zhiling.framework.llm.core.service.GuardPort;

import java.util.Optional;

/**
 * 默认防护实现。
 *
 * @author zhanghongyu
 */
public class DefaultGuardService implements GuardPort {

    /**
     * 方法：interceptInput
     *
     * @author zhanghongyu
     */
    @Override
    public Optional<String> interceptInput(String input) {
        return Optional.empty();
    }

    /**
     * 方法：sanitizeOutput
     *
     * @author zhanghongyu
     */
    @Override
    public String sanitizeOutput(String input, String output) {
        if (output == null) {
            return "";
        }
        String sanitized = output
                .replaceAll("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+", "")
                .replace("🤖", "")
                .replace("😊", "")
                .replace("👍", "");
        return sanitized.trim();
    }
}

