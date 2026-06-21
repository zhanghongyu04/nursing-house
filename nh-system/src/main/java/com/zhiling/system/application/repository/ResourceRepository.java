package com.zhiling.system.application.repository;

import com.zhiling.model.entity.Resource;

import java.util.List;

/**
 * 资源仓储接口。
 *
 * 定义 application 层所需的资源数据访问契约，
 * 由 infrastructure 层提供实现，遵循依赖倒置原则。
 *
 * @author zhanghongyu
 */
public interface ResourceRepository {

    /**
     * 根据角色 ID 查询资源列表
     */
    List<Resource> listByRoleId(Long roleId);
}