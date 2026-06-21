package com.zhiling.system.application.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.NursingTaskDispatchDto;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.dto.NursingTaskUpdateDto;

/**
 * 护理任务服务
 * @author zhanghongyu
 */
public interface NursingTaskService {

    PageResult page(NursingTaskPageQueryDto queryDto);

    Boolean dispatch(NursingTaskDispatchDto dispatchDto);

    Boolean update(NursingTaskUpdateDto updateDto);

    Boolean cancel(Long id);

    Boolean reactivate(Long id);

    Boolean remove(Long id);

    PageResult myPage(NursingTaskMyPageQueryDto queryDto);

    Boolean complete(Long id);

    int syncTaskExecutionStatus();

    int markOverdueTasks();
}
