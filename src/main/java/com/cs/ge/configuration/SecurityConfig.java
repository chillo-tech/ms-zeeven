package com.cs.ge.configuration;

import com.cs.ge.security.APIAuthenticationProvider;
import com.cs.ge.security.JwtAuthenticationEntryPoint;
import com.cs.ge.security.JwtAuthorizationTokenFilter;
import com.cs.ge.security.JwtTokenUtil;
import com.cs.ge.services.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Slf4j
//@Configuration
//@EnableWebSecurity
public class SecurityConfig {

    private final String tokenHeader;
    private final JwtTokenUtil jwtTokenUtil;
    private final ProfileService profileService;
    private final APIAuthenticationProvider apiAuthenticationProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(
            @Value("${jwt.header}") final String tokenHeader,
            final JwtTokenUtil jwtTokenUtil,
            final ProfileService profileService,
            final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            final APIAuthenticationProvider apiAuthenticationProvider
    ) {
        this.tokenHeader = tokenHeader;
        this.jwtTokenUtil = jwtTokenUtil;
        this.profileService = profileService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.apiAuthenticationProvider = apiAuthenticationProvider;
    }

    protected void configure(final HttpSecurity httpSecurity) throws Exception {

        httpSecurity.cors().and().csrf().disable();
        httpSecurity.exceptionHandling().authenticationEntryPoint(this.jwtAuthenticationEntryPoint).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                .antMatchers(POST, "/connexion").permitAll()
                .antMatchers(GET, "/ticket").permitAll()
                .antMatchers("/webhooks").permitAll()
                .antMatchers(POST, "/inscription").permitAll()
                .antMatchers(POST, "/activation").permitAll()
                .anyRequest()
                .authenticated()
                .and().httpBasic();

        httpSecurity.headers().httpStrictTransportSecurity().disable();
        // Custom JWT based security filter
        final JwtAuthorizationTokenFilter authenticationTokenFilter = new JwtAuthorizationTokenFilter(this.profileService, this.jwtTokenUtil, this.tokenHeader);
        httpSecurity
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity
                .headers()
                .frameOptions().sameOrigin()  // required to set for H2 else H2 Console will be blank.
                .cacheControl();
    }

    protected void configure(final AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(this.daoAuthenticationProvider());
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        final DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(this.bCryptPasswordEncoder());
        provider.setUserDetailsService(this.profileService);
        return provider;
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(this.apiAuthenticationProvider);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public static WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedHeaders("*")
                        .allowedMethods("*")
                        .allowedOrigins("*");
            }
        };
    }

}
