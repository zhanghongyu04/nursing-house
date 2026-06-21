package com.zhiling.system.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhiling.common.security.LoginVo;
import com.zhiling.model.entity.LoginLog;
import com.zhiling.model.vo.OnlineUserVo;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 登录与在线状态监控服务。
 *
 * @author zhanghongyu
 */
public interface LoginMonitorService {

    void recordSuccess(LoginVo loginVo, HttpServletRequest request);

    void recordFailure(String username, String message, HttpServletRequest request);

    IPage<LoginLog> pageLoginLogs(Integer pageNum, Integer pageSize, String username, Integer successFlag);

    List<OnlineUserVo> listOnlineUsers();

    Boolean kickOut(String username);
}
