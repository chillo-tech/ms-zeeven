package com.cs.ge.repositories;

import com.cs.ge.entites.EventMessage;
import com.cs.ge.enums.Channel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.stream.Stream;

public interface EventMessageRepository extends MongoRepository<EventMessage, String> {
    Stream<EventMessage> findByChannelAndEventId(Channel channel, String eventId);

    void deleteByEventId(String id);
}
