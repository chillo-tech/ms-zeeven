package com.cs.ge.entites;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Document("EVENT_GUEST")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EventGuest {
    private String type;
    private String position;
    private String event;
    private String guest;
    @JsonProperty(access = WRITE_ONLY)
    private String ticket;
    private String ticketType;
    private boolean isTicketSent;
    private boolean isGuestChecked;
}
