package com.cs.ge.entites;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Schedule {
    @JsonProperty(access = WRITE_ONLY)
    private String id;
    private String publicId;
    private String slug;
    private String title;
    private String note;
    private String location;
    private String end;
    private String start;
    @Expose
    private Instant date;
}
