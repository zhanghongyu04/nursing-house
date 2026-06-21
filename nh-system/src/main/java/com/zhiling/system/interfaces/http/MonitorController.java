package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.framework.monitor.service.MonitorService;
import com.zhiling.model.entity.LoginLog;
import com.zhiling.model.vo.OnlineUserVo;
import com.zhiling.system.application.service.LoginMonitorService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/monitor")
@Slf4j
@Tag(name = "运维监测", description = "运维监测")
/**
 * MonitorController
 *
 * @author zhanghongyu
 */
public class MonitorController {
    private final MonitorService monitorService;
    private final LoginMonitorService loginMonitorService;

    /**
     * 构造器：MonitorController
     *
     * @author zhanghongyu
     */
    public MonitorController(MonitorService monitorService, LoginMonitorService loginMonitorService) {
        this.monitorService = monitorService;
        this.loginMonitorService = loginMonitorService;
    }

    /**
     * 方法：monitorIp
     *
     * @author zhanghongyu
     */
    @GetMapping("/ipMonitor")
    @Operation(summary = "获取用户IP及IP解析地址")
    public Result<Map<String, String>> monitorIp(HttpServletRequest request) {
        return Result.success(monitorService.monitorIp(request));
    }

    @GetMapping("/getRedisInfo")
    @Operation(summary = "获取缓存监控信息")
    public Result getInfo() throws Exception {
        return Result.success(monitorService.getRedisInfo());
    }

    @GetMapping("/serverMonitor")
    @Operation(summary = "获取服务器监控信息")
    public Result getServerInfo() throws Exception {
        return Result.success(monitorService.getServerInfo());
    }

    @GetMapping("/loginLogs")
    @Operation(summary = "分页查询登录记录")
    public Result<IPage<LoginLog>> pageLoginLogs(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) Integer successFlag) {
        return Result.success(loginMonitorService.pageLoginLogs(pageNum, pageSize, username, successFlag));
    }

    @GetMapping("/onlineUsers")
    @Operation(summary = "查询在线用户")
    public Result<List<OnlineUserVo>> listOnlineUsers() {
        return Result.success(loginMonitorService.listOnlineUsers());
    }

    @PostMapping("/kickOut")
    @Operation(summary = "踢下线在线用户")
    public Result<Boolean> kickOut(@RequestParam String username) {
        return Result.success(loginMonitorService.kickOut(username));
    }



}


