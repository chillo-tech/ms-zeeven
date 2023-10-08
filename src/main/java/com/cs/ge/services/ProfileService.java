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

    public UserAccount findBySecretsServiceId(final String serviceId) throws UsernameNotFoundException {
        return this.utilisateurRepository.findBySecretsServiceId(serviceId).orElseThrow(() -> new UsernameNotFoundException("User not found with serviceId: " + serviceId));
    }

    public UserAccount findById(final String utilisateurId) throws UsernameNotFoundException {
        return this.utilisateurRepository.findById(utilisateurId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserAccount getAuthenticateUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return this.loadUser(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + authentication.getName()));
    }

    public Optional<UserAccount> loadUser(final String username) {
        Optional<UserAccount> optionalProfile = Optional.empty();
        if (username != null && username.indexOf('@') > -1) {
            optionalProfile = this.utilisateurRepository.findByEmail(username);
        }

        if (optionalProfile.isEmpty()) {
            optionalProfile = this.utilisateurRepository.findByPhone(username);
        }
        return optionalProfile;
    }
}


