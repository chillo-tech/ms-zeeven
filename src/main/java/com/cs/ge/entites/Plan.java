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
public class Plan {
    protected String id;
    private String publicId;
    private Map<String, Guest> contacts;
    private Map<String, Table> tables;
    private Set<String> tablesOrder;
}
