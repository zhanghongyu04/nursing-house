package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;
import com.zhiling.system.application.dto.CameraPlayConfigDTO;
import com.zhiling.system.application.dto.VideoAlertHandleRequest;
import com.zhiling.system.application.dto.VideoPtzRequest;
import com.zhiling.system.application.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/video")
@Slf4j
@Tag(name = "视频监控", description = "视频监控")
/**
 * VideoController
 *
 * @author zhanghongyu
 */
public class VideoController {
    private final VideoService videoService;

    /**
     * 构造器：VideoController
     *
     * @author zhanghongyu
     */
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /**
     * 方法：addCamera
     *
     * @author zhanghongyu
     */
    @PostMapping("/add")
    @Operation(summary = "添加视频监控")
    public Result<Boolean> addCamera(@RequestBody Camera camera) {
        return Result.success(videoService.add(camera));
    }

    /**
     * 方法：RequestParam
     *
     * @author zhanghongyu
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除视频监控")
    public Result<Boolean> deleteCamera(@RequestParam("cameraId") Long cameraId) {
        return Result.success(videoService.delete(cameraId));
    }

    /**
     * 方法：updateCamera
     *
     * @author zhanghongyu
     */
    @PostMapping("/update")
    @Operation(summary = "更新视频监控")
    public Result<Boolean> updateCamera(@RequestBody Camera camera) {
        return Result.success(videoService.update(camera));
    }

    @GetMapping("/list")
    @Operation(summary = "视频监控列表")
    public Result<List<Camera>> list(@RequestParam(value = "sanaId", required = false) Long sanaId,
                                     @RequestParam(value = "sanaName", required = false) String sanaName) {
        return Result.success(videoService.list(sanaId, sanaName));
    }

    @GetMapping("/getAlerts")
    @Operation(summary = "获取视频监控告警")
    public Result<List<VideoAlert>> getAlerts(@RequestParam(value = "sanaId", required = false) Long sanaId,
                                              @RequestParam(value = "sanaName", required = false) String sanaName) {
        return Result.success(videoService.getAlerts(sanaId, sanaName));
    }

    /**
     * 方法：handleAlert
     *
     * @author zhanghongyu
     */
    @PostMapping("/handleAlert")
    @Operation(summary = "处理视频监控告警")
    public Result<Boolean> handleAlert(@RequestBody VideoAlertHandleRequest request) {
        return Result.success(videoService.handleAlert(request));
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @GetMapping("/{cameraId}/play-config")
    @Operation(summary = "获取摄像头播放配置")
    public Result<CameraPlayConfigDTO> getPlayConfig(@PathVariable("cameraId") Long cameraId) {
        return Result.success(videoService.getPlayConfig(cameraId));
    }

    @PostMapping("/{cameraId}/ptz/start")
    @Operation(summary = "开始云台控制")
    public Result<Boolean> startPtz(@PathVariable("cameraId") Long cameraId,
                                    @RequestBody VideoPtzRequest request) {
        videoService.startPtz(cameraId, request);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 方法：PathVariable
     *
     * @author zhanghongyu
     */
    @PostMapping("/{cameraId}/ptz/stop")
    @Operation(summary = "停止云台控制")
    public Result<Boolean> stopPtz(@PathVariable("cameraId") Long cameraId) {
        videoService.stopPtz(cameraId);
        return Result.success(Boolean.TRUE);
    }

}