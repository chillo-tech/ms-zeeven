package com.cs.ge.services;

import com.cs.ge.entites.Utilisateur;
import com.cs.ge.repositories.UtilisateurRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ProfileService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return this.utilisateurRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public static Utilisateur getAuthenticateUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Utilisateur) authentication.getPrincipal();
    }

}
