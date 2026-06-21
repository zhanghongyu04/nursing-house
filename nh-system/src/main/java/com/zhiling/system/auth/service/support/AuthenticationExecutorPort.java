package com.zhiling.system.auth.service.support;

import org.springframework.security.core.Authentication;

/**

 * AuthenticationExecutorPort

 *

 * @author zhanghongyu

 */

public interface AuthenticationExecutorPort {

    Authentication authenticate(String username, String password) throws Exception;
}

