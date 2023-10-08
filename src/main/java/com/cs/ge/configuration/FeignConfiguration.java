package com.cs.ge.configuration;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ImportAutoConfiguration({FeignAutoConfiguration.class})
@Component
public class FeignConfiguration implements RequestInterceptor {

    private final String token;
    private final String backofficeToken;
    private final String maxMindAccountId;
    private final String maxMindLicenceKey;

    public FeignConfiguration(
            @Value("${providers.whatsapp.token:''}") final String token,
            @Value("${providers.backoffice.token:''}") final String backofficeToken,
            @Value("${providers.maxmind.accountId:''}") final String maxMindAccountId,
            @Value("${providers.maxmind.licenceKey:''}") final String maxMindLicenceKey
    ) {
        this.token = token;
        this.backofficeToken = backofficeToken;
        this.maxMindLicenceKey = maxMindLicenceKey;
        this.maxMindAccountId = maxMindAccountId;
    }

    @Override
    public void apply(final RequestTemplate requestTemplate) {
        requestTemplate.header("content-type", APPLICATION_JSON_VALUE);
        requestTemplate.header("produces", APPLICATION_JSON_VALUE);
        if (requestTemplate.feignTarget().name().equalsIgnoreCase("backoffice")) {
            requestTemplate.header("Authorization", "Bearer " + this.backofficeToken);
        } else if (requestTemplate.feignTarget().name().equalsIgnoreCase("FeignMaxMindIPGeolocation")) {
            final String valueToEncode = this.maxMindAccountId + ":" + this.maxMindLicenceKey;
            final byte[] encodedAuth = Base64.encodeBase64(valueToEncode.getBytes(Charset.forName("US-ASCII")));
            final String authHeader = "Basic " + new String(encodedAuth);
            requestTemplate.header("Authorization", authHeader);
        } else {
            requestTemplate.header("Authorization", "Bearer " + this.token);
        }
    }
}

