package com.zhiling.system.application.dto;

/**

 * CameraPlayConfigDTO

 *

 * @author zhanghongyu

 */

public record CameraPlayConfigDTO(
        Long cameraId,
        String cameraName,
        String deviceSerial,
        Integer channelNo,
        String protocol,
        String streamQuality,
        String playUrl,
        String accessToken,
        Integer cameraStatus,
        Integer onlineStatus
) {
}

