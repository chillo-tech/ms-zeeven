package com.cs.ge.controllers;

import com.cs.ge.entites.Guest;
import com.cs.ge.services.InvitesService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "invite", produces = "Application/json")
public class InvitesControlleur {

    private final InvitesService invitesService;

    public InvitesControlleur(final InvitesService invitesService) {
        this.invitesService = invitesService;

    }

    @GetMapping
    public List<Guest> Search() {
        return this.invitesService.Search();
    }

    @PostMapping
    public void add(@RequestBody final Guest guest) {
		this.invitesService.add(guest);
    }

    @DeleteMapping(value = "/{id}")
    public void delete(@PathVariable final String id) {
		this.invitesService.deleteInvite(id);
    }

    @PutMapping(value = "/{id}")
    public void Update(@PathVariable final String id, @RequestBody final Guest guest) {
		this.invitesService.UpdateInvite(id, guest);
    }
}


