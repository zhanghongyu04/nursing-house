package com.zhiling.system.infrastructure.persistence.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingLogMyPageQueryDto;
import com.zhiling.model.dto.NursingLogPageQueryDto;
import com.zhiling.model.entity.NursingLog;

/**
 * 护理日志查询服务
 * @author zhanghongyu
 */
public interface NursingLogQueryService {

    NursingLog selectById(Long id);

    NursingLog selectByTaskIdAndNurseUserId(Long taskId, Long nurseUserId);

    IPage<NursingLog> page(Page<NursingLog> page, NursingLogPageQueryDto dto);

    IPage<NursingLog> myPage(Page<NursingLog> page, NursingLogMyPageQueryDto dto);
}
