package com.cs.ge.controllers;

import com.cs.ge.dto.ProfileDTO;
import com.cs.ge.services.administration.AdministrationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping
public class AdministrationController {

    private final AdministrationService administrationService;

    public AdministrationController(final AdministrationService administrationService) {
        this.administrationService = administrationService;
    }

    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    @PostMapping(path = "user-role", consumes = APPLICATION_JSON_VALUE)
    public void updateUserRole(@RequestBody final ProfileDTO profile) {
        this.administrationService.updateUserRole(profile);
    }

    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    @PostMapping(path = "user-stock", consumes = APPLICATION_JSON_VALUE)
    public void updateUserStock(@RequestBody final ProfileDTO profile) {
        this.administrationService.updateUserStock(profile);
    }

    @PreAuthorize("hasAuthority('SCOPE_ROLE_ADMIN')")
    @PostMapping(path = "event-parameters", consumes = APPLICATION_JSON_VALUE)
    public void updateEventParameters(@RequestBody final Map<String, String> parameters) {
        this.administrationService.updateEventParameters(parameters);
    }
}
