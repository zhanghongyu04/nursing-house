package com.zhiling.system.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * System 模块 Mapper 自动配置。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
@MapperScan({
    "com.zhiling.system.infrastructure.persistence.mapper",
    "com.zhiling.system.infrastructure.ezviz.mapper"
})
public class SystemMapperAutoConfiguration {
}
