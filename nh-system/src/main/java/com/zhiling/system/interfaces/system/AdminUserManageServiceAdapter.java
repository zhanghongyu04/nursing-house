package com.zhiling.system.interfaces.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.UserPageQueryDto;
import com.zhiling.model.entity.Role;
import com.zhiling.model.entity.User;
import com.zhiling.system.admin.service.AdminUserManageService;
import com.zhiling.system.infrastructure.persistence.command.RoleCommandService;
import com.zhiling.system.infrastructure.persistence.command.UserCommandService;
import com.zhiling.system.infrastructure.persistence.query.RoleQueryService;
import com.zhiling.system.infrastructure.persistence.query.SanatoriumQueryService;
import com.zhiling.system.infrastructure.persistence.query.UserQueryService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 管理员域用户管理适配器。
 *
 * 适配领域服务接口，委托持久化查询/命令服务执行操作。
 *
 * @author zhanghongyu
 */
@Component
public class AdminUserManageServiceAdapter implements AdminUserManageService {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final RoleQueryService roleQueryService;
    private final RoleCommandService roleCommandService;
    private final SanatoriumQueryService sanatoriumQueryService;

    public AdminUserManageServiceAdapter(UserQueryService userQueryService,
                                         UserCommandService userCommandService,
                                         RoleQueryService roleQueryService,
                                         RoleCommandService roleCommandService,
                                         SanatoriumQueryService sanatoriumQueryService) {
        this.userQueryService = userQueryService;
        this.userCommandService = userCommandService;
        this.roleQueryService = roleQueryService;
        this.roleCommandService = roleCommandService;
        this.sanatoriumQueryService = sanatoriumQueryService;
    }

    /**
     * 方法：selectRoleIdByLabel
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectRoleIdByLabel(String roleLabel) {
        return roleQueryService.selectIdByLabel(roleLabel);
    }

    /**
     * 方法：selectRoleById
     *
     * @author zhanghongyu
     */
    @Override
    public Role selectRoleById(Long roleId) {
        return roleQueryService.selectById(roleId);
    }

    /**
     * 方法：listRoleByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Role> listRoleByUserId(Long userId) {
        return roleQueryService.listByUserId(userId);
    }

    /**
     * 方法：selectUserIdByUsername
     *
     * @author zhanghongyu
     */
    @Override
    public Long selectUserIdByUsername(String username) {
        return userQueryService.selectIdByUsername(username);
    }

    /**
     * 方法：selectUserById
     *
     * @author zhanghongyu
     */
    @Override
    public User selectUserById(Long userId) {
        return userQueryService.selectById(userId);
    }

    /**
     * 方法：addUser
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean addUser(User user) {
        return userCommandService.insert(user);
    }

    /**
     * 方法：updateUser
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean updateUser(User user) {
        return userCommandService.updateById(user);
    }

    /**
     * 方法：removeUserById
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean removeUserById(Long userId) {
        return userCommandService.deleteById(userId);
    }

    /**
     * 方法：addUserRole
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean addUserRole(Long roleId, Long userId) {
        return roleCommandService.insertRoleUser(roleId, userId);
    }

    /**
     * 方法：removeUserRoleByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean removeUserRoleByUserId(Long userId) {
        return roleCommandService.deleteRoleUser(userId);
    }

    /**
     * 方法：listSanaScopeIdsByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Long> listSanaScopeIdsByUserId(Long userId) {
        return userQueryService.listSanaScopeIdsByUserId(userId);
    }

    /**
     * 方法：listSanaScopeIdsByUserIds
     *
     * @author zhanghongyu
     */
    @Override
    public Map<Long, List<Long>> listSanaScopeIdsByUserIds(List<Long> userIds) {
        return userQueryService.listSanaScopeIdsByUserIds(userIds);
    }

    /**
     * 方法：deleteSanaScopesByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean deleteSanaScopesByUserId(Long userId) {
        return userCommandService.deleteSanaScopesByUserId(userId);
    }

    /**
     * 方法：insertSanaScopes
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean insertSanaScopes(Long userId, List<Long> sanaIds, Integer scopeType, String remark) {
        return userCommandService.insertSanaScopes(userId, sanaIds, scopeType, remark);
    }

    /**
     * 方法：pageUser
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult pageUser(UserPageQueryDto userPageQueryDto) {
        Page<User> page = new Page<>(userPageQueryDto.getPage(), userPageQueryDto.getPageSize());
        IPage<User> result = userCommandService.selectPage(page, userPageQueryDto);
        return new PageResult(result.getTotal(), result.getRecords());
    }

    /**
     * 方法：updatePasswordByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean updatePasswordByUserId(Long userId, String encodedPassword) {
        return userCommandService.updatePasswordByUserId(userId, encodedPassword);
    }

    /**
     * 方法：existsSanatorium
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean existsSanatorium(Long sanaId) {
        return sanatoriumQueryService.selectById(sanaId) != null;
    }
}
