package com.cs.ge.controllers;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Plan;
import com.cs.ge.entites.Schedule;
import com.cs.ge.entites.Table;
import com.cs.ge.services.EventService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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

    @PostMapping(value = "{id}/guest")
    public void addGuests(@PathVariable final String id, @RequestBody final Guest guest) {
        this.eventService.addGuest(id, guest);
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


    @GetMapping(value = "{id}/guest")
    public List<Guest> fetchGuests(@PathVariable final String id) {
        return this.eventService.guests(id);
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

    @PostMapping(value = "{id}/schedule")
    public void addSchedule(@PathVariable final String id, @RequestBody final Schedule schedule) {
        this.eventService.addSchedule(id, schedule);
    }

    @GetMapping(value = "{id}/schedule")
    public List<Schedule> fetchSchedules(@PathVariable final String id) {
        return this.eventService.schedules(id);
    }

    @DeleteMapping(value = "{eventId}/schedule/{scheduleId}")
    public void deleteSchedule(@PathVariable final String eventId, @PathVariable final String scheduleId) {
        this.eventService.deleteSchedule(eventId, scheduleId);
    }

    @GetMapping(value = "{id}/statistic")
    public List<Object> fetchStatistic(@PathVariable final String id) {
        return this.eventService.statistics(id);
    }


}
