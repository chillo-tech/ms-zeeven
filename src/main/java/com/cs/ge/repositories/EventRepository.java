package com.cs.ge.repositories;

import com.cs.ge.entites.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface EventRepository extends MongoRepository<Event, String> {
    Optional<Event> findByPublicId(String id);
}
