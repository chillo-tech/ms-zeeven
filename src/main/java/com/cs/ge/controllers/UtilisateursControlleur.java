package com.cs.ge.controllers;

import com.cs.ge.entites.UserAccount;
import com.cs.ge.services.UtilisateursService;
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
@RequestMapping(path = "utilisateurs", produces = "application/json")

public class UtilisateursControlleur {

    private final UtilisateursService utilisateursService;

    public UtilisateursControlleur(UtilisateursService utilisateursService) {
        this.utilisateursService = utilisateursService;
    }

    @PostMapping
    public void creation(@RequestBody UserAccount userAccount) {
        this.utilisateursService.add(userAccount);
    }

    @ResponseBody
    @GetMapping
    public List<UserAccount> search() {
        return this.utilisateursService.search();
    }

    //@GetMapping(path ="confirm")
    // public  void confirm(@RequestParam("code")  String code){
    //utilisateursService.confirmCode(code);
    // }
    @DeleteMapping(value = "/{id}")
    public void deleteUtilisateur(@PathVariable String id) {
        this.utilisateursService.deleteUtilisateur(id);
    }


    @ResponseBody
    @PutMapping(value = "/{id}")
    public void updateUtilisateur(@PathVariable String id, @RequestBody UserAccount userAccount) {
        this.utilisateursService.updateUtilisateur(id, userAccount);
    }

    @GetMapping("/queryparam")
    List<UserAccount> search(@RequestParam("username") String username) {
        return this.utilisateursService.search();
    }
}


