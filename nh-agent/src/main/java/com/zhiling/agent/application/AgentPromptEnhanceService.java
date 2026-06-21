package com.zhiling.agent.application;

import com.zhiling.framework.llm.model.AgentPromptEnhanceResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Agent 文件增强提示词服务。
 *
 * @author zhanghongyu
 */
public interface AgentPromptEnhanceService {

    AgentPromptEnhanceResult buildEnhancedPrompt(String userPrompt, List<MultipartFile> files);

    String ensurePromptNotEmpty(String prompt);
}