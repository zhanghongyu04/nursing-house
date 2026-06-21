package com.zhiling.system.application.service;

import com.zhiling.system.application.dto.CameraPlayConfigDTO;
import com.zhiling.system.application.dto.VideoAlertHandleRequest;
import com.zhiling.system.application.dto.VideoPtzRequest;
import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;

import java.util.List;

/**
 * 视频监控服务接口
 *
 * @author zhanghongyu
 */
public interface VideoService {

    /**
     * 添加视频监控
     * @param camera
     */
    Boolean add(Camera camera);

    /**
     * 删除视频监控
     * @param cameraId
     * @return
     */
    Boolean delete(Long cameraId);

    /**
     * 查询视频监控。
     */
    List<Camera> list(Long sanaId, String sanaName);

    /**
     * 更新视频监控
     * @param camera
     * @return
     */
    Boolean update(Camera camera);

    /**
     * 查询视频告警列表。
     */
    List<VideoAlert> getAlerts(Long sanaId, String sanaName);

    /**
     * 处理视频告警。
     */
    Boolean handleAlert(VideoAlertHandleRequest request);

    /**
     * 获取摄像头播放配置。
     */
    CameraPlayConfigDTO getPlayConfig(Long cameraId);

    /**
     * 开始云台控制。
     */
    void startPtz(Long cameraId, VideoPtzRequest request);

    /**
     * 停止云台控制。
     */
    void stopPtz(Long cameraId);

    /**
     * 从萤石平台同步所有摄像头的在线状态到数据库。
     *
     * @return 更新的摄像头数量
     */
    int syncOnlineStatus();
}


