package com.zhiling.system.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.common.properties.JwtProperties;
import com.zhiling.common.security.LoginVo;
import com.zhiling.common.security.UserAuth;
import com.zhiling.common.utils.JwtUtil;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.framework.security.model.CurrentUser;
import com.zhiling.model.dto.LoginDto;
import com.zhiling.model.entity.Resource;
import com.zhiling.model.entity.Role;
import com.zhiling.system.auth.service.AuthDomainService;
import com.zhiling.system.auth.service.support.AuthenticationExecutorPort;
import com.zhiling.system.auth.service.support.CaptchaService;
import com.zhiling.system.auth.service.support.AuthResourceQueryService;
import com.zhiling.system.auth.service.support.AuthRoleQueryService;
import com.zhiling.system.auth.service.support.AuthUserScopeService;
import com.zhiling.system.auth.service.support.TokenStorePort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 认证域服务实现。
 *
 * @author zhanghongyu
 */
@Service
public class AuthDomainServiceImpl implements AuthDomainService {

    private final AuthenticationExecutorPort authenticationExecutorPort;
    private final CaptchaService captchaService;
    private final TokenStorePort tokenStorePort;
    private final JwtProperties jwtProperties;
    private final JwtUtil jwtUtil;
    private final AuthRoleQueryService authRoleQueryService;
    private final AuthResourceQueryService authResourceQueryService;
    private final AuthUserScopeService authUserScopeService;
    private final SecurityHelper securityHelper;

    public AuthDomainServiceImpl(AuthenticationExecutorPort authenticationExecutorPort,
                                 CaptchaService captchaService,
                                 TokenStorePort tokenStorePort,
                                 JwtProperties jwtProperties,
                                 JwtUtil jwtUtil,
                                 AuthRoleQueryService authRoleQueryService,
                                 AuthResourceQueryService authResourceQueryService,
                                 AuthUserScopeService authUserScopeService,
                                 SecurityHelper securityHelper) {
        this.authenticationExecutorPort = authenticationExecutorPort;
        this.captchaService = captchaService;
        this.tokenStorePort = tokenStorePort;
        this.jwtProperties = jwtProperties;
        this.jwtUtil = jwtUtil;
        this.authRoleQueryService = authRoleQueryService;
        this.authResourceQueryService = authResourceQueryService;
        this.authUserScopeService = authUserScopeService;
        this.securityHelper = securityHelper;
    }

    /**
     * 方法：login
     *
     * @author zhanghongyu
     */
    @Override
    public LoginVo login(LoginDto loginDto) {
        // 登录先过验证码，再走用户名密码认证，避免无效爆破直接打到认证器。
        captchaService.validateCaptcha(loginDto.getCaptchaKey(), loginDto.getCaptchaCode());
        Authentication authenticate;
        try {
            authenticate = authenticationExecutorPort.authenticate(loginDto.getUsername(), loginDto.getPassword());
        } catch (AuthenticationException e) {
            throw new ProjectException(401, "用户名或密码错误");
        } catch (Exception e) {
            throw new ProjectException(500, "认证服务异常: " + e.getMessage());
        }
        if (!authenticate.isAuthenticated()) {
            throw new ProjectException(401, "用户名或密码错误");
        }

        UserAuth userAuth = (UserAuth) authenticate.getPrincipal();
        LoginVo userLoginVo = BeanUtil.copyProperties(userAuth, LoginVo.class);
        userLoginVo.setSanaId(userAuth.getSanaId());

        // 角色标签用于前端菜单和数据权限判断。
        List<Role> roleList = authRoleQueryService.getRoleListByUserId(userAuth.getId());
        Set<String> roleLabelsSet = roleList.stream()
                .map(Role::getLabel)
                .collect(Collectors.toSet());
        userLoginVo.setRoleLabels(roleLabelsSet);

        // 登录响应需要同时带菜单页和接口权限，前端用菜单页资源控制导航显隐。
        List<Resource> resourceList = authResourceQueryService.getResourceListByUserId(userAuth.getId());
        Set<String> resourcePathSet = resourceList.stream()
                .filter(resource -> "m".equals(resource.getResourceType()) || "r".equals(resource.getResourceType()))
                .map(Resource::getRequestPath)
                .filter(path -> path != null && !path.isBlank())
                .collect(Collectors.toSet());
        userLoginVo.setResourcePaths(resourcePathSet);

        Long userId = Long.valueOf(userAuth.getId());
        Set<Long> sanaScopeIds = new LinkedHashSet<>(authUserScopeService.listSanaScopeIdsByUserId(userId));
        if (sanaScopeIds.isEmpty() && userLoginVo.getSanaId() != null) {
            sanaScopeIds.add(userLoginVo.getSanaId());
        }
        userLoginVo.setSanaScopeIds(sanaScopeIds);

        // 对外暴露的是随机 userToken，真实 JWT 仅保存在 Redis，便于统一续期和失效控制。
        userLoginVo.setPassword("");
        String userToken = UUID.randomUUID().toString();
        userLoginVo.setToken(userToken);

        Map<String, Object> claims = new HashMap<>();
        String loginVoJson = JSONUtil.toJsonStr(userLoginVo);
        claims.put("currentUser", loginVoJson);

        String jwtToken = jwtUtil.generateToken(claims);
        Long ttl = Long.valueOf(jwtProperties.getExpireTime() / 1000);
        tokenStorePort.saveTokens(userLoginVo.getUsername(), userToken, jwtToken, ttl);
        return userLoginVo;
    }

    /**
     * 方法：logout
     *
     * @author zhanghongyu
     */
    @Override
    public Boolean logout() {
        Optional<CurrentUser> currentUserOpt = securityHelper.getCurrentUser();
        if (currentUserOpt.isEmpty()) {
            return true;
        }
        CurrentUser currentUser = currentUserOpt.get();
        // 登出时同时删除用户 -> token 与 token -> jwt 的映射，彻底失效当前会话。
        tokenStorePort.deleteTokens(currentUser.getUsername(), currentUser.getToken());
        return true;
    }

}
