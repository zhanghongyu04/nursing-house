package com.zhiling.system.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiling.common.constant.RoleConstant;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.entity.Resource;
import com.zhiling.model.entity.Role;
import com.zhiling.model.entity.RoleResource;
import com.zhiling.model.vo.PermissionResourceTreeVo;
import com.zhiling.system.application.service.PermissionManageService;
import com.zhiling.system.infrastructure.persistence.mapper.ResourceMapper;
import com.zhiling.system.infrastructure.persistence.mapper.RoleMapper;
import com.zhiling.system.infrastructure.persistence.mapper.RoleResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限管理应用服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class PermissionManageServiceImpl implements PermissionManageService {

    private static final long LEGACY_ADMIN_ROLE_ID = 1L;
    private static final String ROOT_PARENT_NO = "0";
    private static final String USER_MANAGE_MENU_RESOURCE_NO = "1101";
    private static final String USER_MANAGE_PAGE_QUERY_RESOURCE_NO = "1117";
    private static final String PERMISSION_ROLE_LIST_PATH = "/web/permission/roles";
    private static final String PERMISSION_RESOURCE_TREE_PATH = "/web/permission/resources/tree";
    private static final String PERMISSION_ROLE_RESOURCE_PATH = "/web/permission/roles/*/resources";
    private static final Set<String> GOV_ADMIN_REQUIRED_RESOURCE_NOS = Set.of("1109", "1110", "1111", "1112");

    private final RoleMapper roleMapper;
    private final ResourceMapper resourceMapper;
    private final RoleResourceMapper roleResourceMapper;
    private final SecurityHelper securityHelper;

    public PermissionManageServiceImpl(RoleMapper roleMapper,
                                       ResourceMapper resourceMapper,
                                       RoleResourceMapper roleResourceMapper,
                                       SecurityHelper securityHelper) {
        this.roleMapper = roleMapper;
        this.resourceMapper = resourceMapper;
        this.roleResourceMapper = roleResourceMapper;
        this.securityHelper = securityHelper;
    }

    @Override
    public List<Role> listRoles() {
        assertResourcePermission(PERMISSION_ROLE_LIST_PATH);
        return roleMapper.selectList(new LambdaQueryWrapper<Role>()
                .eq(Role::getStatus, 0)
                .ne(Role::getId, LEGACY_ADMIN_ROLE_ID)
                .orderByAsc(Role::getSortNo)
                .orderByAsc(Role::getId));
    }

    @Override
    public List<PermissionResourceTreeVo> listResourceTree() {
        assertResourcePermission(PERMISSION_RESOURCE_TREE_PATH);
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getStatus, 0)
                .orderByAsc(Resource::getSortNo)
                .orderByAsc(Resource::getId));

        Map<String, PermissionResourceTreeVo> nodeMap = new LinkedHashMap<>();
        for (Resource resource : resources) {
            if (!StringUtils.hasText(resource.getResourceNo())) {
                continue;
            }
            nodeMap.put(resource.getResourceNo(), toNode(resource));
        }

        List<PermissionResourceTreeVo> roots = new ArrayList<>();
        for (PermissionResourceTreeVo node : nodeMap.values()) {
            PermissionResourceTreeVo parent = nodeMap.get(node.getParentResourceNo());
            if (parent == null || ROOT_PARENT_NO.equals(node.getParentResourceNo())) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    @Override
    public List<String> listRoleResourceNos(Long roleId) {
        assertResourcePermission(PERMISSION_ROLE_RESOURCE_PATH);
        validateRole(roleId);
        List<RoleResource> rows = roleResourceMapper.selectList(new LambdaQueryWrapper<RoleResource>()
                .eq(RoleResource::getRoleId, roleId)
                .eq(RoleResource::getStatus, 0)
                .orderByAsc(RoleResource::getResourceNo));
        return rows.stream()
                .map(RoleResource::getResourceNo)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    @Override
    @Transactional
    public Boolean saveRoleResources(Long roleId, List<String> resourceNos) {
        assertResourcePermission(PERMISSION_ROLE_RESOURCE_PATH);
        Role role = validateRole(roleId);

        Set<String> validResourceNos = loadValidResourceNos();
        Set<String> normalizedNos = new LinkedHashSet<>();
        if (resourceNos != null) {
            for (String resourceNo : resourceNos) {
                if (!StringUtils.hasText(resourceNo)) {
                    continue;
                }
                String trimmedNo = resourceNo.trim();
                if (!validResourceNos.contains(trimmedNo)) {
                    throw new ProjectException(400, "资源不存在或已停用：" + trimmedNo);
                }
                normalizedNos.add(trimmedNo);
            }
        }
        if (RoleConstant.GOV_ADMIN.equals(role.getLabel())) {
            normalizedNos.addAll(GOV_ADMIN_REQUIRED_RESOURCE_NOS);
        }
        if (normalizedNos.contains(USER_MANAGE_MENU_RESOURCE_NO)
                && validResourceNos.contains(USER_MANAGE_PAGE_QUERY_RESOURCE_NO)) {
            normalizedNos.add(USER_MANAGE_PAGE_QUERY_RESOURCE_NO);
        }

        roleResourceMapper.delete(new LambdaQueryWrapper<RoleResource>().eq(RoleResource::getRoleId, roleId));
        for (String resourceNo : normalizedNos) {
            RoleResource roleResource = RoleResource.builder()
                    .roleId(roleId)
                    .resourceNo(resourceNo)
                    .build();
            roleResource.setStatus(0);
            roleResource.setRemark("权限管理页面授权");
            roleResourceMapper.insert(roleResource);
        }
        return true;
    }

    private void assertResourcePermission(String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            throw new ProjectException(403, "无权限维护权限配置");
        }
    }

    private Role validateRole(Long roleId) {
        if (roleId == null) {
            throw new ProjectException(400, "角色ID不能为空");
        }
        if (LEGACY_ADMIN_ROLE_ID == roleId) {
            throw new ProjectException(400, "历史管理员角色已弃用");
        }
        Role role = roleMapper.selectById(roleId);
        if (role == null || role.getStatus() == null || role.getStatus() != 0) {
            throw new ProjectException(404, "角色不存在或已停用");
        }
        return role;
    }

    private Set<String> loadValidResourceNos() {
        List<Resource> resources = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getStatus, 0)
                .select(Resource::getResourceNo));
        Set<String> result = new LinkedHashSet<>();
        for (Resource resource : resources) {
            if (StringUtils.hasText(resource.getResourceNo())) {
                result.add(resource.getResourceNo());
            }
        }
        return result;
    }

    private PermissionResourceTreeVo toNode(Resource resource) {
        PermissionResourceTreeVo node = new PermissionResourceTreeVo();
        node.setId(resource.getId());
        node.setResourceNo(resource.getResourceNo());
        node.setParentResourceNo(resource.getParentResourceNo());
        node.setResourceName(resource.getResourceName());
        node.setResourceType(resource.getResourceType());
        node.setRequestPath(resource.getRequestPath());
        node.setLabel(resource.getLabel());
        node.setSortNo(resource.getSortNo());
        node.setIcon(resource.getIcon());
        return node;
    }

    private void sortTree(List<PermissionResourceTreeVo> nodes) {
        nodes.sort(Comparator
                .comparing((PermissionResourceTreeVo node) -> node.getSortNo() == null ? Integer.MAX_VALUE : node.getSortNo())
                .thenComparing(PermissionResourceTreeVo::getResourceNo, Comparator.nullsLast(String::compareTo)));
        for (PermissionResourceTreeVo node : nodes) {
            sortTree(node.getChildren());
        }
    }
}
