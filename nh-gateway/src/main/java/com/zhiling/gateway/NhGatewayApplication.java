package com.zhiling.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 网关启动入口。
 * 仅扫描 gateway 模块，其他模块通过自动配置导入。
 * @author zhanghongyu
 */
@SpringBootApplication
@EnableScheduling
public class NhGatewayApplication {

    /**
     * 方法：main
     *
     * @author zhanghongyu
     */
    public static void main(String[] args) {
        SpringApplication.run(NhGatewayApplication.class, args);
    }
}

