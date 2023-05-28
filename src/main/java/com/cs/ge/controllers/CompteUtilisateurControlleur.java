package com.cs.ge.controllers;

import com.cs.ge.entites.JwtRequest;
import com.cs.ge.entites.JwtResponse;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.services.ProfileService;
import com.cs.ge.services.UtilisateursService;
import com.cs.ge.services.security.TokenService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@AllArgsConstructor
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class CompteUtilisateurControlleur {

    private final ProfileService profileService;
    private final UtilisateursService utilisateursService;
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    @PostMapping(value = "signin", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody final JwtRequest authenticationRequest) throws Exception {
        Authentication authentication = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword()));
        final String token = this.tokenService.generateToken(authentication);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    private void authenticate(final String username, final String password) {
        this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

    @PostMapping(path = "signup", consumes = APPLICATION_JSON_VALUE)
    public void inscription(@RequestBody final UserAccount userAccount) throws MessagingException, IOException {
        this.utilisateursService.inscription(userAccount);
    }

    @ResponseBody
    @PostMapping(path = "activation", consumes = APPLICATION_JSON_VALUE)
    public void activated(@RequestBody final Map<String, String> params) {
        this.utilisateursService.activate(params.get("code"));
    }

    @ResponseBody
    @GetMapping(path = "profile")
    public UserAccount getAuthenticateUser() {
        return this.profileService.getAuthenticateUser();
    }

    @ResponseBody
    @PatchMapping(path = "profile/{id}")
    public void getAuthenticateUser(@PathVariable String id, @RequestBody final UserAccount userAccount) {
        this.utilisateursService.updateUtilisateur(id, userAccount);
    }
}

