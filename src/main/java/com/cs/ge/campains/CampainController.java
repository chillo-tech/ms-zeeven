package com.cs.ge.campains;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping(path = "campains", produces = "application/json")
public class CampainController {
    private final CampainService campainService;

    @GetMapping
    public List<Campain> Search() {
        return this.campainService.search();
    }

    @GetMapping(value = "/{id}/simulate")
    public void simulate(@PathVariable final String id) {
        this.campainService.simulate(id);
    }

    @DeleteMapping(value = "/{id}")
    public void delete(@PathVariable final String id) {
        this.campainService.delete(id);
    }


    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void add(@RequestBody final Campain campain) {
        this.campainService.add(campain);
    }


}
