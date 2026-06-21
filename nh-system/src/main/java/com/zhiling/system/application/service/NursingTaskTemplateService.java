package com.zhiling.system.application.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.TaskTemplateCreateDto;
import com.zhiling.model.dto.TaskTemplatePageQueryDto;
import com.zhiling.model.entity.NursingTaskTemplate;

import java.util.List;

/**
 * 护理任务模板服务
 * @author zhanghongyu
 */
public interface NursingTaskTemplateService {

    PageResult page(TaskTemplatePageQueryDto dto);

    Boolean create(TaskTemplateCreateDto dto);

    Boolean update(TaskTemplateCreateDto dto);

    Boolean toggleEnabled(Long id);

    Boolean remove(Long id);

    int generateTasksForTemplate(Long templateId);

    int generateAllDueTasks();

    List<NursingTaskTemplate> listActiveTemplates();
}
