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
    private boolean table = true;
    private boolean schedule = true;
    private boolean invitation = true;
    private boolean contact = true;
}
