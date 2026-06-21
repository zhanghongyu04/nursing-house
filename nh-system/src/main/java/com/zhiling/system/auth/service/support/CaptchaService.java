package com.zhiling.system.auth.service.support;

/**

 * CaptchaService

 *

 * @author zhanghongyu

 */

public interface CaptchaService {

    CaptchaImageResult generateCaptchaImage();

    void validateCaptcha(String captchaKey, String captchaCode);
}

