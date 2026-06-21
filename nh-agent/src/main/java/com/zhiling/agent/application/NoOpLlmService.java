package com.zhiling.agent.application;

import com.zhiling.framework.llm.core.service.LlmService;
import com.zhiling.framework.llm.core.model.LlmCapabilities;
import com.zhiling.framework.llm.core.model.LlmChatRequest;
import com.zhiling.framework.llm.core.model.LlmChatResponse;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NoOp 降级实现。
 *
 * @author zhanghongyu
 */
public class NoOpLlmService implements LlmService {

    /**
     * 方法：chat
     *
     * @author zhanghongyu
     */
    @Override
    public LlmChatResponse chat(LlmChatRequest request) {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("llm.chat", false);
        flags.put("llm.prompt", false);
        flags.put("llm.guard", false);
        flags.put("llm.rag", false);
        flags.put("llm.tool", false);
        flags.put("llm.memory", false);
        return LlmChatResponse.builder()
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .content("当前 LLM 模块未启用，请联系管理员开启 nh.modules.llm-enabled。")
                .capabilityFlags(flags)
                .degradeReason("LLM_DISABLED")
                .sources(java.util.List.of())
                .build();
    }

    /**
     * 方法：stream
     *
     * @author zhanghongyu
     */
    @Override
    public Flux<String> stream(LlmChatRequest request) {
        return Flux.just(chat(request).getContent());
    }

    /**
     * 方法：capabilities
     *
     * @author zhanghongyu
     */
    @Override
    public LlmCapabilities capabilities() {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("llm.chat", false);
        flags.put("llm.prompt", false);
        flags.put("llm.guard", false);
        flags.put("llm.rag", false);
        flags.put("llm.tool", false);
        flags.put("llm.memory", false);
        return LlmCapabilities.builder()
                .enabled(false)
                .capabilityFlags(flags)
                .build();
    }
}
