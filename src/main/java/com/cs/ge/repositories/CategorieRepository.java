package com.cs.ge.repositories;

import com.cs.ge.entites.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CategorieRepository extends MongoRepository<Category, String> {

    Optional<Category> findByIdOrLabel(String id, String libelle);
}
