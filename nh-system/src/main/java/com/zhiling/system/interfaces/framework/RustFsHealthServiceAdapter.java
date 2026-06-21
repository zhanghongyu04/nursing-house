package com.zhiling.system.interfaces.framework;

import com.zhiling.framework.file.RustFsHealthService;
import com.zhiling.system.application.service.RustFsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RustFS 健康检查服务适配器。
 *
 * @author zhanghongyu
 */
@Component
@Slf4j
public class RustFsHealthServiceAdapter implements RustFsHealthService {

    private final RustFsService rustFsService;

    /**
     * 构造器：RustFsHealthServiceAdapter
     *
     * @author zhanghongyu
     */
    public RustFsHealthServiceAdapter(RustFsService rustFsService) {
        this.rustFsService = rustFsService;
    }

    /**
     * 方法：health
     *
     * @author zhanghongyu
     */
    @Override
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        try {
            var objects = rustFsService.listObjects("");
            result.put("status", "connected");
            result.put("objectCount", objects.size());
            result.put("message", "RustFS 连接正常");
            log.info("RustFS 健康检查成功: objectCount={}", objects.size());
        } catch (Exception e) {
            result.put("status", "disconnected");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            log.error("RustFS 健康检查失败", e);
        }
        return result;
    }

    /**
     * 方法：config
     *
     * @author zhanghongyu
     */
    @Override
    public Map<String, String> config() {
        Map<String, String> config = new HashMap<>();
        config.put("endpoint", "configured");
        config.put("bucketName", "configured");
        config.put("credentials", "configured");
        config.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return config;
    }
}
