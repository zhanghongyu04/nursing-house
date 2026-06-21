package com.zhiling.system.infrastructure.persistence.repository.impl;

import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;
import com.zhiling.system.application.repository.VideoRepository;
import com.zhiling.system.infrastructure.persistence.command.VideoCommandService;
import com.zhiling.system.infrastructure.persistence.query.VideoQueryService;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 视频仓储实现。
 *
 * 委托给 VideoQueryService 和 VideoCommandService，
 * 实现 application 层定义的 VideoRepository 契约。
 *
 * @author zhanghongyu
 */
@Repository
public class VideoRepositoryImpl implements VideoRepository {

    private final VideoQueryService videoQueryService;
    private final VideoCommandService videoCommandService;

    public VideoRepositoryImpl(VideoQueryService videoQueryService,
                                VideoCommandService videoCommandService) {
        this.videoQueryService = videoQueryService;
        this.videoCommandService = videoCommandService;
    }

    // === 查询操作 ===

    /**
     * 方法：listCameraBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Camera> listCameraBySanaId(Long sanaId) {
        return videoQueryService.listBySanaId(sanaId);
    }

    /**
     * 方法：getCameraById
     *
     * @author zhanghongyu
     */
    @Override
    public Camera getCameraById(Long cameraId) {
        return videoQueryService.getById(cameraId);
    }

    /**
     * 方法：listAlertsBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public List<VideoAlert> listAlertsBySanaId(Long sanaId) {
        return videoQueryService.listAlertsBySanaId(sanaId);
    }

    /**
     * 方法：listAllWithDeviceSerial
     *
     * @author zhanghongyu
     */
    @Override
    public List<Camera> listAllWithDeviceSerial() {
        return videoQueryService.listAllWithDeviceSerial();
    }

    /**
     * 方法：getAlertById
     *
     * @author zhanghongyu
     */
    @Override
    public VideoAlert getAlertById(Long alertId) {
        return videoQueryService.getAlertById(alertId);
    }

    // === 命令操作 ===

    /**
     * 方法：insertCamera
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insertCamera(Camera camera) {
        return videoCommandService.insert(camera);
    }

    /**
     * 方法：updateCameraById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateCameraById(Camera camera) {
        return videoCommandService.updateById(camera);
    }

    /**
     * 方法：deleteCameraById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteCameraById(Long cameraId) {
        return videoCommandService.deleteById(cameraId);
    }

    /**
     * 方法：handleAlert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean handleAlert(Long alertId, Long handledBy, String handleRemark) {
        return videoCommandService.handleAlert(alertId, handledBy, handleRemark);
    }

    /**
     * 方法：insertStatusLog
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insertStatusLog(Long sanaId, Long cameraId, String content) {
        return videoCommandService.insertStatusLog(sanaId, cameraId, content);
    }

}