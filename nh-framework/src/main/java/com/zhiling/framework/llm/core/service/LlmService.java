package com.zhiling.framework.llm.core.service;

import com.zhiling.framework.llm.core.model.LlmCapabilities;
import com.zhiling.framework.llm.core.model.LlmChatRequest;
import com.zhiling.framework.llm.core.model.LlmChatResponse;
import reactor.core.publisher.Flux;

/**
 * 统一大模型门面接口，屏蔽底层厂商 SDK 与基础设施细节。
 *
 * @author zhanghongyu
 */
public interface LlmService {

    /**
     * 同步对话。
     */
    LlmChatResponse chat(LlmChatRequest request);

    /**
     * 流式对话。
     */
    Flux<String> stream(LlmChatRequest request);

    /**
     * 能力声明。
     */
    LlmCapabilities capabilities();
}
