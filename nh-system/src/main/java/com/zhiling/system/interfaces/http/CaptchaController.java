package com.zhiling.system.interfaces.http;

import com.zhiling.common.result.Result;
import com.zhiling.system.auth.service.support.CaptchaImageResult;
import com.zhiling.system.auth.service.support.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 验证码控制器
 * 生成图片验证码并存储到 Redis
 *
 * @author zhanghongyu
 */
@RestController
@Tag(name = "验证码接口")
@Slf4j
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 构造器：CaptchaController
     *
     * @author zhanghongyu
     */
    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * 获取验证码（JSON 格式，返回 base64 图片）
     * 前端推荐使用此接口
     */
    @GetMapping("/api/v1/captcha")
    @Operation(summary = "获取验证码（JSON格式）")
    public Result<Map<String, String>> getCaptcha() {
        CaptchaImageResult captchaImageResult = captchaService.generateCaptchaImage();
        Map<String, String> data = new HashMap<>();
        data.put("captchaKey", captchaImageResult.captchaKey());
        data.put("captchaImage", captchaImageResult.captchaImage());
        return Result.success(data);
    }

}
