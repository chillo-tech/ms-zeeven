package com.cs.ge.repositories;

import com.cs.ge.entites.EventMessage;
import com.cs.ge.enums.Channel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.stream.Stream;

public interface EventMessageRepository extends MongoRepository<EventMessage, String> {

    @Query(
            """
                        FROM EventMessage em 
                        WHERE em.channel = :channel 
                        AND em.eventId = :eventId 
                        AND em.isHandled = false
                        AND em.handle = null
                    """
    )
    Stream<EventMessage> findMessagesToSend(Channel channel, String eventId);

    void deleteByEventId(String id);
}
