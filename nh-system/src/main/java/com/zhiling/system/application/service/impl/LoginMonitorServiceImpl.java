package com.zhiling.system.application.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhiling.common.constant.UserConstant;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.common.security.LoginVo;
import com.zhiling.common.utils.AddressUtil;
import com.zhiling.common.utils.IPUtil;
import com.zhiling.common.utils.JwtUtil;
import com.zhiling.framework.redis.NhRedisBeanNames;
import com.zhiling.framework.redis.RedisKeyScanSupport;
import com.zhiling.framework.security.SecurityHelper;
import com.zhiling.model.entity.LoginLog;
import com.zhiling.model.vo.OnlineUserVo;
import com.zhiling.system.application.service.LoginMonitorService;
import com.zhiling.system.infrastructure.persistence.mapper.LoginLogMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 登录与在线状态监控服务实现。
 *
 * @author zhanghongyu
 */
@Slf4j
@Service
public class LoginMonitorServiceImpl implements LoginMonitorService {
    private static final String LOGIN_LOGS_PATH = "/web/monitor/loginLogs";
    private static final String ONLINE_USERS_PATH = "/web/monitor/onlineUsers";
    private static final String KICK_OUT_PATH = "/web/monitor/kickOut";

    private final LoginLogMapper loginLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final SecurityHelper securityHelper;

    public LoginMonitorServiceImpl(LoginLogMapper loginLogMapper,
                                   @Qualifier(NhRedisBeanNames.AUTH_STRING_REDIS_TEMPLATE) StringRedisTemplate stringRedisTemplate,
                                   JwtUtil jwtUtil,
                                   SecurityHelper securityHelper) {
        this.loginLogMapper = loginLogMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtUtil = jwtUtil;
        this.securityHelper = securityHelper;
    }

    @Override
    public void recordSuccess(LoginVo loginVo, HttpServletRequest request) {
        LoginLog loginLog = buildBaseLog(loginVo == null ? null : loginVo.getUsername(), request);
        if (loginVo != null && loginVo.getId() != null) {
            loginLog.setUserId(Long.valueOf(loginVo.getId()));
        }
        loginLog.setSuccessFlag(1);
        loginLog.setMessage("登录成功");
        saveSafely(loginLog);
    }

    @Override
    public void recordFailure(String username, String message, HttpServletRequest request) {
        LoginLog loginLog = buildBaseLog(username, request);
        loginLog.setSuccessFlag(0);
        loginLog.setMessage(StringUtils.hasText(message) ? message : "登录失败");
        saveSafely(loginLog);
    }

    @Override
    public IPage<LoginLog> pageLoginLogs(Integer pageNum, Integer pageSize, String username, Integer successFlag) {
        requireResource("monitor.loginLogs", LOGIN_LOGS_PATH);
        int current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<LoginLog>()
                .like(StringUtils.hasText(username), LoginLog::getUsername, username)
                .eq(successFlag != null, LoginLog::getSuccessFlag, successFlag)
                .orderByDesc(LoginLog::getLoginTime)
                .orderByDesc(LoginLog::getId);
        return loginLogMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public List<OnlineUserVo> listOnlineUsers() {
        requireResource("monitor.onlineUsers", ONLINE_USERS_PATH);
        Set<String> keys = RedisKeyScanSupport.scanKeys(stringRedisTemplate, UserConstant.USER_TOKEN + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<OnlineUserVo> users = new ArrayList<>();
        for (String key : keys) {
            String username = key.substring(UserConstant.USER_TOKEN.length());
            String userToken = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(userToken)) {
                continue;
            }
            OnlineUserVo onlineUser = buildOnlineUser(username, userToken);
            if (onlineUser != null) {
                users.add(onlineUser);
            }
        }
        users.sort(Comparator.comparing(OnlineUserVo::getLastLoginTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return users;
    }

    @Override
    public Boolean kickOut(String username) {
        requireResource("monitor.kickOut", KICK_OUT_PATH);
        if (!StringUtils.hasText(username)) {
            throw new ProjectException(400, "用户名不能为空");
        }
        String currentUsername = securityHelper.getCurrentUser()
                .map(currentUser -> currentUser.getUsername())
                .orElse(null);
        if (username.equals(currentUsername)) {
            throw new ProjectException(400, "不能踢下线当前登录用户");
        }
        String userToken = stringRedisTemplate.opsForValue().get(UserConstant.USER_TOKEN + username);
        if (!StringUtils.hasText(userToken)) {
            throw new ProjectException(404, "用户当前不在线");
        }
        stringRedisTemplate.delete(UserConstant.USER_TOKEN + username);
        stringRedisTemplate.delete(UserConstant.JWT_TOKEN + userToken);
        return true;
    }

    private void requireResource(String scene, String resourcePath) {
        if (!securityHelper.hasResourcePathForSensitiveOperation(resourcePath)) {
            log.warn("[LoginMonitorPermission] 用户 {} 缺少登录监控权限 scene={}, path={}",
                    securityHelper.getCurrentUserId(), scene, resourcePath);
            throw new AccessDeniedException("无权访问登录监控功能");
        }
    }

    private LoginLog buildBaseLog(String username, HttpServletRequest request) {
        String ip = request == null ? "" : IPUtil.getIpAddr(request);
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setLoginIp(ip);
        loginLog.setLoginLocation(resolveLocation(ip));
        loginLog.setUserAgent(request == null ? "" : request.getHeader("User-Agent"));
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setStatus(0);
        return loginLog;
    }

    private OnlineUserVo buildOnlineUser(String username, String userToken) {
        try {
            String jwtToken = stringRedisTemplate.opsForValue().get(UserConstant.JWT_TOKEN + userToken);
            if (!StringUtils.hasText(jwtToken)) {
                return null;
            }
            Claims claims = jwtUtil.parseToken(jwtToken);
            LoginVo loginVo = JSONUtil.toBean(String.valueOf(claims.get("currentUser")), LoginVo.class);
            OnlineUserVo onlineUser = new OnlineUserVo();
            onlineUser.setUserId(loginVo.getId() == null ? null : Long.valueOf(loginVo.getId()));
            onlineUser.setUsername(loginVo.getUsername() == null ? username : loginVo.getUsername());
            onlineUser.setSanaId(loginVo.getSanaId());
            onlineUser.setRoleLabels(loginVo.getRoleLabels());
            onlineUser.setToken(maskToken(userToken));
            onlineUser.setExpireSeconds(stringRedisTemplate.getExpire(UserConstant.USER_TOKEN + username));
            onlineUser.setLastLoginTime(resolveLastLoginTime(username));
            return onlineUser;
        } catch (Exception ex) {
            log.warn("解析在线用户信息失败 username={}", username, ex);
            return null;
        }
    }

    private LocalDateTime resolveLastLoginTime(String username) {
        LoginLog loginLog = loginLogMapper.selectOne(new LambdaQueryWrapper<LoginLog>()
                .eq(LoginLog::getUsername, username)
                .eq(LoginLog::getSuccessFlag, 1)
                .orderByDesc(LoginLog::getLoginTime)
                .last("limit 1"));
        return loginLog == null ? null : loginLog.getLoginTime();
    }

    private String resolveLocation(String ip) {
        try {
            return StringUtils.hasText(ip) ? AddressUtil.getRealAddressByIP(ip) : "";
        } catch (Exception ex) {
            log.warn("解析登录IP归属地失败 ip={}", ip, ex);
            return "未知";
        }
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token) || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    private void saveSafely(LoginLog loginLog) {
        try {
            loginLogMapper.insert(loginLog);
        } catch (Exception ex) {
            log.warn("保存登录日志失败 username={}", loginLog.getUsername(), ex);
        }
    }
}
