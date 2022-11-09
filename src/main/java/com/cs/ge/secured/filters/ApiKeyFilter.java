package com.cs.ge.secured.filters;

import com.cs.ge.secured.authentication.ApiKeyAuthentication;
import com.cs.ge.secured.managers.ApiKeyAuthenticationManager;
import com.cs.ge.services.ProfileService;
import lombok.AllArgsConstructor;
import org.passay.Rule;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@AllArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private final List<Rule> passwordRules;
    private final ProfileService profileService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        String serviceKey = request.getHeader("service-key");
        String serviceId = request.getHeader("service-id");
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            ApiKeyAuthenticationManager apiKeyAuthenticationManager = new ApiKeyAuthenticationManager(this.passwordRules, this.profileService);
            ApiKeyAuthentication authentication = new ApiKeyAuthentication(serviceKey, serviceId);
            final Authentication authenticate = apiKeyAuthenticationManager.authenticate(authentication);
            if (authenticate.isAuthenticated()) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }
}
