package com.zhiling.system.infrastructure.persistence.command;

/**
 * 角色命令服务接口。
 *
 * 封装 RoleMapper 的写操作（insert/update/delete）。
 *
 * @author zhanghongyu
 */
public interface RoleCommandService {

    /**
     * 添加角色用户关联
     */
    boolean insertRoleUser(Long roleId, Long userId);

    /**
     * 删除角色用户关联
     */
    boolean deleteRoleUser(Long userId);
}