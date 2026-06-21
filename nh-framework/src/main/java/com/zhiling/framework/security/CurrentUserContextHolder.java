package com.zhiling.framework.security;

import com.zhiling.framework.security.model.CurrentUserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * 当前用户上下文的请求级持有器。
 *
 * @author zhanghongyu
 */
public final class CurrentUserContextHolder {

    public static final String REQUEST_ATTRIBUTE = CurrentUserContextHolder.class.getName() + ".CURRENT_USER_CONTEXT";

    /**
     * 构造器：CurrentUserContextHolder
     *
     * @author zhanghongyu
     */
    private CurrentUserContextHolder() {
    }

    /**
     * 方法：set
     *
     * @author zhanghongyu
     */
    public static void set(HttpServletRequest request, CurrentUserContext context) {
        if (request != null && context != null) {
            request.setAttribute(REQUEST_ATTRIBUTE, context);
        }
    }

    /**
     * 方法：get
     *
     * @author zhanghongyu
     */
    public static Optional<CurrentUserContext> get() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return Optional.empty();
        }
        Object value = servletRequestAttributes.getRequest().getAttribute(REQUEST_ATTRIBUTE);
        if (value instanceof CurrentUserContext currentUserContext) {
            return Optional.of(currentUserContext);
        }
        return Optional.empty();
    }
}