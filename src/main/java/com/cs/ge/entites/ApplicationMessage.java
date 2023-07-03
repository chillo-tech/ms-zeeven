package com.cs.ge.entites;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApplicationMessage {
    private String id;
    private String text;
    private List<String> informations;
    private List<ApplicationMessageSchedule> schedules;

}
