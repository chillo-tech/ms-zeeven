package com.cs.ge.controllers;

import com.cs.ge.dto.Scan;
import com.cs.ge.entites.*;
import com.cs.ge.services.EventService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@RestController
@RequestMapping(path = "event", produces = "application/json")
public class EventController {
    private final EventService eventService;

    @GetMapping
    public List<Event> Search() {
        return this.eventService.search();
    }


    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void add(@RequestBody final Event event) {
        this.eventService.add(event);
    }
    
    //@PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public void delete(@PathVariable final String id) {
        this.eventService.delete(id);
    }

    @ResponseBody
    @GetMapping(value = "/{id}")
    public Event read(@PathVariable final String id) {
        return this.eventService.read(id);
    }

    @PutMapping(value = "/{id}")
    public void update(@PathVariable final String id, @RequestBody final Event event) {
        this.eventService.update(id, event);
    }

    @GetMapping(value = "{id}/guest")
    public List<Guest> fetchGuests(@PathVariable final String id) {
        return this.eventService.guests(id);
    }

    @PostMapping(value = "{id}/guest")
    public void addGuest(
            @RequestParam(required = false) final Map<String, Object> parameters,
            @PathVariable final String id, @RequestBody final Guest guest
    ) throws IOException {
        this.eventService.addGuest(id, guest, parameters);
    }

    @PostMapping(value = "{id}/guest-list")
    public void addGuests(
            @RequestParam(required = false) final Map<String, Object> parameters,
            @PathVariable final String id, @RequestBody final Set<Guest> guests
    ) throws IOException {
        this.eventService.addGuests(id, guests, parameters);
    }

    @PostMapping(value = "{id}/plan")
    public void addPlan(@PathVariable final String id, @RequestBody final Plan plan) {
        this.eventService.addPlan(id, plan);
    }

    @DeleteMapping(value = "{eventId}/guest/{guestId}")
    public void deleteGuest(@PathVariable final String eventId, @PathVariable final String guestId) {
        this.eventService.deleteGuest(eventId, guestId);
    }

    @PostMapping(value = "{id}/invitations")
    public void sendInvitations(@PathVariable final String id, @RequestBody final Set<String> guestIds) {
        this.eventService.sendInvitations(id, guestIds);
    }

    @PostMapping(value = "{id}/invitations/{invitationId}")
    public void getInvitation(@PathVariable final String id, @PathVariable final String invitationId) {
        this.eventService.getInvitation(id, invitationId);
    }


    @GetMapping(value = "{id}/table")
    public List<Table> fetchTables(@PathVariable final String id) {
        return this.eventService.tables(id);
    }

    @PostMapping(value = "{eventId}/table")
    public void addTable(@PathVariable final String eventId, @RequestBody final Table table) {
        this.eventService.addTable(eventId, table);
    }

    @DeleteMapping(value = "{eventId}/table/{tableId}")
    public void deleteTable(@PathVariable final String eventId, @PathVariable final String tableId) {
        this.eventService.deleteTable(eventId, tableId);
    }

    @DeleteMapping(value = "{eventId}/schedule/{scheduleId}")
    public void deleteSchedule(@PathVariable final String eventId, @PathVariable final String scheduleId) {
        this.eventService.deleteSchedule(eventId, scheduleId);
    }

    @PostMapping(value = "{id}/schedule")
    public void addSchedule(@PathVariable final String id, @RequestBody final Schedule schedule) {
        this.eventService.addSchedule(id, schedule);
    }

    @DeleteMapping(value = "{eventId}/scan/{scanId}")
    public void deleteScan(@PathVariable final String eventId, @PathVariable final String scanId) {
        this.eventService.deleteScan(eventId, scanId);
    }

    @PostMapping(value = "{id}/scan")
    public void addScan(@PathVariable final String id, @RequestBody final Scan scan) {
        this.eventService.addScan(id, scan);
    }

    @GetMapping(value = "{id}/schedule")
    public List<Schedule> fetchSchedules(@PathVariable final String id) {
        return this.eventService.schedules(id);
    }

    @GetMapping(value = "{id}/statistic")
    public List<Object> fetchStatistic(@PathVariable final String id) {
        return this.eventService.statistics(id);
    }

    @PostMapping(value = "{id}/invitation")
    public void addInvitation(@PathVariable final String id, @RequestBody final Invitation invitation) {
        this.eventService.addInvitation(id, invitation);
    }

    @DeleteMapping(value = "{eventId}/invitation/{invitationPublicId}")
    public void deleteInvitation(@PathVariable final String eventId, @PathVariable final String invitationPublicId) {
        this.eventService.deleteInvitation(eventId, invitationPublicId);
    }

    @GetMapping(value = "{eventId}/invitation/{invitationPublicId}/simulate")
    public void simulateInvitation(@PathVariable final String eventId, @PathVariable final String invitationPublicId) {
        this.eventService.simulate(eventId, invitationPublicId);
    }

    @PatchMapping(value = "{eventPublicId}/invitation/{invitationPublicId}/params")
    public Map<String, String> setTemplateParams(@PathVariable final String eventPublicId, @PathVariable final String invitationPublicId, @RequestBody final Map<String, String> templateParams) {
        final String image = this.eventService.setTemplateParams(eventPublicId, invitationPublicId, templateParams);
        return Map.of("image", image);
    }

    @PatchMapping(value = "{id}/params")
    public void updateParams(@PathVariable final String id, @RequestBody final Map<String, Boolean> params) {
        this.eventService.updateParams(id, params);
    }

}
