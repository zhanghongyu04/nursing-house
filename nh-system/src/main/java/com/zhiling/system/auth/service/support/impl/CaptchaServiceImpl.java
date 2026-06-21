package com.zhiling.system.auth.service.support.impl;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.zhiling.common.exception.ProjectException;
import com.zhiling.system.auth.service.support.CaptchaImageResult;
import com.zhiling.system.auth.service.support.CaptchaService;
import com.zhiling.system.auth.service.support.CaptchaStorePort;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

@Service
/**
 * CaptchaServiceImpl
 *
 * @author zhanghongyu
 */
public class CaptchaServiceImpl implements CaptchaService {

    private static final int CAPTCHA_EXPIRE_SECONDS = 300;

    private final DefaultKaptcha defaultKaptcha;
    private final CaptchaStorePort captchaStorePort;

    /**
     * 构造器：CaptchaServiceImpl
     *
     * @author zhanghongyu
     */
    public CaptchaServiceImpl(DefaultKaptcha defaultKaptcha, CaptchaStorePort captchaStorePort) {
        this.defaultKaptcha = defaultKaptcha;
        this.captchaStorePort = captchaStorePort;
    }

    /**
     * 方法：generateCaptchaImage
     *
     * @author zhanghongyu
     */
    @Override
    public CaptchaImageResult generateCaptchaImage() {
        String captchaText = defaultKaptcha.createText();
        String captchaKey = UUID.randomUUID().toString();
        captchaStorePort.save(captchaKey, captchaText, CAPTCHA_EXPIRE_SECONDS);
        BufferedImage captchaImage = defaultKaptcha.createImage(captchaText);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(captchaImage, "jpg", baos);
            String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
            return new CaptchaImageResult(captchaKey, "data:image/jpeg;base64," + base64Image);
        } catch (Exception e) {
            throw new ProjectException(500, "验证码生成失败");
        }
    }

    /**
     * 方法：validateCaptcha
     *
     * @author zhanghongyu
     */
    @Override
    public void validateCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaKey.isEmpty()) {
            throw new ProjectException(400, "验证码 key 不能为空");
        }
        if (captchaCode == null || captchaCode.isEmpty()) {
            throw new ProjectException(400, "请输入验证码");
        }
        String storedCaptcha = captchaStorePort.get(captchaKey);
        if (storedCaptcha == null) {
            throw new ProjectException(400, "验证码已过期，请重新获取");
        }
        if (!storedCaptcha.equalsIgnoreCase(captchaCode)) {
            throw new ProjectException(400, "验证码错误");
        }
        captchaStorePort.remove(captchaKey);
    }
}