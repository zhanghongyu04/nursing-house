package com.zhiling.system.application.service.impl;

import com.zhiling.model.entity.Role;
import com.zhiling.system.application.service.RoleService;
import com.zhiling.system.application.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色服务实现
 *
 * @author zhanghongyu
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    /**
     * 注入角色仓储，负责承接用户与角色关系查询能力。
     *
     * @author zhanghongyu
     */
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * 根据用户 ID 查询角色列表。
     *
     * @param id 用户 ID（字符串形式）
     * @return 用户关联的角色集合
     * @author zhanghongyu
     */
    @Override
    public List<Role> getRoleListByUserId(String id) {
        return roleRepository.listByUserId(Long.valueOf(id));
    }


}
