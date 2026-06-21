package com.zhiling.system.infrastructure.persistence.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingTaskMyPageQueryDto;
import com.zhiling.model.dto.NursingTaskPageQueryDto;
import com.zhiling.model.entity.NursingTask;

/**
 * 护理任务查询服务
 * @author zhanghongyu
 */
public interface NursingTaskQueryService {

    NursingTask selectById(Long id);

    IPage<NursingTask> page(Page<NursingTask> page, NursingTaskPageQueryDto dto);

    IPage<NursingTask> myPage(Page<NursingTask> page, NursingTaskMyPageQueryDto dto);
}
