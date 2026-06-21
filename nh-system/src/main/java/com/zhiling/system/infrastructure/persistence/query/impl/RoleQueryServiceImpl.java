package com.zhiling.system.infrastructure.persistence.query.impl;

import com.zhiling.model.entity.Role;
import com.zhiling.system.infrastructure.persistence.mapper.RoleMapper;
import com.zhiling.system.infrastructure.persistence.query.RoleQueryService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 角色查询服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class RoleQueryServiceImpl implements RoleQueryService {

    private final RoleMapper roleMapper;

    /**
     * 构造器：RoleQueryServiceImpl
     *
     * @author zhanghongyu
     */
    public RoleQueryServiceImpl(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    /**
     * 方法：listByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Role> listByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Role> roles = roleMapper.getRoleListByUserId(userId);
        return roles != null ? roles : Collections.emptyList();
    }

    /**
     * 方法：selectIdByLabel
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectIdByLabel(String label) {
        if (label == null || label.trim().isEmpty()) {
            return null;
        }
        return roleMapper.selectIdByLabel(label);
    }

    /**
     * 方法：selectById
     *
     * @author zhanghongyu
     */
    @Override
    public Role selectById(Long id) {
        if (id == null) {
            return null;
        }
        return roleMapper.selectById(id);
    }
}