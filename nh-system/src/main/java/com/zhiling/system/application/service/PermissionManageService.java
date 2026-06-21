package com.zhiling.system.application.service;

import com.zhiling.model.entity.Role;
import com.zhiling.model.vo.PermissionResourceTreeVo;

import java.util.List;

/**
 * 权限管理应用服务。
 *
 * @author zhanghongyu
 */
public interface PermissionManageService {
    List<Role> listRoles();

    List<PermissionResourceTreeVo> listResourceTree();

    List<String> listRoleResourceNos(Long roleId);

    Boolean saveRoleResources(Long roleId, List<String> resourceNos);
}
