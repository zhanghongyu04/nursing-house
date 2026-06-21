package com.zhiling.system.interfaces.system;

import com.zhiling.model.entity.Role;
import com.zhiling.system.auth.service.support.AuthRoleQueryService;
import com.zhiling.system.application.service.RoleService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 认证域角色查询适配器。
 *
 * @author zhanghongyu
 */
@Component
public class AuthRoleQueryServiceAdapter implements AuthRoleQueryService {

    private final RoleService roleService;

    /**
     * 构造器：AuthRoleQueryServiceAdapter
     *
     * @author zhanghongyu
     */
    public AuthRoleQueryServiceAdapter(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 方法：getRoleListByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Role> getRoleListByUserId(String userId) {
        return roleService.getRoleListByUserId(userId);
    }
}