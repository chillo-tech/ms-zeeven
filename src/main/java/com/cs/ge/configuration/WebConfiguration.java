package com.cs.ge.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
//public class WebConfiguration implements WebMvcConfigurer {
public class WebConfiguration {
    //@Override
    public static void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://app.zeeven.chillo.fr")
                .allowedMethods("*");
    }
}
