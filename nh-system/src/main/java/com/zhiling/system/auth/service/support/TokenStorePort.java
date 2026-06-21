package com.zhiling.system.auth.service.support;

/**

 * TokenStorePort

 *

 * @author zhanghongyu

 */

public interface TokenStorePort {

    void saveTokens(String username, String userToken, String jwtToken, long ttlSeconds);

    void refreshTokens(String username, String userToken, String jwtToken, long ttlSeconds);

    String getJwtToken(String userToken);

    String getUserToken(String username);

    void deleteTokens(String username, String userToken);
}

