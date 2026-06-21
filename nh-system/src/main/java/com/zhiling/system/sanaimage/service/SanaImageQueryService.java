package com.zhiling.system.sanaimage.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.SanaImagePageQueryDto;
import com.zhiling.model.entity.SanaImage;

/**
 * 机构图片查询与持久化服务。
 *
 * @author zhanghongyu
 */
public interface SanaImageQueryService {

    Long selectSanaIdByName(String sanaName);

    Boolean save(SanaImage sanaImage);

    SanaImage selectByImageUrl(String imageUrl);

    Boolean removeByImageUrl(String imageUrl);

    PageResult page(SanaImagePageQueryDto sanaImagePageQueryDto);
}
