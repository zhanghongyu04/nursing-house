package com.zhiling.system.infrastructure.ezviz.service;

/**

 * EzvizTokenService

 *

 * @author zhanghongyu

 */

public interface EzvizTokenService {

    String getValidAccessToken();

    void refreshAccessToken();

    void invalidateAccessToken();
}

