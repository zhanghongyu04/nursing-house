package com.zhiling.system.interfaces.security;

import com.zhiling.common.context.UserThreadLocal;
import com.zhiling.common.properties.JwtProperties;
import com.zhiling.common.security.UserAuth;
import com.zhiling.common.utils.JwtUtil;
import com.zhiling.framework.security.CurrentUserContextHolder;
import com.zhiling.framework.security.model.CurrentUserContext;
import com.zhiling.system.auth.service.support.TokenStorePort;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证过滤器：解析 Token 并设置 SecurityContext、CurrentUserContextHolder、UserThreadLocal。
 *
 * @author zhanghongyu
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final TokenStorePort tokenStorePort;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private static final String NO_SLIDING_REFRESH_HEADER = "X-No-Sliding-Refresh";

    /**
     * 构造器：JwtAuthenticationFilter
     *
     * @author zhanghongyu
     */
    public JwtAuthenticationFilter(TokenStorePort tokenStorePort, JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.tokenStorePort = tokenStorePort;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userToken = request.getHeader("Authorization");

        if (userToken != null) {
            if (userToken.startsWith("Bearer ")) {
                userToken = userToken.substring(7);
            }
            // 前端实际透传的是用户侧 token，服务端再映射回对应 JWT 做签名校验和续期。
            String jwtToken = tokenStorePort.getJwtToken(userToken);
            if (jwtToken != null) {
                try {
                    Claims claims = jwtUtil.parseToken(jwtToken);
                    CurrentUserContext currentUserContext = CurrentUserContextMapper.fromClaims(claims, userToken);
                    if (currentUserContext == null) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    String currentUserToken = tokenStorePort.getUserToken(currentUserContext.getUsername());
                    if (!userToken.equals(currentUserToken)) {
                        log.warn("JWT认证发现 token 不匹配(用户:{}, 期望:{}, 实际:{})", currentUserContext.getUsername(), currentUserToken, userToken);
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }
                    // 鉴权通过后，同时回填 Spring Security、当前用户上下文和历史 ThreadLocal 兼容层。
                    if (!skipSlidingRefresh(request)) {
                        refreshSlidingSession(currentUserContext, userToken, claims);
                    }
                    CurrentUserContextHolder.set(request, currentUserContext);
                    UserThreadLocal.set(currentUserContext.toLoginVo());
                    UserDetails userDetails = new UserAuth(currentUserContext.toLoginVo());
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    log.warn("JWT认证异常: {}", e.getMessage());
                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 方法：refreshSlidingSession
     *
     * @author zhanghongyu
     */
    private void refreshSlidingSession(CurrentUserContext currentUserContext, String userToken, Claims claims) {
        Object currentUserClaim = claims.get("currentUser");
        if (currentUserClaim == null) {
            log.warn("用户 {} 缺少 currentUser claim，跳过会话续期", currentUserContext.getUsername());
            return;
        }

        Map<String, Object> refreshedClaims = new HashMap<>();
        refreshedClaims.put("currentUser", String.valueOf(currentUserClaim));
        String refreshedJwtToken = jwtUtil.generateToken(refreshedClaims);
        // 续期时保持 userToken 不变，只替换底层 JWT 和 Redis TTL，前端无感知。
        long ttlSeconds = jwtProperties.getExpireTime() / 1000;
        tokenStorePort.refreshTokens(currentUserContext.getUsername(), userToken, refreshedJwtToken, ttlSeconds);
    }

    /**
     * 前端在线状态轮询只用于发现踢下线/过期，不应刷新滑动会话窗口。
     */
    private boolean skipSlidingRefresh(HttpServletRequest request) {
        String noRefresh = request.getHeader(NO_SLIDING_REFRESH_HEADER);
        return "true".equalsIgnoreCase(noRefresh) || "1".equals(noRefresh);
    }
}
