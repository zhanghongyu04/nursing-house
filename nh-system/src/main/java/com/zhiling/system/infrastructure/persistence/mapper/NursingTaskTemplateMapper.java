package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.TaskTemplatePageQueryDto;
import com.zhiling.model.entity.NursingTaskTemplate;
import com.zhiling.system.infrastructure.persistence.provider.NursingTaskTemplateSqlProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * 护理任务模板Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface NursingTaskTemplateMapper extends BaseMapper<NursingTaskTemplate> {

    @SelectProvider(type = NursingTaskTemplateSqlProvider.class, method = "page")
    IPage<NursingTaskTemplate> page(Page<NursingTaskTemplate> page, @Param("dto") TaskTemplatePageQueryDto dto);
}
