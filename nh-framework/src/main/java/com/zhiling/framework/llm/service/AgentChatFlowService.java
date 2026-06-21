package com.zhiling.framework.llm.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Agent 聊天流程编排服务。
 *
 * @author zhanghongyu
 */
public interface AgentChatFlowService {

    Flux<String> chat(String prompt, String chatId, List<MultipartFile> files, HttpServletResponse response);
}
