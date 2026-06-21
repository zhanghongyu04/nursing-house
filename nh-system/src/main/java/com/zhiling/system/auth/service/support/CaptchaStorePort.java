package com.zhiling.system.auth.service.support;

/**

 * CaptchaStorePort

 *

 * @author zhanghongyu

 */

public interface CaptchaStorePort {

    void save(String captchaKey, String captchaText, long ttlSeconds);

    String get(String captchaKey);

    void remove(String captchaKey);
}

