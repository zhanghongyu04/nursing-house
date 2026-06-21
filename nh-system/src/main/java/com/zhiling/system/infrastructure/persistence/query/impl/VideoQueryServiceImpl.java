package com.zhiling.system.infrastructure.persistence.query.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiling.model.entity.Camera;
import com.zhiling.model.entity.VideoAlert;
import com.zhiling.system.infrastructure.persistence.mapper.VideoMapper;
import com.zhiling.system.infrastructure.persistence.query.VideoQueryService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 视频查询服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class VideoQueryServiceImpl implements VideoQueryService {

    private final VideoMapper videoMapper;

    /**
     * 构造器：VideoQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public VideoQueryServiceImpl(VideoMapper videoMapper) {
        this.videoMapper = videoMapper;
    }

    /**
     * 方法：listBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Camera> listBySanaId(Long sanaId) {
        if (sanaId == null) {
            return Collections.emptyList();
        }
        List<Camera> cameras = videoMapper.listBySanaId(sanaId);
        return cameras != null ? cameras : Collections.emptyList();
    }

    /**
     * 方法：getById
     *
     * @author zhanghongyu
     */
    @Override
    public Camera getById(Long cameraId) {
        if (cameraId == null) {
            return null;
        }
        return videoMapper.selectById(cameraId);
    }

    /**
     * 方法：getAlertById
     *
     * @author zhanghongyu
     */
    @Override
    public VideoAlert getAlertById(Long alertId) {
        if (alertId == null) {
            return null;
        }
        return videoMapper.getAlertById(alertId);
    }

    /**
     * 方法：listAlertsBySanaId
     *
     * @author zhanghongyu
     */
    @Override
    public List<VideoAlert> listAlertsBySanaId(Long sanaId) {
        if (sanaId == null) {
            return Collections.emptyList();
        }
        List<VideoAlert> alerts = videoMapper.getAlertsBySanaId(sanaId);
        return alerts != null ? alerts : Collections.emptyList();
    }

    /**
     * 方法：listAllWithDeviceSerial
     *
     * @author zhanghongyu
     */
    @Override
    public List<Camera> listAllWithDeviceSerial() {
        return videoMapper.selectList(
            new LambdaQueryWrapper<Camera>()
                .isNotNull(Camera::getDeviceSerial)
                .ne(Camera::getDeviceSerial, "")
        );
    }
}