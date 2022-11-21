package com.cs.ge.repositories;

import com.cs.ge.entites.Event;
import com.cs.ge.enums.EventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Optional;
import java.util.stream.Stream;

public interface EventRepository extends MongoRepository<Event, String> {
    Optional<Event> findByPublicId(String id);

    Stream<Event> findByAuthorId(String id);

    @Query("{status: ?0}")
    Stream<Event> queryEvents(EventStatus status);
}
