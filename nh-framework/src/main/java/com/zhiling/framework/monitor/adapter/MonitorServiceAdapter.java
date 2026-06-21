package com.zhiling.framework.monitor.adapter;

import com.zhiling.common.utils.AddressUtil;
import com.zhiling.common.utils.IPUtil;
import com.zhiling.framework.monitor.service.MonitorService;
import com.zhiling.framework.monitor.server.Server;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 监控服务适配器（将现有监控实现适配到 nh-framework）。
 *
 * @author zhanghongyu
 */
@Component
public class MonitorServiceAdapter implements MonitorService {

    private final RedisTemplate redisTemplate;

    /**
     * 构造器：MonitorServiceAdapter
     *
     * @author zhanghongyu
     */
    public MonitorServiceAdapter(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 方法：monitorIp
     *
     * @author zhanghongyu
     */
    @Override
    public Map<String, String> monitorIp(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        String ip = IPUtil.getIpAddr(request);
        String location = AddressUtil.getRealAddressByIP(ip);
        result.put("ip", ip);
        result.put("location", location);
        return result;
    }

    @Override
    public Map<String, Object> getRedisInfo() throws Exception {
        Map<String, Object> result = new HashMap<>(3);
        Properties info = (Properties) redisTemplate
                .execute((RedisCallback<Object>) connection -> connection.commands().info());
        Properties commandStats = (Properties) redisTemplate
                .execute((RedisCallback<Object>) connection -> connection.commands().info("commandstats"));
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) connection -> connection.commands().dbSize());
        result.put("info", info);
        result.put("dbSize", dbSize);

        List<Map<String, String>> pieList = new ArrayList<>();
        commandStats.stringPropertyNames().forEach(key -> {
            Map<String, String> data = new HashMap<>(2);
            String property = commandStats.getProperty(key);
            data.put("name", StringUtils.removeStart(key, "cmdstat_"));
            data.put("value", StringUtils.substringBetween(property, "calls=", ",usec"));
            pieList.add(data);
        });
        result.put("commandStats", pieList);
        return result;
    }

    @Override
    public Object getServerInfo() throws Exception {
        Server server = new Server();
        server.copyTo();
        return server;
    }
}
