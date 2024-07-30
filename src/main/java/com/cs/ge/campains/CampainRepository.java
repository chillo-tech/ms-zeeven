package com.cs.ge.campains;

import com.cs.ge.enums.EventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface CampainRepository extends MongoRepository<Campain, String> {

    Stream<Campain> findByStatusIn(List<EventStatus> status);

    Optional<Campain> findByPublicId(String id);

    void deleteByPublicId(String id);


}

