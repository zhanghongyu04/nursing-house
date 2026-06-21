package com.zhiling.system.application.repository;

import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;

import java.util.List;

/**
 * 视频仓储接口。
 *
 * 定义 application 层所需的视频数据访问契约，
 * 由 infrastructure 层提供实现，遵循依赖倒置原则。
 *
 * @author zhanghongyu
 */
public interface VideoRepository {

    // === 查询操作 ===

    /**
     * 根据机构 ID 查询摄像头列表
     */
    List<Camera> listCameraBySanaId(Long sanaId);

    /**
     * 根据主键查询摄像头
     */
    Camera getCameraById(Long cameraId);

    /**
     * 根据机构 ID 查询视频告警。
     */
    List<VideoAlert> listAlertsBySanaId(Long sanaId);

    /**
     * 查询所有已配置萤石设备序列号的摄像头。
     */
    List<Camera> listAllWithDeviceSerial();

    /**
     * 根据主键查询视频告警。
     */
    VideoAlert getAlertById(Long alertId);

    // === 命令操作 ===

    /**
     * 添加摄像头
     */
    boolean insertCamera(Camera camera);

    /**
     * 更新摄像头
     */
    boolean updateCameraById(Camera camera);

    /**
     * 删除摄像头
     */
    boolean deleteCameraById(Long cameraId);

    /**
     * 处理视频告警。
     */
    boolean handleAlert(Long alertId, Long handledBy, String handleRemark);

    /**
     * 插入状态变更日志。
     */
    boolean insertStatusLog(Long sanaId, Long cameraId, String content);

}