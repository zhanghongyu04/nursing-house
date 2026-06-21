package com.zhiling.system.infrastructure.persistence.query;

import com.zhiling.model.entity.Resource;

import java.util.List;

/**
 * 资源查询服务接口。
 *
 * @author zhanghongyu
 */
public interface ResourceQueryService {

    /**
     * 根据角色 ID 查询资源列表。
     */
    List<Resource> listByRoleId(Long roleId);
}
