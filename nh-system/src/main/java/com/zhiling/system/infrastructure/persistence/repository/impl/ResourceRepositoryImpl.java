package com.zhiling.system.infrastructure.persistence.repository.impl;

import com.zhiling.model.entity.Resource;
import com.zhiling.system.application.repository.ResourceRepository;
import com.zhiling.system.infrastructure.persistence.query.ResourceQueryService;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 资源仓储实现。
 *
 * 委托给 ResourceQueryService，
 * 实现 application 层定义的 ResourceRepository 契约。
 *
 * @author zhanghongyu
 */
@Repository
public class ResourceRepositoryImpl implements ResourceRepository {

    private final ResourceQueryService resourceQueryService;

    /**
     * 构造器：ResourceRepositoryImpl
     *
     * @author zhanghongyu
     */
    public ResourceRepositoryImpl(ResourceQueryService resourceQueryService) {
        this.resourceQueryService = resourceQueryService;
    }

    /**
     * 方法：listByRoleId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Resource> listByRoleId(Long roleId) {
        return resourceQueryService.listByRoleId(roleId);
    }
}