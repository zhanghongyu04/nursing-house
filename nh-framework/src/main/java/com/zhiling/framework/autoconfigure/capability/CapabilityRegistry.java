package com.zhiling.framework.autoconfigure.capability;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 能力注册中心。
 *
 * @author zhanghongyu
 */
@Slf4j
public class CapabilityRegistry {

    private final Map<String, Boolean> capabilities = new LinkedHashMap<>();

    /**
     * 方法：register
     *
     * @author zhanghongyu
     */
    public void register(String capability, boolean enabled) {
        capabilities.put(capability, enabled);
    }

    /**
     * 方法：snapshot
     *
     * @author zhanghongyu
     */
    public Map<String, Boolean> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(capabilities));
    }

    /**
     * 方法：logMatrix
     *
     * @author zhanghongyu
     */
    public void logMatrix() {
        log.info("[CapabilityMatrix] {}", capabilities);
    }
}