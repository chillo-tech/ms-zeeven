package com.cs.ge.entites;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Guest {
    private Profile profile;
    private String classe;
    private String position;
    private String ticketType;
    private String isTicketSent;

}
