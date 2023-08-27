package com.cs.ge.entites;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Table {
    @JsonProperty(access = WRITE_ONLY)
    private String id;
    private String publicId;
    private String name;
    private String position;
    private String type;
    private String slug;
    private String description;
    private Set<String> contactIds;
    private boolean active = true;
    private boolean deletable = true;
}
