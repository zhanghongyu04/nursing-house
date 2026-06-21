package com.zhiling.system.infrastructure.persistence.query.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.model.dto.SanaImagePageQueryDto;
import com.zhiling.model.entity.SanaImage;
import com.zhiling.system.infrastructure.persistence.mapper.SanaImageMapper;
import com.zhiling.system.infrastructure.persistence.mapper.SanatoriumMapper;
import com.zhiling.system.infrastructure.persistence.query.SanaImageQueryService;
import org.springframework.stereotype.Service;

/**
 * 机构图片查询服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class SanaImageQueryServiceImpl implements SanaImageQueryService {

    private final SanaImageMapper sanaImageMapper;
    private final SanatoriumMapper sanatoriumMapper;

    public SanaImageQueryServiceImpl(SanaImageMapper sanaImageMapper,
                                     SanatoriumMapper sanatoriumMapper) {
        this.sanaImageMapper = sanaImageMapper;
        this.sanatoriumMapper = sanatoriumMapper;
    }

    /**
     * 方法：selectByImageUrl
     *
     * @author zhanghongyu
     */
    @Override
    public SanaImage selectByImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        return sanaImageMapper.selectOne(
                new LambdaQueryWrapper<SanaImage>()
                        .eq(SanaImage::getImageUrl, imageUrl)
        );
    }

    /**
     * 方法：selectSanaIdByName
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectSanaIdByName(String sanaName) {
        if (sanaName == null || sanaName.trim().isEmpty()) {
            return null;
        }
        return sanatoriumMapper.selectIdByName(sanaName);
    }

    /**
     * 方法：selectPage
     *
     * @author zhanghongyu
     */
    @Override
    public IPage<SanaImage> selectPage(Page<SanaImage> page, SanaImagePageQueryDto dto) {
        return sanaImageMapper.selectPage(page, dto);
    }

    /**
     * 方法：save
     *
     * @author zhanghongyu
     */
    @Override
    public boolean save(SanaImage sanaImage) {
        return sanaImageMapper.insert(sanaImage) > 0;
    }

    /**
     * 方法：removeByImageUrl
     *
     * @author zhanghongyu
     */
    @Override
    public boolean removeByImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return false;
        }
        return sanaImageMapper.delete(
                new LambdaQueryWrapper<SanaImage>()
                        .eq(SanaImage::getImageUrl, imageUrl)
        ) > 0;
    }
}