package com.cs.ge.secured;

import com.cs.ge.secured.filters.ApiKeyFilter;
import com.cs.ge.services.ProfileService;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.LengthRule;
import org.passay.Rule;
import org.passay.WhitespaceRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebSecurity
public class ZeevenSecurityConfiguration {

    private final ProfileService profileService;

    public ZeevenSecurityConfiguration(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .cors(Customizer.withDefaults())
                .headers(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(new ApiKeyFilter(this.passwordRules(), this.profileService), UsernamePasswordAuthenticationFilter.class)
                .authorizeRequests(
                        auth ->
                                auth.antMatchers(POST, "/connexion").permitAll()
                                        .antMatchers(GET, "/ticket").permitAll()
                                        .antMatchers("/webhooks").permitAll()
                                        .antMatchers(POST, "/inscription").permitAll()
                                        .antMatchers(POST, "/activation").permitAll()
                                        .anyRequest().authenticated()
                )
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
}
