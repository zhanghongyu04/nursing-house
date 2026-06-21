package com.zhiling.system.sanaimage.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.SanaImagePageQueryDto;
import com.zhiling.model.entity.SanaImage;

/**
 * 机构图片域服务接口。
 *
 * @author zhanghongyu
 */
public interface SanaImageDomainService {

    Boolean save(SanaImage sanaImage);

    Boolean remove(SanaImage sanaImage);

    PageResult page(SanaImagePageQueryDto sanaImage);
}