package com.cs.ge.repositories;

import com.cs.ge.entites.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.stream.Stream;

public interface EventRepository extends MongoRepository<Event, String> {
    Optional<Event> findByPublicId(String id);

    Stream<Event> findByAuthorId(String id);
}
