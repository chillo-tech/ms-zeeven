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
    private boolean table = false;
    private boolean schedule = false;
    private boolean invitation = false;
    private boolean contact = true;
}
