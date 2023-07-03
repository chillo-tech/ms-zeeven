package com.cs.ge.secured.filters;

import com.cs.ge.secured.authentication.ApiKeyAuthentication;
import com.cs.ge.secured.managers.ApiKeyAuthenticationManager;
import com.cs.ge.services.ProfileService;
import jakarta.servlet.FilterChain;
import lombok.AllArgsConstructor;
import org.passay.Rule;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@AllArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private final List<Rule> passwordRules;
    private final ProfileService profileService;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {
        final String auth = request.getHeader("Authorization");
        final String serviceKey = request.getHeader("service-key");
        final String serviceId = request.getHeader("service-id");
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            final ApiKeyAuthenticationManager apiKeyAuthenticationManager = new ApiKeyAuthenticationManager(this.passwordRules, this.profileService);
            final ApiKeyAuthentication authentication = new ApiKeyAuthentication(serviceKey, serviceId);
            final Authentication authenticate = apiKeyAuthenticationManager.authenticate(authentication);
            if (authenticate.isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
