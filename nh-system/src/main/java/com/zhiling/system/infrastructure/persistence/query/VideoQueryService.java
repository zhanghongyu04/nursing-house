package com.zhiling.system.infrastructure.persistence.query;

import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;

import java.util.List;

/**
 * 视频查询服务接口。
 *
 * @author zhanghongyu
 */
public interface VideoQueryService {

    /**
     * 根据机构 ID 查询摄像头列表。
     */
    List<Camera> listBySanaId(Long sanaId);

    /**
     * 根据主键查询摄像头。
     */
    Camera getById(Long cameraId);

    /**
     * 根据主键查询视频告警。
     */
    VideoAlert getAlertById(Long alertId);

    /**
     * 根据机构 ID 查询视频告警。
     */
    List<VideoAlert> listAlertsBySanaId(Long sanaId);

    /**
     * 查询所有已配置萤石设备序列号的摄像头。
     */
    List<Camera> listAllWithDeviceSerial();
}