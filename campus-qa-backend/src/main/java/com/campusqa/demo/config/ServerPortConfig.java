package com.campusqa.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 强制锁定服务端口为 8081，并在启动时输出明确日志。
 */
@Configuration
public class ServerPortConfig {

    private static final Logger log = LoggerFactory.getLogger(ServerPortConfig.class);
    private static final int FIXED_PORT = 8081;

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> forcePort8081Customizer() {
        return factory -> factory.setPort(FIXED_PORT);
    }

    @Bean
    public ApplicationListener<WebServerInitializedEvent> serverReadyLogger() {
        return event -> {
            int actualPort = event.getWebServer().getPort();
            if (actualPort == FIXED_PORT) {
                log.info("Server is running on 8081");
                return;
            }
            log.warn("Server port drift detected, expected 8081 but actual port is {}", actualPort);
        };
    }
}
