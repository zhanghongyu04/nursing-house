package com.zhiling.system.infrastructure.ezviz.config;

/**

 * EzvizCredential

 *

 * @author zhanghongyu

 */

public record EzvizCredential(String appKey, String appSecret) {

    /**
     * 方法：isValid
     *
     * @author zhanghongyu
     */
    public boolean isValid() {
        return appKey != null && !appKey.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }
}