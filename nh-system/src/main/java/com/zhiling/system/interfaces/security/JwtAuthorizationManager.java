package com.zhiling.system.interfaces.security;
import cn.hutool.core.util.ObjectUtil;
import com.zhiling.common.utils.JwtUtil;
import com.zhiling.framework.security.model.CurrentUserContext;
import com.zhiling.model.entity.Resource;
import com.zhiling.system.auth.service.support.AuthResourceQueryService;
import com.zhiling.system.auth.service.support.TokenStorePort;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * 授权管理器
 *
 * @author zhanghongyu
 */
@Slf4j
public class JwtAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
    private static final String[] AUTHENTICATED_ALLOW_PATHS = new String[]{
            "/api/v1/login",
            "/api/v1/captcha**",
            "/api/v1/logout",
            "/error",
            "/api/v1/user/**",
            "/api/v1/commonFile/**"
    };

    private final TokenStorePort tokenStorePort;
    private final JwtUtil jwtUtil;
    private final AuthResourceQueryService authResourceQueryService;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public JwtAuthorizationManager(TokenStorePort tokenStorePort,
                                   JwtUtil jwtUtil,
                                   AuthResourceQueryService authResourceQueryService) {
        this.tokenStorePort = tokenStorePort;
        this.jwtUtil = jwtUtil;
        this.authResourceQueryService = authResourceQueryService;
    }

    /**
     * 方法：check
     *
     * @author zhanghongyu
     */
    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier, RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        if (CorsUtils.isPreFlightRequest(request)) {
            log.debug("CORS preflight请求放行: {}", request.getRequestURI());
            return new AuthorizationDecision(true);
        }

        String requestURI = request.getRequestURI();

        // 先检查是否在白名单路径中（无需认证即可访问）
        for (String pattern : AUTHENTICATED_ALLOW_PATHS) {
            if (antPathMatcher.match(pattern, requestURI)) {
                log.info("请求 {} 匹配白名单 {}，放行", requestURI, pattern);
                return new AuthorizationDecision(true);
            }
        }
        log.info("请求 {} 未匹配白名单，进入token验证", requestURI);

        // 检查 token
        String userToken = request.getHeader("Authorization");
        if (ObjectUtil.isEmpty(userToken)) {
            log.warn("请求 {} 缺少Authorization header，拒绝访问", requestURI);
            return new AuthorizationDecision(false);
        }
        if (userToken.startsWith("Bearer ")) {
            userToken = userToken.substring(7);
        }

        String jwtToken = tokenStorePort.getJwtToken(userToken);
        if (ObjectUtil.isEmpty(jwtToken)) {
            log.warn("请求 {} token无效或已过期，拒绝访问", requestURI);
            return new AuthorizationDecision(false);
        }

        Claims claims = jwtUtil.parseToken(jwtToken);
        if (ObjectUtil.isEmpty(claims)) {
            log.warn("请求 {} JWT解析失败，拒绝访问", requestURI);
            return new AuthorizationDecision(false);
        }

        CurrentUserContext currentUserContext = CurrentUserContextMapper.fromClaims(claims, userToken);
        if (currentUserContext == null) {
            log.warn("请求 {} 当前用户上下文为空，拒绝访问", requestURI);
            return new AuthorizationDecision(false);
        }

        String currentUserToken = tokenStorePort.getUserToken(currentUserContext.getUsername());
        if (!userToken.equals(currentUserToken)) {
            log.warn("请求 {} token不匹配(用户:{}, 期望:{}, 实际:{}), 拒绝访问", requestURI, currentUserContext.getUsername(), currentUserToken, userToken);
            return new AuthorizationDecision(false);
        }

        Set<String> resourcePaths = resolveResourcePaths(currentUserContext);
        if (ObjectUtil.isEmpty(resourcePaths)) {
            log.warn("请求 {} 用户 {} 无资源权限配置，拒绝访问", requestURI, currentUserContext.getUsername());
            return new AuthorizationDecision(false);
        }

        // 将 /api/v1/xxx 转换为 /web/xxx 以匹配数据库中存储的权限路径
        String matchPath = requestURI.replaceFirst("^/api/v1/", "/web/");
        boolean matched = resourcePaths.stream().anyMatch(pattern -> antPathMatcher.match(pattern, matchPath));
        if (matched) {
            log.debug("请求 {} 匹配用户 {} 的资源权限，放行", requestURI, currentUserContext.getUsername());
        } else {
            log.warn("请求 {} 不在用户 {} 的资源权限列表中，拒绝访问。权限列表: {}", requestURI, currentUserContext.getUsername(), resourcePaths);
        }
        return new AuthorizationDecision(matched);
    }

    /**
     * 方法：resolveResourcePaths
     *
     * @author zhanghongyu
     */
    private Set<String> resolveResourcePaths(CurrentUserContext currentUserContext) {
        if (currentUserContext == null || currentUserContext.getUserId() == null) {
            return Set.of();
        }
        try {
            List<Resource> resourceList = authResourceQueryService.getResourceListByUserId(String.valueOf(currentUserContext.getUserId()));
            Set<String> liveResourcePaths = resourceList.stream()
                    .filter(resource -> "r".equals(resource.getResourceType()))
                    .map(Resource::getRequestPath)
                    .filter(path -> path != null && !path.isBlank())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!liveResourcePaths.isEmpty()) {
                return liveResourcePaths;
            }
            log.warn("用户 {} 实时资源权限为空，回退到 token 内资源权限", currentUserContext.getUsername());
        } catch (Exception ex) {
            log.error("查询用户 {} 实时资源权限失败，回退到 token 内资源权限", currentUserContext.getUsername(), ex);
        }
        return currentUserContext.getResourcePaths();
    }

}
