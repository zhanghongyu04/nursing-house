package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.NursingLogMyPageQueryDto;
import com.zhiling.model.dto.NursingLogPageQueryDto;
import com.zhiling.model.entity.NursingLog;
import com.zhiling.system.infrastructure.persistence.provider.NursingLogSqlProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * 护理日志Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface NursingLogMapper extends BaseMapper<NursingLog> {

    /**
     * 机构侧日志分页
     */
    @SelectProvider(type = NursingLogSqlProvider.class, method = "page")
    IPage<NursingLog> page(Page<NursingLog> page, @Param("dto") NursingLogPageQueryDto dto);

    /**
     * 护理端我的日志分页
     */
    @SelectProvider(type = NursingLogSqlProvider.class, method = "myPage")
    IPage<NursingLog> myPage(Page<NursingLog> page, @Param("dto") NursingLogMyPageQueryDto dto);
}
