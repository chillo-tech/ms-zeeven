package com.cs.ge.entites;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("EVENT_GUEST_MESSAGE")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EventGuestMessage {
    private String message;
    private String event;
    private String guest;
    private Instant send;
    private boolean isMessageSend;
}
