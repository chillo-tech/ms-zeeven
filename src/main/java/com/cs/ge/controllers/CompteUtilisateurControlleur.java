package com.cs.ge.controllers;

import com.cs.ge.entites.JwtRequest;
import com.cs.ge.entites.JwtResponse;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.services.UtilisateursService;
import com.cs.ge.services.scurity.TokenService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;

    @RequestMapping(value = "signin", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody final JwtRequest authenticationRequest) throws Exception {
        Authentication authentication = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword()));
        final String token = this.tokenService.generateToken(authentication);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    private void authenticate(final String username, final String password) {
        this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

    @PostMapping(path = "signup")
    public void inscription(@RequestBody final UserAccount userAccount) throws MessagingException, IOException {
        this.utilisateursService.inscription(userAccount);
    }

    @ResponseBody
    @GetMapping(path = "activation")
    public void activated(@RequestParam("code") final String code) {
        this.utilisateursService.activate(code);
    }
}



