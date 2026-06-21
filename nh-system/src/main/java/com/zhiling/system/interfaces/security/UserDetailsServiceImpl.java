package com.zhiling.system.interfaces.security;

import com.zhiling.common.security.UserAuth;
import com.zhiling.common.security.LoginVo;
import com.zhiling.system.application.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Security 自定义 UserDetailsService 实现
 *
 * @author zhanghongyu
 */
@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    /**
     * 构造器：UserDetailsServiceImpl
     *
     * @author zhanghongyu
     */
    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        LoginVo userVo = userService.findUserVoForLogin(username);
        return new UserAuth(userVo);
    }
}

