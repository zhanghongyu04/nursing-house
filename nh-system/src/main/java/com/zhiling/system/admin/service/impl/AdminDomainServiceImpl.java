package com.zhiling.system.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zhiling.common.constant.RoleConstant;
import com.zhiling.common.constant.UserConstant;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.common.properties.SecurityConfigProperties;
import com.zhiling.common.result.PageResult;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.security.port.SessionRevocationPort;
import com.zhiling.model.dto.UserDto;
import com.zhiling.model.dto.UserPageQueryDto;
import com.zhiling.model.entity.Role;
import com.zhiling.model.entity.User;
import com.zhiling.system.admin.service.AdminDomainService;
import com.zhiling.system.admin.service.AdminUserManageService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 管理员域服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class AdminDomainServiceImpl implements AdminDomainService {

    private static final String ADMIN_ADD_PATH = "/web/admin/add";
    private static final String ADMIN_DELETE_PATH = "/web/admin/delete";
    private static final String ADMIN_UPDATE_PATH = "/web/admin/update";
    private static final String ADMIN_RESET_PASSWORD_PATH = "/web/admin/resetPassword";
    private static final String ADMIN_PAGE_USER_PATH = "/web/admin/pageUser";
    private static final String ASSIGN_GOV_ADMIN_PATH = "/web/admin/assign-role/gov-admin";
    private static final String ASSIGN_PARENT_ADMIN_PATH = "/web/admin/assign-role/parent-admin";
    private static final String ASSIGN_ORG_ADMIN_PATH = "/web/admin/assign-role/org-admin";
    private static final String ASSIGN_NURSE_PATH = "/web/admin/assign-role/nurse";
    private static final Map<String, String> ASSIGN_ROLE_PERMISSION_PATHS = Map.of(
            RoleConstant.GOV_ADMIN, ASSIGN_GOV_ADMIN_PATH,
            RoleConstant.PARENT_ORG_ADMIN, ASSIGN_PARENT_ADMIN_PATH,
            RoleConstant.ORG_ADMIN, ASSIGN_ORG_ADMIN_PATH,
            RoleConstant.NURSE, ASSIGN_NURSE_PATH
    );

    private final AdminUserManageService adminUserManageService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecurityHelper securityHelper;
    private final SecurityConfigProperties securityConfigProperties;
    private final SessionRevocationPort sessionRevocationPort;

    public AdminDomainServiceImpl(AdminUserManageService adminUserManageService,
                                  BCryptPasswordEncoder passwordEncoder,
                                  SecurityHelper securityHelper,
                                  SecurityConfigProperties securityConfigProperties,
                                  SessionRevocationPort sessionRevocationPort) {
        this.adminUserManageService = adminUserManageService;
        this.passwordEncoder = passwordEncoder;
        this.securityHelper = securityHelper;
        this.securityConfigProperties = securityConfigProperties;
        this.sessionRevocationPort = sessionRevocationPort;
    }

    /**
     * 方法：addUser
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean addUser(UserDto userDto) {
        boolean govAdmin = ensureAdminOperationAllowedAndCheckGovRole(ADMIN_ADD_PATH);

        User user = BeanUtil.copyProperties(userDto, User.class);
        String rawPassword = StringUtils.hasText(userDto.getPassword()) ? userDto.getPassword() : defaultPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (!StringUtils.hasText(userDto.getAvatar())) {
            user.setAvatar("https://picsum.photos/seed/random123/800/600");
        }

        Long roleId = userDto.getRoleId();
        if (roleId == null) {
            roleId = adminUserManageService.selectRoleIdByLabel(RoleConstant.ORG_ADMIN);
        }
        if (roleId == null) {
            throw new ProjectException(400, "角色不存在");
        }
        Role targetRole = adminUserManageService.selectRoleById(roleId);
        if (targetRole == null || targetRole.getStatus() == null || targetRole.getStatus() != 0) {
            throw new ProjectException(400, "角色不存在或已停用");
        }
        validateTargetRoleAssignable(targetRole);

        Set<Long> targetSanaScopeIds = resolveTargetSanaScopeIds(userDto, null, govAdmin, targetRole);
        user.setSanaId(pickPrimarySanaId(targetSanaScopeIds));

        boolean created = adminUserManageService.addUser(user);
        Long userId = adminUserManageService.selectUserIdByUsername(user.getUsername());
        Boolean bindRole = adminUserManageService.addUserRole(roleId, userId);
        syncUserSanaScope(userId, targetSanaScopeIds, targetRole);
        return created && bindRole;
    }

    /**
     * 方法：deleteUser
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean deleteUser(String username) {
        boolean isGovAdmin = ensureAdminOperationAllowedAndCheckGovRole(ADMIN_DELETE_PATH);
        Long userId = adminUserManageService.selectUserIdByUsername(username);
        if (userId == null) {
            throw new ProjectException(404, "用户不存在");
        }

        validateTargetUserOperable(userId, isGovAdmin);
        sessionRevocationPort.revokeByUsername(username);
        Boolean removedUser = adminUserManageService.removeUserById(userId);
        Boolean removedRole = adminUserManageService.removeUserRoleByUserId(userId);
        adminUserManageService.deleteSanaScopesByUserId(userId);
        return removedUser && removedRole;
    }

    /**
     * 方法：pageUser
     *
     * @author zhanghongyu
     */
    @Override
    public PageResult pageUser(UserPageQueryDto userDto) {
        boolean isGovAdmin = ensureAdminOperationAllowedAndCheckGovRole(ADMIN_PAGE_USER_PATH);
        if (!isGovAdmin) {
            Set<Long> scopeIds = securityHelper.requireCurrentSanaScopeIds();
            userDto.setSanaScopeIds(scopeIds);
            // 若前端指定了 sanaId，校验是否在授权范围内；有效则保留以实现单机构精确筛选
            if (userDto.getSanaId() != null && !scopeIds.contains(userDto.getSanaId())) {
                throw new ProjectException(403, "无权查询其他机构用户");
            }
            // sanaId 为 null 时，SQL 按 IN(scopeIds) 过滤（保持原逻辑）
        }
        PageResult result = adminUserManageService.pageUser(userDto);
        List<?> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            // 批量查询所有用户的机构范围（避免 N+1）
            List<Long> userIds = records.stream()
                    .filter(row -> row instanceof User)
                    .map(row -> ((User) row).getId())
                    .toList();
            Map<Long, List<Long>> scopeMap = adminUserManageService.listSanaScopeIdsByUserIds(userIds);
            for (Object row : records) {
                if (!(row instanceof User user)) {
                    continue;
                }
                List<Long> scopeIds = scopeMap.get(user.getId());
                user.setSanaScopeIds(scopeIds != null ? new LinkedHashSet<>(scopeIds) : Set.of());
            }
        }
        return result;
    }

    /**
     * 方法：updateUser
     *
     * @author zhanghongyu
     */
    @Override
    @Transactional
    public Boolean updateUser(UserDto userDto) {
        boolean isGovAdmin = ensureAdminOperationAllowedAndCheckGovRole(ADMIN_UPDATE_PATH);
        if (userDto.getId() == null) {
            throw new ProjectException(400, "用户ID不能为空");
        }

        validateTargetUserOperable(userDto.getId(), isGovAdmin);

        User user = BeanUtil.copyProperties(userDto, User.class);
        User existing = adminUserManageService.selectUserById(userDto.getId());
        if (StringUtils.hasText(userDto.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        } else {
            user.setPassword(null);
        }
        if (!StringUtils.hasText(userDto.getAvatar())) {
            user.setAvatar(null);
        }

        Role targetRole = userDto.getRoleId() == null ? null : adminUserManageService.selectRoleById(userDto.getRoleId());
        if (targetRole == null) {
            List<Role> roleList = adminUserManageService.listRoleByUserId(userDto.getId());
            if (roleList != null && !roleList.isEmpty()) {
                targetRole = roleList.get(0);
            }
        }

        Set<Long> targetSanaScopeIds = resolveTargetSanaScopeIds(
                userDto,
                existing,
                isGovAdmin,
                targetRole
        );
        user.setSanaId(pickPrimarySanaId(targetSanaScopeIds));

        List<Role> existingRoles = adminUserManageService.listRoleByUserId(userDto.getId());
        Long existingRoleId = (existingRoles == null || existingRoles.isEmpty()) ? null : existingRoles.get(0).getId();
        List<Long> existingScopeList = adminUserManageService.listSanaScopeIdsByUserId(userDto.getId());
        Set<Long> existingScopeSet = existingScopeList == null
                ? Set.of()
                : new LinkedHashSet<>(existingScopeList.stream().filter(Objects::nonNull).toList());

        boolean updated = adminUserManageService.updateUser(user);
        if (updated) {
            boolean roleChanged = false;
            if (targetRole != null && !Objects.equals(existingRoleId, targetRole.getId())) {
                validateTargetRoleAssignable(targetRole);
                adminUserManageService.removeUserRoleByUserId(userDto.getId());
                adminUserManageService.addUserRole(targetRole.getId(), userDto.getId());
                roleChanged = true;
            }
            syncUserSanaScope(userDto.getId(), targetSanaScopeIds, targetRole);
            boolean scopeChanged = !Objects.equals(existingScopeSet, targetSanaScopeIds);
            if (roleChanged || scopeChanged) {
                sessionRevocationPort.revokeByUserId(userDto.getId());
            }
        }
        return updated;
    }

    /**
     * 方法：resetPassword
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean resetPassword(UserDto userDto) {
        boolean isGovAdmin = ensureAdminOperationAllowedAndCheckGovRole(ADMIN_RESET_PASSWORD_PATH);
        Long userId = adminUserManageService.selectUserIdByUsername(userDto.getUsername());
        if (userId == null) {
            throw new ProjectException(404, "用户不存在");
        }

        validateTargetUserOperable(userId, isGovAdmin);
        String encodePassword = passwordEncoder.encode(defaultPassword());
        boolean updated = adminUserManageService.updatePasswordByUserId(userId, encodePassword);
        if (updated) {
            sessionRevocationPort.revokeByUserId(userId);
        }
        return updated;
    }

    private String defaultPassword() {
        String configuredPassword = securityConfigProperties.getDefaulePassword();
        return StringUtils.hasText(configuredPassword) ? configuredPassword : UserConstant.DEFAULT_PASSWORD;
    }

    /**
     * 方法：ensureAdminOperationAllowedAndCheckGovRole
     *
     * @author zhanghongyu
     */
    private boolean ensureAdminOperationAllowedAndCheckGovRole(String requiredPath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(requiredPath)) {
            throw new ProjectException(403, "无权限执行该操作");
        }
        boolean isGovAdmin = securityHelper.hasGovAdminRoleForSensitiveOperation();
        return isGovAdmin;
    }

    private void validateTargetRoleAssignable(Role targetRole) {
        if (targetRole == null) {
            return;
        }
        String requiredPath = ASSIGN_ROLE_PERMISSION_PATHS.get(targetRole.getLabel());
        if (!StringUtils.hasText(requiredPath)) {
            throw new ProjectException(403, "角色未配置可分配权限");
        }
        if (!securityHelper.hasResourcePathForSensitiveOperation(requiredPath)) {
            throw new ProjectException(403, "无权创建或配置该角色账号");
        }
    }

    /**
     * 方法：validateTargetUserOperable
     *
     * @author zhanghongyu
     */
    private void validateTargetUserOperable(Long targetUserId, boolean isGovAdmin) {
        User targetUser = adminUserManageService.selectUserById(targetUserId);
        if (targetUser == null || targetUser.getStatus() == null || targetUser.getStatus() != 0) {
            throw new ProjectException(404, "目标用户不存在或已停用");
        }

        if (!isGovAdmin) {
            Set<Long> sanaScopeIds = securityHelper.requireCurrentSanaScopeIds();
            Set<Long> targetScopeIds = resolveExistingSanaScopeIds(targetUser);
            if (targetScopeIds.isEmpty() || !sanaScopeIds.containsAll(targetScopeIds)) {
                throw new ProjectException(403, "仅可操作授权范围内机构用户");
            }
        }

        List<Role> roleList = adminUserManageService.listRoleByUserId(targetUserId);
        if (roleList == null || roleList.isEmpty()) {
            return;
        }
        for (Role role : roleList) {
            if (RoleConstant.GOV_ADMIN.equals(role.getLabel())) {
                if (!isGovAdmin) {
                    throw new ProjectException(403, "不可操作政府管理员账号");
                }
                continue;
            }
        }
    }

    /**
     * 方法：syncUserSanaScope
     *
     * @author zhanghongyu
     */
    private void syncUserSanaScope(Long userId, Set<Long> sanaIds, Role targetRole) {
        if (userId == null) {
            return;
        }
        adminUserManageService.deleteSanaScopesByUserId(userId);
        if (sanaIds == null || sanaIds.isEmpty()) {
            return;
        }
        int scopeType = RoleConstant.NURSE.equals(targetRole == null ? null : targetRole.getLabel()) ? 2 : 1;
        adminUserManageService.insertSanaScopes(userId, new ArrayList<>(sanaIds), scopeType, "用户管理同步授权范围");
    }

    /**
     * 方法：resolveTargetSanaScopeIds
     *
     * @author zhanghongyu
     */
    private Set<Long> resolveTargetSanaScopeIds(UserDto userDto, User existingUser, boolean govAdmin, Role targetRole) {
        Set<Long> requested = new LinkedHashSet<>();
        if (userDto.getSanaScopeIds() != null) {
            requested.addAll(userDto.getSanaScopeIds().stream().filter(java.util.Objects::nonNull).toList());
        }
        if (requested.isEmpty() && userDto.getSanaId() != null) {
            requested.add(userDto.getSanaId());
        }
        if (requested.isEmpty() && existingUser != null && existingUser.getId() != null) {
            List<Long> existingScopes = adminUserManageService.listSanaScopeIdsByUserId(existingUser.getId());
            if (existingScopes != null) {
                requested.addAll(existingScopes.stream().filter(java.util.Objects::nonNull).toList());
            }
        }
        if (requested.isEmpty() && existingUser != null && existingUser.getSanaId() != null) {
            requested.add(existingUser.getSanaId());
        }

        if (!govAdmin) {
            Set<Long> operatorScopes = securityHelper.requireCurrentSanaScopeIdsForSensitiveOperation();
            if (requested.isEmpty()) {
                if (operatorScopes.size() == 1) {
                    requested.add(operatorScopes.iterator().next());
                } else {
                    throw new ProjectException(400, "当前账号已授权多个机构，请明确指定所属机构");
                }
            }
            if (!operatorScopes.containsAll(requested)) {
                throw new ProjectException(403, "仅可配置授权范围内机构");
            }
        }

        if (targetRole != null && (
                RoleConstant.PARENT_ORG_ADMIN.equals(targetRole.getLabel())
                        || RoleConstant.ORG_ADMIN.equals(targetRole.getLabel())
                        || RoleConstant.NURSE.equals(targetRole.getLabel())
        )) {
            if (requested.isEmpty()) {
                throw new ProjectException(400, "子机构管理员/母机构管理员/护理人员账号必须绑定至少一个机构");
            }
            for (Long sanaId : requested) {
                if (!adminUserManageService.existsSanatorium(sanaId)) {
                    throw new ProjectException(400, "所属机构不存在: " + sanaId);
                }
            }
        }
        return requested;
    }

    /**
     * 方法：pickPrimarySanaId
     *
     * @author zhanghongyu
     */
    private Long pickPrimarySanaId(Set<Long> sanaScopeIds) {
        if (sanaScopeIds == null || sanaScopeIds.isEmpty()) {
            return null;
        }
        return sanaScopeIds.iterator().next();
    }

    private Set<Long> resolveExistingSanaScopeIds(User user) {
        Set<Long> result = new LinkedHashSet<>();
        if (user != null && user.getId() != null) {
            List<Long> existingScopes = adminUserManageService.listSanaScopeIdsByUserId(user.getId());
            if (existingScopes != null) {
                result.addAll(existingScopes.stream().filter(Objects::nonNull).toList());
            }
        }
        if (result.isEmpty() && user != null && user.getSanaId() != null) {
            result.add(user.getSanaId());
        }
        return result;
    }
}
