package com.zhiling.system.infrastructure.persistence.command;

import com.zhiling.model.entity.NursingTask;

import java.time.LocalDateTime;

/**
 * 护理任务命令服务
 * @author zhanghongyu
 */
public interface NursingTaskCommandService {

    boolean insert(NursingTask nursingTask);

    boolean updateById(NursingTask nursingTask);

    boolean deleteById(Long id);

    int markRunningTasks(LocalDateTime now, Integer runningStatus);

    int markOverdueTasks(LocalDateTime now, Integer overdueStatus);
}
