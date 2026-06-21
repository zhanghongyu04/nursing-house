package com.zhiling.system.application.service.impl;

import com.zhiling.model.entity.Resource;
import com.zhiling.model.entity.Role;
import com.zhiling.system.application.service.ResourceService;
import com.zhiling.system.application.repository.ResourceRepository;
import com.zhiling.system.application.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 资源服务实现
 *
 * @author zhanghongyu
 */
@Service
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final RoleRepository roleRepository;

    public ResourceServiceImpl(ResourceRepository resourceRepository,
                               RoleRepository roleRepository) {
        this.resourceRepository = resourceRepository;
        this.roleRepository = roleRepository;
    }

    /**
     *  根据用户id获取资源列表
     * @param id
     * @return
     */
    public List<Resource> getResourceListByUserId(String id) {
        Long userId = Long.parseLong(id);
        List<Role> roles = roleRepository.listByUserId(userId);
        Set<Resource> resourceSet = new LinkedHashSet<>();
        for (Role role : roles) {
            List<Resource> resources = resourceRepository.listByRoleId(role.getId());
            resourceSet.addAll(resources);
        }
        return new ArrayList<>(resourceSet);
    }
}


