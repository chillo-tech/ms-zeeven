package com.cs.ge.controllers;

import com.cs.ge.entites.Guest;
import com.cs.ge.entites.JwtRequest;
import com.cs.ge.entites.JwtResponse;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.GuestType;
import com.cs.ge.services.ProfileService;
import com.cs.ge.services.UtilisateursService;
import com.cs.ge.services.security.TokenService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
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
        log.info("login {} {}", authenticationRequest.getUsername(), authenticationRequest.getPassword());
        final Authentication authentication = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(), authenticationRequest.getPassword()));
        final String token = this.tokenService.generateToken(authentication);
        return ResponseEntity.ok(new JwtResponse(token));
    }

    private void authenticate(final String username, final String password) {
        this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

    @PostMapping(path = "signup", consumes = APPLICATION_JSON_VALUE)
    public void inscription(@RequestBody final UserAccount userAccount) {
        this.utilisateursService.inscription(userAccount);
    }

    @ResponseBody
    @PostMapping(path = "activation", consumes = APPLICATION_JSON_VALUE)
    public void activated(@RequestBody final Map<String, String> params) {
        this.utilisateursService.activate(params.get("code"));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(path = "reset-password-link")
    public void resetPasswordLink(@RequestBody final Map<String, String> params) {
        this.utilisateursService.resetPasswordLink(params.get("email"));
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(path = "update-password")
    public void activate(@RequestBody final Map<String, String> credentials) {
        this.utilisateursService.updatePassword(credentials.get("code"), credentials.get("password"));
    }

    @ResponseBody
    @GetMapping(path = "profile")
    public UserAccount getAuthenticateUser() {
        return this.profileService.getAuthenticateUser();
    }

    @ResponseBody
    @PatchMapping(path = "profile/{id}")
    public void getAuthenticateUser(@PathVariable final String id, @RequestBody final UserAccount userAccount) {
        this.utilisateursService.updateUtilisateur(id, userAccount);
    }

    @ResponseBody
    @GetMapping(path = "contact")
    public List<Guest> getAuthenticateUserContacts(@RequestParam(defaultValue = "LOCAL", required = false) final GuestType type) {
        return this.utilisateursService.contacts(type);
    }

    @ResponseBody
    @GetMapping(path = "authorization")
    public String getAuthorizations() {
        return this.utilisateursService.getAuthorizations();
    }

    @ResponseBody
    @PostMapping(path = "contact")
    public void addAuthenticateUserContact(@RequestBody final Guest guest) {
        this.utilisateursService.addGuest(guest);
    }

    @ResponseBody
    @PostMapping(path = "contact/list")
    public void addAuthenticateUserContacts(@RequestBody final List<Guest> guests) {
        this.utilisateursService.addGuests(guests);
    }

    @ResponseBody
    @DeleteMapping(path = "contact/{id}")
    public void deleteAuthenticateUserContacts(@PathVariable final String id) {
        this.utilisateursService.deleteContact(id);
    }
}

