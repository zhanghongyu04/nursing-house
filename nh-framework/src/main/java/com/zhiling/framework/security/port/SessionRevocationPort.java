package com.zhiling.framework.security.port;

/**
 * 会话吊销端口：在角色/机构授权变更后强制用户重新登录。
 */
public interface SessionRevocationPort {

    /**
     * 吊销指定用户名的所有 Redis 登录态。
     *
     * @return 是否找到并删除了 token 映射
     */
    boolean revokeByUsername(String username);

    /**
     * 按用户 ID 吊销登录态。
     */
    boolean revokeByUserId(Long userId);
}
