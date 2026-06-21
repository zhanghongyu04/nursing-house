package com.zhiling.framework.autoconfigure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM 子能力配置。
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nh.llm")
public class NhLlmProperties {

    private boolean enabled = true;
    private boolean ragEnabled = true;
    private boolean toolEnabled = true;
    private boolean memoryEnabled = true;
    private boolean guardEnabled = true;
}
