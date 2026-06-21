package com.zhiling.system.auth.service.support;

import com.zhiling.model.entity.Role;

import java.util.List;

/**
 * 认证域角色查询服务。
 *
 * @author zhanghongyu
 */
public interface AuthRoleQueryService {

    List<Role> getRoleListByUserId(String userId);
}