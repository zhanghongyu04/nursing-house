package com.zhiling.system.infrastructure.persistence.command.impl;

import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;
import com.zhiling.system.infrastructure.persistence.command.VideoCommandService;
import com.zhiling.system.infrastructure.persistence.mapper.VideoMapper;
import org.springframework.stereotype.Service;

/**
 * 视频命令服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class VideoCommandServiceImpl implements VideoCommandService {

    private final VideoMapper videoMapper;

    /**
     * 构造器：VideoCommandServiceImpl
     *
     * @author zhanghongyu
     */
    public VideoCommandServiceImpl(VideoMapper videoMapper) {
        this.videoMapper = videoMapper;
    }

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(Camera camera) {
        return videoMapper.insert(camera) > 0;
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(Camera camera) {
        return videoMapper.updateById(camera) > 0;
    }

    /**
     * 方法：deleteById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteById(Long cameraId) {
        return videoMapper.deleteById(cameraId) > 0;
    }

    /**
     * 方法：handleAlert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean handleAlert(Long alertId, Long handledBy, String handleRemark) {
        return videoMapper.handleAlert(alertId, handledBy, handleRemark) > 0;
    }

    /**
     * 方法：insertStatusLog
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insertStatusLog(Long sanaId, Long cameraId, String content) {
        return videoMapper.insertStatusLog(sanaId, cameraId, content, java.time.LocalDateTime.now()) > 0;
    }
}