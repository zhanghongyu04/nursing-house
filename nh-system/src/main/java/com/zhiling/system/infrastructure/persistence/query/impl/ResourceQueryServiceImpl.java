package com.zhiling.system.infrastructure.persistence.query.impl;

import com.zhiling.model.entity.Resource;
import com.zhiling.system.infrastructure.persistence.mapper.ResourceMapper;
import com.zhiling.system.infrastructure.persistence.query.ResourceQueryService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 资源查询服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class ResourceQueryServiceImpl implements ResourceQueryService {

    private final ResourceMapper resourceMapper;

    /**
     * 构造器：ResourceQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public ResourceQueryServiceImpl(ResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
    }

    /**
     * 方法：listByRoleId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Resource> listByRoleId(Long roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }
        List<Resource> resources = resourceMapper.getResourceByRoleId(roleId);
        return resources != null ? resources : Collections.emptyList();
    }
}