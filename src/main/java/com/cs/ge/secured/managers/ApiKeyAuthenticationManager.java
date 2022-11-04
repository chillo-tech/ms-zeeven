package com.cs.ge.secured.managers;

import com.cs.ge.secured.providers.ApiKeyAuthenticationProvider;
import com.cs.ge.services.ProfileService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@AllArgsConstructor
public class ApiKeyAuthenticationManager implements AuthenticationManager {


    private final ProfileService profileService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationProvider apiKeyAuthenticationProvider = new ApiKeyAuthenticationProvider(this.profileService);
        return apiKeyAuthenticationProvider.authenticate(authentication);
    }
}
