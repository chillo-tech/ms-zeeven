package com.cs.ge.entites;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMessageSchedule {
    private LocalDateTime date;
    private String timezone;
    private boolean handled;
    private LocalDateTime handledDate;
}
