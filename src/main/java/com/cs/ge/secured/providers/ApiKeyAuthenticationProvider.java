package com.cs.ge.secured.providers;

import com.cs.ge.entites.UserAccount;
import com.cs.ge.secured.authentication.ApiKeyAuthentication;
import com.cs.ge.services.ProfileService;
import lombok.AllArgsConstructor;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.passay.RuleResult;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

@AllArgsConstructor
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {
    private final List<Rule> passwordRules;
    private final ProfileService profileService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthentication apiKeyAuthentication = (ApiKeyAuthentication) authentication;
        UserAccount userAccount = this.profileService.findBySecretsServiceId(apiKeyAuthentication.getServiceId());
        final String applicationKey = apiKeyAuthentication.getServiceKey();
        PasswordValidator passwordValidator = new PasswordValidator(this.passwordRules);
        RuleResult result = passwordValidator.validate(new PasswordData(applicationKey));
        boolean isValidPassword = result.isValid();
        if (isValidPassword && userAccount.getSecrets().getServiceKey().equals(applicationKey)) {
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
