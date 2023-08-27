package com.cs.ge.entites;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Template {
    private String id;
    private String publicId;
    private String title;
    private String name;
    private String address;
    private String text;
    private Map<String, String> params;
    private Set<Schedule> schedules;
}
