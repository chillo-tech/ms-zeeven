package com.cs.ge.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.stereotype.Component;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ImportAutoConfiguration({FeignAutoConfiguration.class})
@Component
public class FeignConfiguration implements RequestInterceptor {

    private final String token;
    private final String backofficeToken;

    public FeignConfiguration(
            @Value("${providers.whatsapp.token:''}") final String token,
            @Value("${providers.backoffice.token:''}") final String backofficeToken
    ) {
        this.token = token;
        this.backofficeToken = backofficeToken;
    }

    @Override
    public void apply(final RequestTemplate requestTemplate) {
        requestTemplate.header("content-type", APPLICATION_JSON_VALUE);
        requestTemplate.header("produces", APPLICATION_JSON_VALUE);
        if (requestTemplate.feignTarget().name().equalsIgnoreCase("backoffice")) {
            requestTemplate.header("Authorization", "Bearer " + this.backofficeToken);
        } else {
            requestTemplate.header("Authorization", "Bearer " + this.token);
        }
    }
}

