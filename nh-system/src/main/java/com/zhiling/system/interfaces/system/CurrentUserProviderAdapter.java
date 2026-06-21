package com.zhiling.system.interfaces.system;

import com.zhiling.framework.security.CurrentUserProvider;
import com.zhiling.framework.security.CurrentUserContextHolder;
import com.zhiling.framework.security.model.AccessScope;
import com.zhiling.framework.security.model.CurrentUser;
import com.zhiling.framework.security.model.CurrentUserContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 当前登录用户上下文提供器实现。
 *
 * 优先从请求级统一上下文读取，兼容回退到 ThreadLocal 镜像。
 *
 * @author zhanghongyu
 */
@Component
public class CurrentUserProviderAdapter implements CurrentUserProvider {

    /**
     * 方法：currentUser
     *
     * @author zhanghongyu
     */
    @Override
    public Optional<CurrentUser> currentUser() {
        return readCurrentUserContext().map(CurrentUserContext::toCurrentUser);
    }

    /**
     * 方法：currentAccessScope
     *
     * @author zhanghongyu
     */
    @Override
    public Optional<AccessScope> currentAccessScope() {
        return readCurrentUserContext().map(CurrentUserContext::toAccessScope);
    }

    /**
     * 方法：readCurrentUserContext
     *
     * @author zhanghongyu
     */
    private Optional<CurrentUserContext> readCurrentUserContext() {
        return CurrentUserContextHolder.get()
                .or(() -> Optional.ofNullable(com.zhiling.common.context.UserThreadLocal.get())
                        .map(CurrentUserContext::fromLoginVo));
    }
}