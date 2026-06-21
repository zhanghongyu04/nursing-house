package com.zhiling.agent.domain.guard;

import com.zhiling.framework.llm.core.service.GuardPort;

import java.util.Optional;

/**
 * 透传防护实现（关闭 Guard 时使用）。
 *
 * @author zhanghongyu
 */
public class PassThroughGuardService implements GuardPort {

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
        return output == null ? "" : output;
    }
}

