package com.cs.ge.entites;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Guest {
    private Profile profile;
    private String classe;
    private String position;
    private String ticketType;
    private boolean sendInvitation;
    private String isTicketSent;
    @JsonProperty(access = WRITE_ONLY)
    private String ticket;

}
