package com.zhiling.common.context;

import com.zhiling.common.security.LoginVo;

/**
 * 请求级用户上下文持有器。
 *
 * <h3>边界约束（架构门禁）</h3>
 * <ul>
 *   <li>写入方：仅 {@code UserTokenInterceptor}（拦截器层）</li>
 *   <li>读取方：仅 {@code CurrentUserProviderAdapter}（适配器层）</li>
 *   <li>业务层：禁止直接调用本类，必须通过 {@code SecurityHelper} 获取当前用户</li>
 *   <li>测试代码：使用 {@link #set(LoginVo)} 直接写入 {@code LoginVo} 对象</li>
 * </ul>
 *
 * @author zhanghongyu
 */
public class UserThreadLocal {

    private static final ThreadLocal<LoginVo> CONTEXT = new ThreadLocal<>();

    /**
     * 构造器：UserThreadLocal
     *
     * @author zhanghongyu
     */
    private UserThreadLocal() {
    }

    /**
     * 方法：set
     *
     * @author zhanghongyu
     */
    public static void set(LoginVo loginVo) {
        CONTEXT.set(loginVo);
    }

    /**
     * 方法：get
     *
     * @author zhanghongyu
     */
    public static LoginVo get() {
        return CONTEXT.get();
    }

    /**
     * 方法：remove
     *
     * @author zhanghongyu
     */
    public static void remove() {
        CONTEXT.remove();
    }
}