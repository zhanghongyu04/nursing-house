package com.zhiling.system.interfaces.security;

import com.zhiling.framework.security.port.SessionRevocationPort;
import com.zhiling.model.entity.User;
import com.zhiling.system.auth.service.support.TokenStorePort;
import com.zhiling.system.infrastructure.persistence.query.UserQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 通过删除 Redis Token 映射吊销用户会话。
 */
@Component
@Slf4j
public class SessionRevocationPortAdapter implements SessionRevocationPort {

    private final TokenStorePort tokenStorePort;
    private final UserQueryService userQueryService;

    public SessionRevocationPortAdapter(TokenStorePort tokenStorePort, UserQueryService userQueryService) {
        this.tokenStorePort = tokenStorePort;
        this.userQueryService = userQueryService;
    }

    @Override
    public boolean revokeByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        String userToken = tokenStorePort.getUserToken(username);
        if (!StringUtils.hasText(userToken)) {
            return false;
        }
        tokenStorePort.deleteTokens(username, userToken);
        log.info("[Security] 已吊销用户会话 username={}", username);
        return true;
    }

    @Override
    public boolean revokeByUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userQueryService.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return false;
        }
        return revokeByUsername(user.getUsername());
    }
}
