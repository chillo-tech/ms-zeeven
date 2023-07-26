package com.cs.ge.services;

import com.cs.ge.entites.Category;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.repositories.CategorieRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public CategorieService(final CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
    }


    public void add(final Category category) { // en entrée je dois avoir quelque chose sous la forme d'un UserAccount de type utilisateur
        String name = category.getLabel();
        name = name.toUpperCase();
        category.setLabel(name);
        this.categorieRepository.save(category);
    }

    public List<Category> search() {
        return this.categorieRepository.findAll();
    }

    public void deleteCategorie(final String id) {
        this.categorieRepository.deleteById(id);
    }

    public Category read(final String query) {
        return this.categorieRepository.findByIdOrLabel(query, query).orElseThrow(() -> new ApplicationException("Category not found with id or label: " + query));
    }

    public void updateCategorie(final String id, final Category category) {
        final Optional<Category> current = this.categorieRepository.findById(id);
        if (current.isPresent()) {
            final Category foundCategory = current.get();
            foundCategory.setId(id);
            foundCategory.setLabel(category.getLabel());
            foundCategory.setDescription(category.getDescription());
            foundCategory.setDescription(category.getDescription());
            this.categorieRepository.save(foundCategory);
        }
    }
}
