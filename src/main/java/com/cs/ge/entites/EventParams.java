package com.cs.ge.entites;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventParams {
    private boolean hasGuests = false;
    private boolean hasTables = false;
    private boolean hasSchedule = false;
    private boolean hasInvitation = false;
    private boolean hasContacts = true;
}
