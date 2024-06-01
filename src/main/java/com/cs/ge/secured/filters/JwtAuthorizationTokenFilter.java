package com.cs.ge.secured.filters;

import com.cs.ge.exception.ApplicationException;
import com.cs.ge.services.security.TokenService;
import com.google.common.base.Strings;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class JwtAuthorizationTokenFilter extends OncePerRequestFilter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final String tokenHeader;

    public JwtAuthorizationTokenFilter(final UserDetailsService userDetailsService, final TokenService tokenService, final String tokenHeader) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.tokenHeader = tokenHeader;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
        final String test = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        this.logger.info("processing authentication for '{}'", request.getRequestURL());
        this.logger.info("{}", test);


        String requestHeader = request.getHeader(this.tokenHeader);
        this.logger.info("requestHeader {} ", requestHeader);
        if (Strings.isNullOrEmpty(requestHeader)) {

            final Cookie[] cookies = request.getCookies();
            final Optional<Cookie> optionalCookie = Arrays.stream(cookies).filter(cookie -> {
                this.logger.info("Cookie {} {} ", cookie.getName(), cookie.getValue());
                return cookie.getName().equals("__Secure-next-auth.session-token");
            }).findFirst();
            if (optionalCookie.isPresent()) {
                final Cookie cookie = optionalCookie.get();
                requestHeader = String.format("Bearer %s", cookie.getValue());
            }
        }

        String username = null;
        String authToken = null;
        if (requestHeader != null && requestHeader.startsWith("Bearer ")) {
            authToken = requestHeader.substring(7);
            try {
                username = this.tokenService.getUsernameFromToken(authToken);
            } catch (final ApplicationException e) {
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
            if (this.tokenService.validateToken(authToken, userDetails)) {
                final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                this.logger.info("authorizated user '{}', setting security context", username);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }

}
