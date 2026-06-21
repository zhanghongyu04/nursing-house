package com.zhiling.system.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.SanaImagePageQueryDto;
import com.zhiling.model.entity.SanaImage;
import com.zhiling.system.infrastructure.persistence.provider.SanaImageSqlProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

/**
 * 养老院图片 Mapper
 *
 * @author zhanghongyu
 */
@Mapper
public interface SanaImageMapper extends BaseMapper<SanaImage> {

    /**
     * 分页查询图片
     */
    @SelectProvider(type = SanaImageSqlProvider.class, method = "selectPage")
    IPage<SanaImage> selectPage(Page<SanaImage> page, @Param("sanaImage") SanaImagePageQueryDto sanaImage);
}

