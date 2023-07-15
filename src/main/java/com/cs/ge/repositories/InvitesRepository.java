package com.cs.ge.repositories;

import com.cs.ge.entites.Guest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InvitesRepository extends MongoRepository<Guest, String> {

}

