package com.zhiling.system.auth.service.support.impl;

import com.zhiling.system.auth.service.support.AuthenticationExecutorPort;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
/**
 * SprintAuthenticationExecutor
 *
 * @author zhanghongyu
 */
public class SprintAuthenticationExecutor implements AuthenticationExecutorPort {

    private final AuthenticationConfiguration authenticationConfiguration;

    /**
     * 构造器：SprintAuthenticationExecutor
     *
     * @author zhanghongyu
     */
    public SprintAuthenticationExecutor(AuthenticationConfiguration authenticationConfiguration) {
        this.authenticationConfiguration = authenticationConfiguration;
    }

    @Override
    public Authentication authenticate(String username, String password) throws Exception {
        return authenticationConfiguration.getAuthenticationManager()
                .authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }
}