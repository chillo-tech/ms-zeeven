package com.cs.ge.secured.providers;

import com.cs.ge.entites.Utilisateur;
import com.cs.ge.secured.authentication.ApiKeyAuthentication;
import com.cs.ge.services.ProfileService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.UUID;

@AllArgsConstructor
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private final ProfileService profileService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthentication apiKeyAuthentication = (ApiKeyAuthentication) authentication;
        Utilisateur utilisateur = this.profileService.findByServiceKeyAndServiceId(apiKeyAuthentication.getServiceKey(), apiKeyAuthentication.getServiceId());
        final UUID applicationKey = apiKeyAuthentication.getServiceKey();
        final UUID applicationId = apiKeyAuthentication.getServiceId();
        if (utilisateur.getServiceKey().compareTo(applicationKey) == 0 && utilisateur.getServiceId().compareTo(applicationId) == 0) {
            authentication.setAuthenticated(true);
            return authentication;
        }
        throw new BadCredentialsException("BAD CREDENTIALS");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthentication.class.equals(authentication);
    }
}
