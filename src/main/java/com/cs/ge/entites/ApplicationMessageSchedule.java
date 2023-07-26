package com.cs.ge.entites;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMessageSchedule {
    private String timezone;
    private boolean handled;

    private Instant handledDate;
    private Instant date;
}
