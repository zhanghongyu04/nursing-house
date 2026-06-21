package com.zhiling.system.infrastructure.persistence.command.impl;

import com.zhiling.system.infrastructure.persistence.command.RoleCommandService;
import com.zhiling.system.infrastructure.persistence.mapper.RoleMapper;
import org.springframework.stereotype.Service;

/**
 * 角色命令服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class RoleCommandServiceImpl implements RoleCommandService {

    private final RoleMapper roleMapper;

    /**
     * 构造器：RoleCommandServiceImpl
     *
     * @author zhanghongyu
     */
    public RoleCommandServiceImpl(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * 方法：insertRoleUser
     *
     * @author zhanghongyu
     */
    @Override
    public boolean insertRoleUser(Long roleId, Long userId) {
        return roleMapper.insertRoleUser(roleId, userId);
    }

    /**
     * 方法：deleteRoleUser
     *
     * @author zhanghongyu
     */
    @Override
    public boolean deleteRoleUser(Long userId) {
        return roleMapper.deleteRoleUser(userId);
    }
}