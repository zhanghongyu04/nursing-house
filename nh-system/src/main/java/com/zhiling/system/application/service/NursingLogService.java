package com.zhiling.system.application.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.NursingLogAddDto;
import com.zhiling.model.dto.NursingLogExportDto;
import com.zhiling.model.dto.NursingLogMyPageQueryDto;
import com.zhiling.model.dto.NursingLogPageQueryDto;
import com.zhiling.model.dto.NursingLogUpdateDto;

/**
 * 护理日志服务
 * @author zhanghongyu
 */
public interface NursingLogService {

    PageResult page(NursingLogPageQueryDto queryDto);

    PageResult myPage(NursingLogMyPageQueryDto queryDto);

    Boolean add(NursingLogAddDto addDto);

    Boolean update(Long id, NursingLogUpdateDto updateDto);

    byte[] export(NursingLogExportDto exportDto);
}
