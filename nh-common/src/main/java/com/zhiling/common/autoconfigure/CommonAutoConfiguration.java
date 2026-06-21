package com.zhiling.common.autoconfigure;

import com.zhiling.common.properties.JwtProperties;
import com.zhiling.common.properties.QdrantProperties;
import com.zhiling.common.properties.RedisProperties;
import com.zhiling.common.properties.RustFsProperties;
import com.zhiling.common.properties.SecurityConfigProperties;
import com.zhiling.common.utils.FileUtil;
import com.zhiling.common.utils.JwtUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 通用模块自动配置。
 *
 * 注册配置属性类和工具 bean。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
@EnableConfigurationProperties({
    JwtProperties.class,
    QdrantProperties.class,
    RedisProperties.class,
    RustFsProperties.class,
    SecurityConfigProperties.class
})
public class CommonAutoConfiguration {

    /**
     * 方法：jwtUtil
     *
     * @author zhanghongyu
     */
    @Bean
    @ConditionalOnMissingBean
    public JwtUtil jwtUtil(JwtProperties jwtProperties) {
        return new JwtUtil(jwtProperties);
    }

    /**
     * 方法：fileUtil
     *
     * @author zhanghongyu
     */
    @Bean
    @ConditionalOnMissingBean
    public FileUtil fileUtil(RustFsProperties rustFsProperties) {
        return new FileUtil(rustFsProperties);
    }
}