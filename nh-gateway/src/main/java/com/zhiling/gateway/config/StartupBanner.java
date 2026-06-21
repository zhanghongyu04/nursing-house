package com.zhiling.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动成功后的输出
 *
 * @author zhanghongyu
 */
@Slf4j
@Component
@Order(1)
public class StartupBanner implements ApplicationRunner {

    /**
     * 方法：run
     *
     * @author zhanghongyu
     */
    @Override
    public void run(ApplicationArguments args) {
        System.out.print("""
                ███████╗██╗  ██╗ ██████╗███╗   ███╗███████╗
                ██╔════╝██║  ██║██╔════╝████╗ ████║██╔════╝
                ███████╗███████║██║     ██╔████╔██║███████╗
                ╚════██║██╔══██║██║     ██║╚██╔╝██║╚════██║
                ███████║██║  ██║╚██████╗██║ ╚═╝ ██║███████║
                ╚══════╝╚═╝  ╚═╝ ╚═════╝╚═╝     ╚═╝╚══════╝
                   ▓▓▓▓▓   ▓▓   ▓▓  ▓▓▓▓▓  ▓▓     ▓▓  ▓▓▓▓▓
                """);
    }
}
