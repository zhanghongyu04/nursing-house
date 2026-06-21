package com.zhiling.system.application.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.entity.NursingTask;

import java.time.LocalDateTime;

/**
 * 护理任务仓储接口
 * @author zhanghongyu
 */
public interface NursingTaskRepository {

    NursingTask selectById(Long id);

    IPage<NursingTask> page(Page<NursingTask> page, NursingTaskPageQueryDto dto);

    IPage<NursingTask> myPage(Page<NursingTask> page, NursingTaskMyPageQueryDto dto);

    boolean insert(NursingTask nursingTask);

    boolean updateById(NursingTask nursingTask);

    boolean deleteById(Long id);

    int markRunningTasks(LocalDateTime now, Integer runningStatus);

    int markOverdueTasks(LocalDateTime now, Integer overdueStatus);
}
