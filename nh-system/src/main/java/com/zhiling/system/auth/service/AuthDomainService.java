package com.zhiling.system.auth.service;

import com.zhiling.model.dto.LoginDto;
import com.zhiling.common.security.LoginVo;

/**
 * 认证域服务接口。
 *
 * @author zhanghongyu
 */
public interface AuthDomainService {

    LoginVo login(LoginDto loginDto);

    Boolean logout();
}
