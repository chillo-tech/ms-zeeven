package com.cs.ge.secured.filters;


import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.stream.Collectors;

public class JwtAuthorizationTokenFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserDetailsService userDetailsService;
    private final JwtDecoder jwtDecoder;
    private final String tokenHeader;

    public JwtAuthorizationTokenFilter(final UserDetailsService userDetailsService, JwtDecoder jwtDecoder, final String tokenHeader) {
        this.userDetailsService = userDetailsService;
        this.jwtDecoder = jwtDecoder;
        this.tokenHeader = tokenHeader;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
        String test = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        this.logger.debug("processing authentication for '{}'", request.getRequestURL());
        this.logger.debug("{}", test);

        final String requestHeader = request.getHeader(this.tokenHeader);

        String username = null;
        String authToken = null;
        if (requestHeader != null && requestHeader.startsWith("Bearer ")) {
            authToken = requestHeader.substring(7);
            try {
                username = this.getUsernameFromToken(authToken);
            } catch (final IllegalArgumentException e) {
                this.logger.error("an error occured during getting username from token", e);
            } catch (final ExpiredJwtException e) {
                this.logger.warn("the token is expired and not valid anymore", e);
            }
        } else {
            this.logger.warn("couldn't find bearer string, will ignore the header");
        }

        this.logger.debug("checking authentication for user '{}'", username);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            this.logger.debug("security context was null, so authorizating user");

            // It is not compelling necessary to load the use details from the database. You could also store the information
            // in the token and read it from it. It's up to you ;)
            final UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // For simple validation it is completely sufficient to just check the token integrity. You don't have to call
            // the database compellingly. Again it's up to you ;)
            if (this.validateToken(authToken, userDetails)) {
                final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                this.logger.info("authorizated user '{}', setting security context", username);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }

    public String getUsernameFromToken(String token) {
        return this.jwtDecoder.decode(token).getClaim("email");
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = this.getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !this.isTokenExpired(token));
    }

    private Boolean isTokenExpired(String token) {
        final Instant expiration = this.getExpirationDateFromToken(token);
        return expiration.isBefore(Instant.now());
    }

    public Instant getExpirationDateFromToken(String token) {
        return this.jwtDecoder.decode(token).getExpiresAt();
    }
}
