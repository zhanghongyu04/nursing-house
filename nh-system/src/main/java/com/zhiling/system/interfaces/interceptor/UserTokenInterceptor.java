package com.zhiling.system.interfaces.interceptor;

import com.zhiling.common.context.UserThreadLocal;
import com.zhiling.framework.security.CurrentUserContextHolder;
import com.zhiling.framework.security.model.CurrentUserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 拦截器：确保 UserThreadLocal 在 controller 层可用。
 * JWT 解析已在 JwtAuthenticationFilter 中完成，此处仅复用已设置的上下文。
 *
 * @author zhanghongyu
 */
public class UserTokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // 复用 JwtAuthenticationFilter 已解析的上下文，避免重复 JWT 解析
        CurrentUserContextHolder.get().ifPresent(ctx -> UserThreadLocal.set(ctx.toLoginVo()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserThreadLocal.remove();
    }
}