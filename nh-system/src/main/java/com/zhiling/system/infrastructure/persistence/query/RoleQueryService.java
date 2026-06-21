package com.zhiling.system.infrastructure.persistence.query;

import com.zhiling.model.entity.Role;

import java.util.List;

/**
 * 角色查询服务接口。
 *
 * @author zhanghongyu
 */
public interface RoleQueryService {

    /**
     * 根据用户 ID 查询角色列表
     */
    List<Role> listByUserId(Long userId);

    /**
     * 根据标签查询角色 ID
     */
    Long selectIdByLabel(String label);

    /**
     * 根据 ID 查询角色
     */
    Role selectById(Long id);
}