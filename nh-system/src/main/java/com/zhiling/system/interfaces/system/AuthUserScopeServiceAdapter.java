package com.zhiling.system.interfaces.system;

import com.zhiling.system.auth.service.support.AuthUserScopeService;
import com.zhiling.system.infrastructure.persistence.query.UserQueryService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 认证域用户机构范围查询适配器。
 *
 * 适配领域服务接口，委托持久化查询服务执行查询。
 *
 * @author zhanghongyu
 */
@Component
public class AuthUserScopeServiceAdapter implements AuthUserScopeService {

    private final UserQueryService userQueryService;

    /**
     * 构造器：AuthUserScopeServiceAdapter
     *
     * @author zhanghongyu
     */
    public AuthUserScopeServiceAdapter(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    /**
     * 方法：listSanaScopeIdsByUserId
     *
     * @author zhanghongyu
     */
    @Override
    public List<Long> listSanaScopeIdsByUserId(Long userId) {
        return userQueryService.listSanaScopeIdsByUserId(userId);
    }
}