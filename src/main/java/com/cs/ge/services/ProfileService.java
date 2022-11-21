package com.cs.ge.services;

import com.cs.ge.entites.UserAccount;
import com.cs.ge.repositories.UtilisateurRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@AllArgsConstructor
@Service
public class ProfileService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return this.loadUser(username).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public UserAccount findBySecretsServiceId(String serviceId) throws UsernameNotFoundException {
        return this.utilisateurRepository.findBySecretsServiceId(serviceId).orElseThrow(() -> new UsernameNotFoundException("User not found with serviceId: " + serviceId));
    }

    public static UserAccount getAuthenticateUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserAccount) authentication.getPrincipal();
    }

    private Optional<UserAccount> loadUser(final String username) {

        Optional<UserAccount> optionalProfile = Optional.empty();
        if (username != null && username.indexOf('@') > -1) {
            optionalProfile = this.utilisateurRepository.findByEmail(username);
        }
        /*
        if (optionalProfile.isEmpty() && authenticationRequest.getPhoneIndex() != null && authenticationRequest.getPhone() != null) {
            optionalProfile = this.utilisateurRepository.findByPhoneIndexAndPhone(authenticationRequest.getPhone());
        }*/
        return optionalProfile;
    }
}


