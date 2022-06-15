package com.cs.ge;

import io.mongock.runner.springboot.EnableMongock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@EnableMongock
@SpringBootApplication
public class MsZeevenApplication {

    @Autowired
    private Environment env;

    public static void main(final String[] args) {
        SpringApplication.run(MsZeevenApplication.class, args);
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                String urls = MsZeevenApplication.this.env.getProperty("cors.urls");
                CorsRegistration reg = registry.addMapping("/api/**");
                for (String url : urls.split(",")) {
                    log.warn("######################################################");
                    log.warn(url);
                    log.warn("######################################################");
                    reg.allowedOrigins(url);
                }
            }
        };
    }
}
