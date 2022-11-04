package com.cs.ge.repositories;

import com.cs.ge.entites.Categorie;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CategorieRepository extends MongoRepository<Categorie, String> {

    Optional<Categorie> findByIdOrLibelle(String id, String libelle);
}
