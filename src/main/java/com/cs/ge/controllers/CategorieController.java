package com.cs.ge.controllers;

import com.cs.ge.entites.Category;
import com.cs.ge.services.CategorieService;
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
@RequestMapping(path = "categorie", produces = "application/json")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(final CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    @PostMapping
    public void creation(@RequestBody final Category category) {
        this.categorieService.add(category);
    }

    @ResponseBody
    @GetMapping
    public List<Category> search() {
        return this.categorieService.search();
    }

    @DeleteMapping(value = "/{id}")
    public void deleteCategorie(@PathVariable final String id) {
        this.categorieService.deleteCategorie(id);
    }

    @ResponseBody
    @PutMapping(value = "/{id}")
    public void updateCategorie(@PathVariable final String id, @RequestBody final Category category) {
        this.categorieService.updateCategorie(id, category);
    }

    @GetMapping("/queryparam")
    List<Category> search(@RequestParam("id") final String id) {
        return this.categorieService.search();
    }

}


