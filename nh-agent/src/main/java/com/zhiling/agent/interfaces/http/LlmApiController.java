package com.zhiling.agent.interfaces.http;

import com.zhiling.framework.llm.core.service.LlmService;
import com.zhiling.framework.llm.core.model.LlmChatRequest;
import com.zhiling.framework.llm.core.model.LlmChatResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 标准化 LLM API。
 *
 * @author zhanghongyu
 */
@RestController
@RequestMapping("/api/llm")
@ConditionalOnProperty(prefix = "nh.llm.chat", name = "expose-api", havingValue = "true", matchIfMissing = true)
public class LlmApiController {

    private final LlmService llmService;

    /**
     * 构造器：LlmApiController
     *
     * @author zhanghongyu
     */
    public LlmApiController(LlmService llmService) {
        this.llmService = llmService;
    }

    /**
     * 方法：chat
     *
     * @author zhanghongyu
     */
    @PostMapping("/chat")
    public LlmChatResponse chat(@RequestBody LlmChatRequest request) {
        return llmService.chat(request);
    }

    /**
     * 方法：chatStream
     *
     * @author zhanghongyu
     */
    @PostMapping(value = "/chat:stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody LlmChatRequest request) {
        return llmService.stream(request);
    }

    /**
     * 方法：capabilities
     *
     * @author zhanghongyu
     */
    @GetMapping("/capabilities")
    public Object capabilities() {
        return llmService.capabilities();
    }
}
