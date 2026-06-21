package com.zhiling.system.infrastructure.persistence.command;

import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;

/**
 * 视频命令服务接口。
 *
 * @author zhanghongyu
 */
public interface VideoCommandService {

    boolean insert(Camera camera);

    boolean updateById(Camera camera);

    boolean deleteById(Long cameraId);

    boolean handleAlert(Long alertId, Long handledBy, String handleRemark);

    boolean insertStatusLog(Long sanaId, Long cameraId, String content);
}