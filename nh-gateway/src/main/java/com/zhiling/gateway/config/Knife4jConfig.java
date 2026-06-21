package com.zhiling.gateway.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

 /**
 * Knife4j 配置类
 *
 * @author zhanghongyu
 */
@Configuration
@EnableKnife4j
public class Knife4jConfig {

    /**
     * 方法：customOpenAPI
     *
     * @author zhanghongyu
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧康养管理系统")
                        .version("1.0.0")
                        .description("智慧康养管理系统后端接口文档"));
    }



}