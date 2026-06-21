package com.zhiling.system.interfaces.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.SanaImagePageQueryDto;
import com.zhiling.model.entity.SanaImage;
import com.zhiling.system.sanaimage.service.SanaImageQueryService;
import com.zhiling.system.infrastructure.persistence.query.SanatoriumQueryService;
import org.springframework.stereotype.Component;

/**
 * 机构图片域查询适配器。
 *
 * 适配领域服务接口，委托持久化查询服务执行查询与命令。
 *
 * @author zhanghongyu
 */
@Component
public class SanaImageQueryServiceAdapter implements SanaImageQueryService {

    private final com.zhiling.system.infrastructure.persistence.query.SanaImageQueryService sanaImagePersistenceQueryService;
    private final SanatoriumQueryService sanatoriumQueryService;

    public SanaImageQueryServiceAdapter(
            com.zhiling.system.infrastructure.persistence.query.SanaImageQueryService sanaImagePersistenceQueryService,
            SanatoriumQueryService sanatoriumQueryService) {
        this.sanaImagePersistenceQueryService = sanaImagePersistenceQueryService;
        this.sanatoriumQueryService = sanatoriumQueryService;
    }

    /**
     * 方法：selectSanaIdByName
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectSanaIdByName(String sanaName) {
        return sanatoriumQueryService.selectIdByName(sanaName);
    }

    /**
     * 方法：save
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean save(SanaImage sanaImage) {
        return sanaImagePersistenceQueryService.save(sanaImage);
    }

    /**
     * 方法：selectByImageUrl
     *
     * @author zhanghongyu
     */
    @Override
    public SanaImage selectByImageUrl(String imageUrl) {
        return sanaImagePersistenceQueryService.selectByImageUrl(imageUrl);
    }

    /**
     * 方法：removeByImageUrl
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean removeByImageUrl(String imageUrl) {
        return sanaImagePersistenceQueryService.removeByImageUrl(imageUrl);
    }

    /**
     * 方法：page
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult page(SanaImagePageQueryDto sanaImagePageQueryDto) {
        Page<SanaImage> page = new Page<>(sanaImagePageQueryDto.getPage(), sanaImagePageQueryDto.getPageSize());
        IPage<SanaImage> result = sanaImagePersistenceQueryService.selectPage(page, sanaImagePageQueryDto);
        return new PageResult(result.getTotal(), result.getRecords());
    }
}
