package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.framework.file.RustFsHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * RustFS 健康检查接口
 * 用于排查 RustFS 连接问题
 *
 * @author zhanghongyu
 */
@RestController
@RequestMapping("/api/v1/rustfs")
@Tag(name = "RustFS健康检查")
@Slf4j
public class RustFsHealthController {

    private final RustFsHealthService rustFsHealthService;

    /**
     * 构造器：RustFsHealthController
     *
     * @author zhanghongyu
     */
    public RustFsHealthController(RustFsHealthService rustFsHealthService) {
        this.rustFsHealthService = rustFsHealthService;
    }

    /**
     * 检查 RustFS 连接状态
     */
    @GetMapping("/health")
    @Operation(summary = "检查RustFS连接状态")
    public Result<Map<String, Object>> health() {
        return Result.success(rustFsHealthService.health());
    }

    /**
     * 获取 RustFS 配置信息（脱敏）
     */
    @GetMapping("/config")
    @Operation(summary = "获取RustFS配置信息")
    public Result<Map<String, String>> config() {
        return Result.success(rustFsHealthService.config());
    }
}
