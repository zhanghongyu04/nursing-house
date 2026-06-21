package com.zhiling.system.admin.service;

import com.zhiling.common.result.PageResult;
import com.zhiling.model.dto.UserPageQueryDto;
import com.zhiling.model.entity.Role;
import com.zhiling.model.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 管理员域用户管理支撑服务。
 *
 * @author zhanghongyu
 */
public interface AdminUserManageService {

    Long selectRoleIdByLabel(String roleLabel);

    Role selectRoleById(Long roleId);

    List<Role> listRoleByUserId(Long userId);

    Long selectUserIdByUsername(String username);

    User selectUserById(Long userId);

    Boolean addUser(User user);

    Boolean updateUser(User user);

    Boolean removeUserById(Long userId);

    Boolean addUserRole(Long roleId, Long userId);

    Boolean removeUserRoleByUserId(Long userId);

    List<Long> listSanaScopeIdsByUserId(Long userId);

    Map<Long, List<Long>> listSanaScopeIdsByUserIds(List<Long> userIds);

    Boolean deleteSanaScopesByUserId(Long userId);

    Boolean insertSanaScopes(Long userId, List<Long> sanaIds, Integer scopeType, String remark);

    PageResult pageUser(UserPageQueryDto userPageQueryDto);

    Boolean updatePasswordByUserId(Long userId, String encodedPassword);

    Boolean existsSanatorium(Long sanaId);
}
