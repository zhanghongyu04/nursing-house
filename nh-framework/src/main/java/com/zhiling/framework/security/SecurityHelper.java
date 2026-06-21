package com.zhiling.framework.security;

import com.zhiling.common.constant.RoleConstant;
import com.zhiling.framework.security.model.AccessScope;
import com.zhiling.framework.security.model.CurrentUser;
import com.zhiling.framework.security.port.LiveAccessScopePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.util.Optional;
import java.util.Set;

/**
 * 安全上下文辅助工具类。
 *
 * 提供基于 {@link CurrentUserProvider} 的便利方法，用于替代直接使用 UserUtil。
 *
 * @author zhanghongyu
 */
@Slf4j
public final class SecurityHelper {

    private static final AntPathMatcher RESOURCE_PATH_MATCHER = new AntPathMatcher();

    private final CurrentUserProvider provider;
    private final LiveAccessScopePort liveAccessScopePort;

    /**
     * 构造器：SecurityHelper
     *
     * @author zhanghongyu
     */
    public SecurityHelper(CurrentUserProvider provider) {
        this(provider, null);
    }

    public SecurityHelper(CurrentUserProvider provider, LiveAccessScopePort liveAccessScopePort) {
        this.provider = provider;
        this.liveAccessScopePort = liveAccessScopePort;
    }

    /**
     * 检查当前用户是否拥有指定角色。
     *
     * @param roleLabel 角色标签
     * @return 如果拥有该角色返回 true，否则返回 false；无上下文时返回 false
     */
    public boolean hasRole(String roleLabel) {
        return provider.currentAccessScope()
                .map(AccessScope::getRoleLabels)
                .map(roles -> roles != null && roles.contains(roleLabel))
                .orElse(false);
    }

    /**
     * 检查当前用户是否为政府管理员。
     *
     * @return 如果是政府管理员返回 true，否则返回 false
     */
    public boolean hasGovAdminRole() {
        return roleLabelsSnapshot().contains(RoleConstant.GOV_ADMIN);
    }

    /**
     * 敏感操作：优先使用数据库实时角色，JWT 仅作兜底。
     */
    public boolean hasGovAdminRoleForSensitiveOperation() {
        return resolveLiveRoleLabels().contains(RoleConstant.GOV_ADMIN);
    }

    /**
     * 检查当前用户是否拥有任意一个指定角色。
     *
     * @param roleLabels 角色标签集合
     * @return 如果拥有任意一个角色返回 true，否则返回 false；无上下文时返回 false
     */
    public boolean hasAnyRole(String... roleLabels) {
        if (roleLabels == null || roleLabels.length == 0) {
            return false;
        }
        Set<String> currentRoles = roleLabelsSnapshot();
        if (currentRoles.isEmpty()) {
            return false;
        }
        for (String roleLabel : roleLabels) {
            if (currentRoles.contains(roleLabel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 敏感操作：优先使用数据库实时角色。
     */
    public boolean hasAnyRoleForSensitiveOperation(String... roleLabels) {
        if (roleLabels == null || roleLabels.length == 0) {
            return false;
        }
        Set<String> currentRoles = resolveLiveRoleLabels();
        for (String roleLabel : roleLabels) {
            if (currentRoles.contains(roleLabel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否拥有指定资源路径。
     */
    public boolean hasResourcePath(String resourcePath) {
        return matchesResourcePath(resourcePathsSnapshot(), resourcePath);
    }

    /**
     * 敏感操作：优先使用数据库实时资源权限，JWT 仅作兜底。
     */
    public boolean hasResourcePathForSensitiveOperation(String resourcePath) {
        return matchesResourcePath(resolveLiveResourcePaths(), resourcePath);
    }

    /**
     * 获取当前用户 ID。
     *
     * @return 当前用户 ID；无上下文时返回 null
     */
    public Long getCurrentUserId() {
        return provider.currentAccessScope()
                .map(AccessScope::getUserId)
                .orElse(null);
    }

    /**
     * 获取当前用户 ID，无上下文时抛出异常。
     *
     * @return 当前用户 ID
     * @throws IllegalStateException 无可用用户上下文时抛出
     */
    public Long requireCurrentUserId() {
        return provider.currentAccessScope()
                .map(AccessScope::getUserId)
                .orElseThrow(() -> new IllegalStateException("当前无可用用户上下文"));
    }

    /**
     * 获取当前机构 ID。
     *
     * @return 当前机构 ID；无上下文时返回 null
     */
    public Long getCurrentSanaId() {
        return provider.currentAccessScope()
                .map(AccessScope::getSanaId)
                .orElse(null);
    }

    /**
     * 获取当前机构范围 IDs。
     *
     * @return 当前机构范围 IDs；无上下文时返回空集合
     */
    public Set<Long> getCurrentSanaScopeIds() {
        return provider.currentAccessScope()
                .map(AccessScope::getSanaScopeIds)
                .orElse(Set.of());
    }

    /**
     * 敏感操作：优先使用数据库实时机构范围。
     */
    public Set<Long> getCurrentSanaScopeIdsForSensitiveOperation() {
        return resolveLiveSanaScopeIds();
    }

    /**
     * 敏感操作：优先使用数据库实时机构范围，无可用范围时抛出异常。
     */
    public Set<Long> requireCurrentSanaScopeIdsForSensitiveOperation() {
        Set<Long> scopeIds = getCurrentSanaScopeIdsForSensitiveOperation();
        if (scopeIds == null || scopeIds.isEmpty()) {
            throw new IllegalStateException("当前用户未绑定机构");
        }
        return scopeIds;
    }

    /**
     * 获取当前资源路径权限。
     *
     * @return 当前资源路径权限；无上下文时返回空集合
     */
    public Set<String> getCurrentResourcePaths() {
        return provider.currentAccessScope()
                .map(AccessScope::getResourcePaths)
                .orElse(Set.of());
    }

    /**
     * 敏感操作：优先使用数据库实时资源路径。
     */
    public Set<String> getCurrentResourcePathsForSensitiveOperation() {
        return resolveLiveResourcePaths();
    }

    /**
     * 获取当前机构范围 IDs，无上下文或为空时抛出异常。
     *
     * @return 当前机构范围 IDs
     * @throws IllegalStateException 无可用机构范围或为空时抛出
     */
    public Set<Long> requireCurrentSanaScopeIds() {
        Set<Long> scopeIds = getCurrentSanaScopeIds();
        if (scopeIds == null || scopeIds.isEmpty()) {
            throw new IllegalStateException("当前用户未绑定机构");
        }
        return scopeIds;
    }

    /**
     * 检查目标机构 ID 是否在当前用户机构范围内。
     *
     * @param targetSanaId 目标机构 ID
     * @return 如果在范围内返回 true，否则返回 false
     */
    public boolean isWithinSanaScope(Long targetSanaId) {
        if (targetSanaId == null) {
            return false;
        }
        Set<Long> scopeIds = getCurrentSanaScopeIds();
        return scopeIds.contains(targetSanaId);
    }

    /**
     * 检查是否可以操作目标机构（政府管理员或机构在范围内）。
     *
     * @param targetSanaId 目标机构 ID
     * @return 如果可以操作返回 true，否则返回 false
     */
    public boolean canOperateSana(Long targetSanaId) {
        if (hasGovAdminRole()) {
            return true;
        }
        return isWithinSanaScope(targetSanaId);
    }

    public boolean canOperateSanaForSensitiveOperation(Long targetSanaId) {
        if (hasGovAdminRoleForSensitiveOperation()) {
            return true;
        }
        if (targetSanaId == null) {
            return false;
        }
        return getCurrentSanaScopeIdsForSensitiveOperation().contains(targetSanaId);
    }

    private Set<String> roleLabelsSnapshot() {
        return provider.currentAccessScope()
                .map(AccessScope::getRoleLabels)
                .orElse(Set.of());
    }

    private Set<String> resourcePathsSnapshot() {
        return provider.currentAccessScope()
                .map(AccessScope::getResourcePaths)
                .orElse(Set.of());
    }

    private Set<String> resolveLiveRoleLabels() {
        AccessScope liveScope = loadLiveAccessScope().orElse(null);
        if (liveScope != null) {
            return liveScope.getRoleLabels() == null ? Set.of() : liveScope.getRoleLabels();
        }
        return roleLabelsSnapshot();
    }

    private Set<Long> resolveLiveSanaScopeIds() {
        AccessScope liveScope = loadLiveAccessScope().orElse(null);
        if (liveScope != null) {
            return liveScope.getSanaScopeIds() == null ? Set.of() : liveScope.getSanaScopeIds();
        }
        Set<Long> snapshotScope = getCurrentSanaScopeIds();
        if (!snapshotScope.isEmpty()) {
            return snapshotScope;
        }
        Long sanaId = liveScope == null ? getCurrentSanaId() : liveScope.getSanaId();
        if (sanaId != null) {
            return Set.of(sanaId);
        }
        return Set.of();
    }

    private Set<String> resolveLiveResourcePaths() {
        AccessScope liveScope = loadLiveAccessScope().orElse(null);
        if (liveScope != null) {
            return liveScope.getResourcePaths() == null ? Set.of() : liveScope.getResourcePaths();
        }
        return resourcePathsSnapshot();
    }

    private boolean matchesResourcePath(Set<String> resourcePaths, String targetPath) {
        if (resourcePaths == null || resourcePaths.isEmpty() || targetPath == null || targetPath.isBlank()) {
            return false;
        }
        String normalizedTarget = normalizeResourcePath(targetPath);
        return resourcePaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(this::normalizeResourcePath)
                .anyMatch(pattern -> RESOURCE_PATH_MATCHER.match(pattern, normalizedTarget));
    }

    private String normalizeResourcePath(String path) {
        String normalized = path.replaceFirst("^/api/v1", "/web");
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private Optional<AccessScope> loadLiveAccessScope() {
        if (liveAccessScopePort == null) {
            return Optional.empty();
        }
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Optional.empty();
        }
        try {
            return liveAccessScopePort.loadAccessScope(userId);
        } catch (Exception ex) {
            log.warn("加载用户 {} 实时访问范围失败，回退到 JWT 快照", userId, ex);
            return Optional.empty();
        }
    }

    /**
     * 获取当前完整用户信息。
     */
    public Optional<CurrentUser> getCurrentUser() {
        return provider.currentUser();
    }

    /**
     * 获取当前访问范围。
     */
    public Optional<AccessScope> getCurrentAccessScope() {
        return provider.currentAccessScope();
    }
}
