package com.cs.ge.secured;

import com.cs.ge.secured.filters.JwtAuthorizationTokenFilter;
import com.cs.ge.secured.properties.RsaConfigurationProperties;
import com.cs.ge.secured.providers.ApiKeyAuthenticationProvider;
import com.cs.ge.services.ProfileService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.Rule;
import org.passay.WhitespaceRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableConfigurationProperties(RsaConfigurationProperties.class)
@EnableWebSecurity
public class ZeevenSecurityConfiguration {

    private final ProfileService profileService;
    private final RsaConfigurationProperties rsaConfigurationProperties;
    private final String tokenHeader;

    public ZeevenSecurityConfiguration(
            @Value("${jwt.header}") String tokenHeader,
            ProfileService profileService,
            RsaConfigurationProperties rsaConfigurationProperties) {
        this.profileService = profileService;
        this.rsaConfigurationProperties = rsaConfigurationProperties;
        this.tokenHeader = tokenHeader;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(new ApiKeyAuthenticationProvider(this.passwordRules(), this.profileService));
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(this.profileService);
        daoAuthenticationProvider.setPasswordEncoder(this.bCryptPasswordEncoder());
        authenticationManagerBuilder.authenticationProvider(daoAuthenticationProvider);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        final JwtAuthorizationTokenFilter authenticationTokenFilter = new JwtAuthorizationTokenFilter(this.profileService, this.jwtDecoder(), this.tokenHeader);

        return httpSecurity
                .cors(Customizer.withDefaults())
                .headers(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests(
                        auth ->
                                auth.mvcMatchers(POST, "/signin").permitAll()
                                        .mvcMatchers(POST, "/signup").permitAll()
                                        .mvcMatchers(GET, "/ticket").permitAll()
                                        .mvcMatchers("/webhooks").permitAll()
                                        .mvcMatchers(POST, "/activation").permitAll()
                                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()))
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public List<Rule> passwordRules() {
        List<Rule> rules = new ArrayList<>();
        //Rule 1: Password length should be in between
        //40 and 52 characters
        rules.add(new LengthRule(40, 52));
        //Rule 2: No whitespace allowed
        rules.add(new WhitespaceRule());
        //Rule 12.a: At least one Upper-case character
        rules.add(new CharacterRule(EnglishCharacterData.UpperCase, 14));
        //Rule 12.b: At least one Lower-case character
        rules.add(new CharacterRule(EnglishCharacterData.LowerCase, 14));
        //Rule 17.c: At least one digit
        rules.add(new CharacterRule(EnglishCharacterData.Digit, 14));

        return rules;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.rsaConfigurationProperties.getPublicKey()).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.rsaConfigurationProperties.getPublicKey()).privateKey(this.rsaConfigurationProperties.getPrivateKey()).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
}
