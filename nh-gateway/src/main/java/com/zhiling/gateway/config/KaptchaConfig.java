package com.zhiling.gateway.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Kaptcha 验证码配置
 *
 * @author zhanghongyu
 */
@Configuration
public class KaptchaConfig {

    /**
     * 默认验证码生成器
     */
    @Bean
    public DefaultKaptcha defaultKaptcha() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();

        // 图片边框
        properties.setProperty(Constants.KAPTCHA_BORDER, "no");

        // 验证码长度
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "4");

        // 验证码字符范围
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_CHAR_STRING, "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789");

        // 字体
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_FONT_NAMES, "宋体,楷体,微软雅黑");

        // 字体大小
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_FONT_SIZE, "32");

        // 字体颜色
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_FONT_COLOR, "72,118,255");

        // 文字间隔
        properties.setProperty(Constants.KAPTCHA_TEXTPRODUCER_CHAR_SPACE, "6");

        // 干扰线颜色
        properties.setProperty(Constants.KAPTCHA_NOISE_COLOR, "180,180,180");

        // 图片宽度
        properties.setProperty(Constants.KAPTCHA_IMAGE_WIDTH, "130");

        // 图片高度
        properties.setProperty(Constants.KAPTCHA_IMAGE_HEIGHT, "48");

        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);

        return defaultKaptcha;
    }

    /**
     * Kaptcha 常量定义
     */
    public static class Constants {
        // 边框
        public static final String KAPTCHA_BORDER = "kaptcha.border";

        // 字体颜色
        public static final String KAPTCHA_TEXTPRODUCER_FONT_COLOR = "kaptcha.textproducer.font.color";

        // 字体大小
        public static final String KAPTCHA_TEXTPRODUCER_FONT_SIZE = "kaptcha.textproducer.font.size";

        // 文字间隔
        public static final String KAPTCHA_TEXTPRODUCER_CHAR_SPACE = "kaptcha.textproducer.char.space";

        // 验证码长度
        public static final String KAPTCHA_TEXTPRODUCER_CHAR_LENGTH = "kaptcha.textproducer.char.length";

        // 验证码字符范围
        public static final String KAPTCHA_TEXTPRODUCER_CHAR_STRING = "kaptcha.textproducer.char.string";

        // 字体
        public static final String KAPTCHA_TEXTPRODUCER_FONT_NAMES = "kaptcha.textproducer.font.names";

        // 图片宽度
        public static final String KAPTCHA_IMAGE_WIDTH = "kaptcha.image.width";

        // 图片高度
        public static final String KAPTCHA_IMAGE_HEIGHT = "kaptcha.image.height";

        // 干扰颜色
        public static final String KAPTCHA_NOISE_COLOR = "kaptcha.noise.color";

    }
}

