package com.zhiling.framework.security.port;

import com.zhiling.framework.security.model.AccessScope;

import java.util.Optional;

/**
 * 从权威数据源（数据库）实时解析用户访问范围。
 *
 * <p>用于 Agent RAG、MCP 等敏感操作，避免仅信任 JWT 内快照导致权限滞后。
 */
public interface LiveAccessScopePort {

    /**
     * 按用户 ID 加载当前有效访问范围。
     */
    Optional<AccessScope> loadAccessScope(Long userId);
}
