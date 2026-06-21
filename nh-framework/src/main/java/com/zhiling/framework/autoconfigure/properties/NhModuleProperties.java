package com.zhiling.framework.autoconfigure.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模块开关配置。
 *
 * @author zhanghongyu
 */
@Data
@ConfigurationProperties(prefix = "nh.modules")
public class NhModuleProperties {

    /**
     * LLM 模块总开关
     */
    private boolean llmEnabled = true;
}
