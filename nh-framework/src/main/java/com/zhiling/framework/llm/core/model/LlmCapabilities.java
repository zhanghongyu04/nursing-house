package com.zhiling.framework.llm.core.model;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * LLM 模块能力声明。
 *
 * @author zhanghongyu
 */
@Data
@Builder
public class LlmCapabilities {

    /**
     * 总开关
     */
    private boolean enabled;

    /**
     * 分能力开关
     */
    private Map<String, Boolean> capabilityFlags;
}
