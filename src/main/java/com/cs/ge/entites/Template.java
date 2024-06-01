package com.cs.ge.entites;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Template {
    private String id;
    private String publicId;
    private String title = "";
    private String name = "";
    private int width = 0;
    private int height = 0;
    @JsonProperty(access = WRITE_ONLY)
    private String file = "";
    private String address;
    private String text;
    private Map<String, String> params;
    private Set<Schedule> schedules;
}
