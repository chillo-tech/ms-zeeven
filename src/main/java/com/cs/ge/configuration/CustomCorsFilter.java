package com.cs.ge.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
public class CustomCorsFilter extends OncePerRequestFilter {
    private final CorsProcessor processor = new DefaultCorsProcessor();


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {


        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);

        log.info("############################################################");
        log.info("############################################################");
        config.setAllowedOrigins(List.of("*"));

        log.info("############################################################");
        log.info("############################################################");
        config.setAllowedMethods(List.of("OPTIONS", "HEAD", "GET", "POST"));

        config.addAllowedHeader("*");

        source.registerCorsConfiguration("/**", config);

        CorsConfiguration corsConfiguration = source.getCorsConfiguration(request);

        boolean isValid = this.processor.processRequest(corsConfiguration, request, response);

        if (!isValid || CorsUtils.isPreFlightRequest(request)) {

            return;

        }
        filterChain.doFilter(request, response);
    }
}
