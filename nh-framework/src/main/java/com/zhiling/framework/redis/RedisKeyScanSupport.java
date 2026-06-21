package com.zhiling.framework.redis;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 使用 SCAN 替代 KEYS，兼容 Redis ACL 对危险命令的限制。
 */
public final class RedisKeyScanSupport {

    private RedisKeyScanSupport() {
    }

    public static Set<String> scanKeys(StringRedisTemplate template, String pattern) {
        if (template == null || pattern == null || pattern.isBlank()) {
            return Set.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(200).build();
        try (Cursor<String> cursor = template.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Redis SCAN 失败, pattern=" + pattern, ex);
        }
        return keys;
    }
}
