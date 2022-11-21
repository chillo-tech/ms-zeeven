package com.cs.ge.controllers;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.services.EventService;
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
@RequestMapping(path = "evenement", produces = "application/json")
public class EvenementsControlleur {

    private final EventService evenementsService;

    public EvenementsControlleur(final EventService evenementsService) {
        this.evenementsService = evenementsService;

    }

    @GetMapping
    public List<Event> Search() {
        return this.evenementsService.search();
    }

    @PostMapping
    public void add(@RequestBody final Event evenement) {
        this.evenementsService.add(evenement);
    }

    @DeleteMapping(value = "/{id}")
    public void delete(@PathVariable final String id) {
        //this.evenementsService.deleteEvenement(id);
    }

    @PutMapping(value = "/{id}")
    public void update(@PathVariable final String id, @RequestBody final Event evenement) {
        //this.evenementsService.updateEvenement(id, evenement);
    }

    @PostMapping(value = "{id}/invite")
    public void addInvites(@PathVariable final String id, @RequestBody final Guest guest) {
        // this.evenementsService.addInvites(id, guest);
    }
}
