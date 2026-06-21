package com.zhiling.agent.application;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Agent 聊天编排服务。
 *
 * @author zhanghongyu
 */
public interface AgentChatService {

    Flux<String> textChat(String prompt, String chatId, HttpServletResponse response);

    Flux<String> multiModalChat(String modelPrompt,
                                String storagePrompt,
                                String chatId,
                                List<MultipartFile> mediaFiles,
                                HttpServletResponse response);
}