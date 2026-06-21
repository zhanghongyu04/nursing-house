package com.zhiling.system.infrastructure.persistence.command.impl;

import com.zhiling.model.entity.NursingTask;
import com.zhiling.system.infrastructure.persistence.command.NursingTaskCommandService;
import com.zhiling.system.infrastructure.persistence.mapper.NursingTaskMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 护理任务命令服务实现
 *
 * @author zhanghongyu
 */
@Service
public class NursingTaskCommandServiceImpl implements NursingTaskCommandService {

    private final NursingTaskMapper nursingTaskMapper;

    /**
     * 构造器：NursingTaskCommandServiceImpl
     *
     * @author zhanghongyu
     */
    public NursingTaskCommandServiceImpl(NursingTaskMapper nursingTaskMapper) {
        this.nursingTaskMapper = nursingTaskMapper;
    }

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(NursingTask nursingTask) {
        return nursingTaskMapper.insert(nursingTask) > 0;
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(NursingTask nursingTask) {
        return nursingTaskMapper.updateById(nursingTask) > 0;
    }

    /**
     * 方法：deleteById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteById(Long id) {
        return nursingTaskMapper.deleteById(id) > 0;
    }

    /**
     * 方法：markRunningTasks
     *
     * @author zhanghongyu
     */
    @Override
    public int markRunningTasks(LocalDateTime now, Integer runningStatus) {
        return nursingTaskMapper.markRunningTasks(now, runningStatus);
    }

    /**
     * 方法：markOverdueTasks
     *
     * @author zhanghongyu
     */
    @Override
    public int markOverdueTasks(LocalDateTime now, Integer overdueStatus) {
        return nursingTaskMapper.markOverdueTasks(now, overdueStatus);
    }
}