package com.zhiling.system.application.service;

import com.zhiling.model.entity.Role;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author zhanghongyu
 */
public interface RoleService {
    /**
     * 根据用户id获取角色列表
     * @param id
     * @return
     */
    List<Role> getRoleListByUserId(String id);
}


