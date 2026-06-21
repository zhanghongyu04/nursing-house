package com.zhiling.system.interfaces.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 异常处理自动配置。
 *
 * 负责导出 system 模块的全局异常处理器，确保通过自动配置接入时
 * {@code @RestControllerAdvice} 能被 Spring 容器正确发现。
 *
 * @author zhanghongyu
 */
@AutoConfiguration
@ComponentScan("com.zhiling.system.interfaces.exception")
public class ExceptionAutoConfiguration {
}