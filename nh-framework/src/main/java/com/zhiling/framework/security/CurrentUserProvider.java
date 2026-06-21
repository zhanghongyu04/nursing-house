package com.zhiling.framework.security;

import com.zhiling.framework.security.model.AccessScope;
import com.zhiling.framework.security.model.CurrentUser;

import java.util.Optional;

/**
 * 当前登录用户上下文提供器。
 *
 * 终态用于替代对 UserUtil/UserThreadLocal 的直接依赖。
 *
 * @author zhanghongyu
 */
public interface CurrentUserProvider {

    /**
     * 返回当前登录用户；无上下文时返回 empty。
     */
    Optional<CurrentUser> currentUser();

    /**
     * 返回当前访问范围；无上下文时返回 empty。
     */
    Optional<AccessScope> currentAccessScope();

    /**
     * 返回当前登录用户；无上下文时抛出异常。
     */
    default CurrentUser requireCurrentUser() {
        return currentUser().orElseThrow(() -> new IllegalStateException("当前无可用登录用户上下文"));
    }

    /**
     * 返回当前访问范围；无上下文时抛出异常。
     */
    default AccessScope requireCurrentAccessScope() {
        return currentAccessScope().orElseThrow(() -> new IllegalStateException("当前无可用访问范围上下文"));
    }
}