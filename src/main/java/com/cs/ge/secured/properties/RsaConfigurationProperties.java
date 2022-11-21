package com.cs.ge.secured.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rsa")
public class RsaConfigurationProperties {
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
}
