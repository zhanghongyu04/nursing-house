package com.zhiling.system.auth.service.support;

import java.util.List;

/**
 * 认证域用户机构范围查询服务。
 *
 * @author zhanghongyu
 */
public interface AuthUserScopeService {

    List<Long> listSanaScopeIdsByUserId(Long userId);
}