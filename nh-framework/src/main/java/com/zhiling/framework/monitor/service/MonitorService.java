package com.zhiling.framework.monitor.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 监控服务接口。
 *
 * @author zhanghongyu
 */
public interface MonitorService {

    Map<String, String> monitorIp(HttpServletRequest request);

    Map<String, Object> getRedisInfo() throws Exception;

    Object getServerInfo() throws Exception;
}