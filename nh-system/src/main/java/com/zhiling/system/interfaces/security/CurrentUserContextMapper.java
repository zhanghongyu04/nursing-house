package com.zhiling.system.interfaces.security;

import com.alibaba.fastjson2.JSONObject;
import com.zhiling.common.security.LoginVo;
import com.zhiling.framework.security.model.CurrentUserContext;
import io.jsonwebtoken.Claims;

/**

 * CurrentUserContextMapper

 *

 * @author zhanghongyu

 */

public final class CurrentUserContextMapper {

    /**
     * 构造器：CurrentUserContextMapper
     *
     * @author zhanghongyu
     */
    private CurrentUserContextMapper() {
    }

    /**
     * 方法：fromClaims
     *
     * @author zhanghongyu
     */
    public static CurrentUserContext fromClaims(Claims claims, String userToken) {
        if (claims == null || claims.get("currentUser") == null) {
            return null;
        }
        LoginVo loginVo = JSONObject.parseObject(String.valueOf(claims.get("currentUser")), LoginVo.class);
        CurrentUserContext context = CurrentUserContext.fromLoginVo(loginVo);
        if (context != null && (context.getToken() == null || context.getToken().isBlank())) {
            context.setToken(userToken);
        }
        return context;
    }
}