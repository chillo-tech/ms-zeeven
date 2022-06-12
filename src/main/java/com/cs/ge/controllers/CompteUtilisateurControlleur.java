package com.cs.ge.controllers;

import com.cs.ge.entites.JwtRequest;
import com.cs.ge.entites.JwtResponse;
import com.cs.ge.entites.Utilisateur;
import com.cs.ge.services.UtilisateursService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import java.io.IOException;


@RestController
@AllArgsConstructor
@RequestMapping(consumes = "application/json", produces = "application/json")
public class CompteUtilisateurControlleur {

    private final UtilisateursService utilisateursService;
    private final AuthenticationManager authenticationManager;

    @RequestMapping(value = "connexion", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody final JwtRequest authenticationRequest) throws Exception {
        this.authenticate(authenticationRequest.getUsername(), authenticationRequest.getPassword());
        final String token = this.utilisateursService.connexion(authenticationRequest);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    private void authenticate(final String username, final String password) throws Exception {
        try {
            this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        } catch (final DisabledException e) {
            throw new Exception("USER_DISABLED", e);
        } catch (final BadCredentialsException e) {
            throw new Exception("INVALID_CREDENTIALS", e);
        }
    }

    @PostMapping(path = "inscription")
    public void inscription(@RequestBody final Utilisateur utilisateur) throws MessagingException, IOException {
        this.utilisateursService.inscription(utilisateur);
    }

    @ResponseBody
    @GetMapping(path = "activation")
    public void activated(@RequestParam("code") final String code) {
        this.utilisateursService.activate(code);
    }
}



