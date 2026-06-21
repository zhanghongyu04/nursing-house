package com.zhiling.system.interfaces.security;

import com.zhiling.framework.security.model.AccessScope;
import com.zhiling.framework.security.port.LiveAccessScopePort;
import com.zhiling.model.entity.Role;
import com.zhiling.model.entity.User;
import com.zhiling.model.entity.Resource;
import com.zhiling.system.auth.service.support.AuthResourceQueryService;
import com.zhiling.system.auth.service.support.AuthRoleQueryService;
import com.zhiling.system.auth.service.support.AuthUserScopeService;
import com.zhiling.system.infrastructure.persistence.query.UserQueryService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于数据库的实时访问范围解析。
 */
@Component
public class LiveAccessScopePortAdapter implements LiveAccessScopePort {

    private final AuthRoleQueryService authRoleQueryService;
    private final AuthResourceQueryService authResourceQueryService;
    private final AuthUserScopeService authUserScopeService;
    private final UserQueryService userQueryService;

    public LiveAccessScopePortAdapter(AuthRoleQueryService authRoleQueryService,
                                      AuthResourceQueryService authResourceQueryService,
                                      AuthUserScopeService authUserScopeService,
                                      UserQueryService userQueryService) {
        this.authRoleQueryService = authRoleQueryService;
        this.authResourceQueryService = authResourceQueryService;
        this.authUserScopeService = authUserScopeService;
        this.userQueryService = userQueryService;
    }

    @Override
    public Optional<AccessScope> loadAccessScope(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        List<Role> roles = authRoleQueryService.getRoleListByUserId(String.valueOf(userId));
        Set<String> roleLabels = roles == null ? Set.of() : roles.stream()
                .map(Role::getLabel)
                .filter(label -> label != null && !label.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> resourcePaths = authResourceQueryService.getResourceListByUserId(String.valueOf(userId)).stream()
                .filter(resource -> "r".equals(resource.getResourceType()))
                .map(Resource::getRequestPath)
                .filter(path -> path != null && !path.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> sanaScopeIds = new LinkedHashSet<>(authUserScopeService.listSanaScopeIdsByUserId(userId));
        User user = userQueryService.selectById(userId);
        Long sanaId = user == null ? null : user.getSanaId();
        if (sanaScopeIds.isEmpty() && sanaId != null) {
            sanaScopeIds.add(sanaId);
        }

        return Optional.of(AccessScope.builder()
                .userId(userId)
                .sanaId(sanaId)
                .sanaScopeIds(sanaScopeIds)
                .roleLabels(roleLabels)
                .resourcePaths(resourcePaths)
                .build());
    }
}
