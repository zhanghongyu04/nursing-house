package com.zhiling.system.application.repository;

import com.zhiling.model.entity.Role;

import java.util.List;

/**
 * 角色仓储接口。
 *
 * 定义 application 层所需的角色数据访问契约，
 * 由 infrastructure 层提供实现，遵循依赖倒置原则。
 *
 * @author zhanghongyu
 */
public interface RoleRepository {

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