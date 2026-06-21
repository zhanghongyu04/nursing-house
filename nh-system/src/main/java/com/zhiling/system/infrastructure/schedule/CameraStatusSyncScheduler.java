package com.zhiling.system.infrastructure.schedule;

import com.zhiling.system.application.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 摄像头在线状态定时同步。
 *
 * 每 3 分钟从萤石平台拉取设备在线状态，更新到数据库。
 *
 * @author zhanghongyu
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CameraStatusSyncScheduler {

    private final VideoService videoService;

    /**
     * 每 1 分钟同步一次摄像头在线状态。
     * 使用 fixedDelay 确保上一次执行完毕后再开始下一次。
     */
    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 30 * 1000)
    public void syncCameraOnlineStatus() {
        try {
            log.info("[CameraSync] 开始同步摄像头在线状态...");
            int updated = videoService.syncOnlineStatus();
            log.info("[CameraSync] 同步结束, 更新 {} 条", updated);
        } catch (Exception e) {
            log.error("[CameraSync] 同步摄像头在线状态失败: {}", e.getMessage(), e);
        }
    }
}