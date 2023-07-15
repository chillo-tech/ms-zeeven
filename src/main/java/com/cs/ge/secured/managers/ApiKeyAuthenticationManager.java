package com.cs.ge.secured.managers;

import com.cs.ge.secured.providers.ApiKeyAuthenticationProvider;
import com.cs.ge.services.ProfileService;
import lombok.AllArgsConstructor;
import org.passay.Rule;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

@AllArgsConstructor
public class ApiKeyAuthenticationManager implements AuthenticationManager {


    private final List<Rule> passwordRules;
    private final ProfileService profileService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthenticationProvider apiKeyAuthenticationProvider = new ApiKeyAuthenticationProvider(this.passwordRules, this.profileService);
        return apiKeyAuthenticationProvider.authenticate(authentication);
    }
}
