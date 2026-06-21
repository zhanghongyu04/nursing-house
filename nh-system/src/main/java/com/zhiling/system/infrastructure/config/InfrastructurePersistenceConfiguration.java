package com.zhiling.system.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure 持久化层 Bean 注册。
 *
 * 独立于 {@code SystemPortAutoConfiguration}，集中管理 persistence 层
 * query/command 实现类的扫描注册，避免主自动配置直接扫描基础设施内部包。
 *
 * @author zhanghongyu
 */
@Configuration
@ComponentScan(basePackages = {
    "com.zhiling.system.infrastructure.persistence.query.impl",
    "com.zhiling.system.infrastructure.persistence.command.impl",
    "com.zhiling.system.infrastructure.persistence.repository.impl"
})
public class InfrastructurePersistenceConfiguration {
}
