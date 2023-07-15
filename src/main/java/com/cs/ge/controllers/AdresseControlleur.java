package com.cs.ge.controllers;


import com.cs.ge.entites.Adresse;
import com.cs.ge.services.AdresseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "adresse", produces = "application/json")
public class AdresseControlleur {

    private AdresseService adresseService;

    public AdresseControlleur(AdresseService adresseService) {
        this.adresseService = adresseService;
    }

    @PostMapping
    public void create(@RequestBody Adresse adresse) {
        this.adresseService.creation(adresse);
    }

    @ResponseBody
    @GetMapping
    public List<Adresse> search() {
        return this.adresseService.search();
    }

    @DeleteMapping(value = "/{id}")
    public void deleteAddress(@PathVariable String id) {
        this.adresseService.deleteAddress(id);
    }

    @ResponseBody
    @PutMapping(value = "/{id}")
    public void updateAddress(@PathVariable String id, @RequestBody Adresse address) {
        this.adresseService.updateAddress(id, address);

    }

    @GetMapping("/queryparam")
    List<Adresse> search(@RequestParam("ville") String ville) {
        return this.adresseService.search();
    }


}
