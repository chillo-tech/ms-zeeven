package com.cs.ge.repositories;

import com.cs.ge.entites.Utilisateur;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;

public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {
    @Query("{$or : [{phone: ?0}, {email : ?0}]}")
    Optional<Utilisateur> findByUsername(String username);
    
    Optional<Utilisateur> findBySecretsServiceId(String serviceId);
}


