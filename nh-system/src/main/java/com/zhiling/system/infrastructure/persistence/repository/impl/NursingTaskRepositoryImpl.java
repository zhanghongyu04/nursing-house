package com.zhiling.system.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.entity.NursingTask;
import com.zhiling.system.application.repository.NursingTaskRepository;
import com.zhiling.system.infrastructure.persistence.command.NursingTaskCommandService;
import com.zhiling.system.infrastructure.persistence.query.NursingTaskQueryService;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 护理任务仓储实现
 *
 * @author zhanghongyu
 */
@Repository
public class NursingTaskRepositoryImpl implements NursingTaskRepository {

    private final NursingTaskQueryService nursingTaskQueryService;
    private final NursingTaskCommandService nursingTaskCommandService;

    public NursingTaskRepositoryImpl(NursingTaskQueryService nursingTaskQueryService,
                                     NursingTaskCommandService nursingTaskCommandService) {
        this.nursingTaskQueryService = nursingTaskQueryService;
        this.nursingTaskCommandService = nursingTaskCommandService;
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public NursingTask selectById(Long id) {
        return nursingTaskQueryService.selectById(id);
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<NursingTask> page(Page<NursingTask> page, NursingTaskPageQueryDto dto) {
        return nursingTaskQueryService.page(page, dto);
    }

    /**
     * 方法：myPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<NursingTask> myPage(Page<NursingTask> page, NursingTaskMyPageQueryDto dto) {
        return nursingTaskQueryService.myPage(page, dto);
    }

    /**
     * 方法：insert
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insert(NursingTask nursingTask) {
        return nursingTaskCommandService.insert(nursingTask);
    }

    /**
     * 方法：updateById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean updateById(NursingTask nursingTask) {
        return nursingTaskCommandService.updateById(nursingTask);
    }

    /**
     * 方法：deleteById
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteById(Long id) {
        return nursingTaskCommandService.deleteById(id);
    }

    /**
     * 方法：markRunningTasks
     *
     * @author zhanghongyu
     */
    @Override
    public int markRunningTasks(LocalDateTime now, Integer runningStatus) {
        return nursingTaskCommandService.markRunningTasks(now, runningStatus);
    }

    /**
     * 方法：markOverdueTasks
     *
     * @author zhanghongyu
     */
    @Override
    public int markOverdueTasks(LocalDateTime now, Integer overdueStatus) {
        return nursingTaskCommandService.markOverdueTasks(now, overdueStatus);
    }
}