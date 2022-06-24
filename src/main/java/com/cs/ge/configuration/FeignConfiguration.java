package com.cs.ge.configuration;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FeignConfiguration {

    private final String token;

    public FeignConfiguration(@Value("${providers.whatsapp.token}") String token) {
        this.token = token;
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        log.debug(this.token);
        return requestTemplate -> {
            requestTemplate.header("Content-Type", "application/json");
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("Authorization", "Bearer " + this.token);
        };
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
