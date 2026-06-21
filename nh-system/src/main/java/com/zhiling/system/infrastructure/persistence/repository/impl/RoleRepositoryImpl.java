package com.zhiling.system.infrastructure.persistence.repository.impl;

import com.zhiling.model.entity.Role;
import com.zhiling.system.application.repository.RoleRepository;
import com.zhiling.system.infrastructure.persistence.query.RoleQueryService;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色仓储实现。
 *
 * 委托给 RoleQueryService，
 * 实现 application 层定义的 RoleRepository 契约。
 *
 * @author zhanghongyu
 */
@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleQueryService roleQueryService;

    /**
     * 构造器：RoleRepositoryImpl
     *
     * @author zhanghongyu
     */
    public RoleRepositoryImpl(RoleQueryService roleQueryService) {
        this.roleQueryService = roleQueryService;
    }

    /**
     * 方法：listByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Role> listByUserId(Long userId) {
        return roleQueryService.listByUserId(userId);
    }

    /**
     * 方法：selectIdByLabel
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectIdByLabel(String label) {
        return roleQueryService.selectIdByLabel(label);
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public Role selectById(Long id) {
        return roleQueryService.selectById(id);
    }
}