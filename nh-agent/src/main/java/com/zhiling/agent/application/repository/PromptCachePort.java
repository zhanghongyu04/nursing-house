package com.zhiling.agent.application.repository;

/**

 * PromptCachePort

 *

 * @author zhanghongyu

 */

public interface PromptCachePort {

    String get(String key);

    void set(String key, String value);
}

