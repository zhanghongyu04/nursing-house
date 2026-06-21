package com.zhiling.system.infrastructure.persistence.query;

import com.zhiling.model.dto.SanaImagePageQueryDto;
import com.zhiling.model.entity.SanaImage;

import java.util.List;

/**
 * 机构图片查询服务接口。
 *
 * @author zhanghongyu
 */
public interface SanaImageQueryService {

    /**
     * 根据图片 URL 查询图片
     */
    SanaImage selectByImageUrl(String imageUrl);

    /**
     * 根据机构名称查询机构 ID
     */
    Long selectSanaIdByName(String sanaName);

    /**
     * 分页查询图片
     */
    com.baomidou.mybatisplus.core.metadata.IPage<SanaImage> selectPage(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<SanaImage> page,
            SanaImagePageQueryDto dto);

    /**
     * 保存图片
     */
    boolean save(SanaImage sanaImage);

    /**
     * 根据图片 URL 删除
     */
    boolean removeByImageUrl(String imageUrl);
}