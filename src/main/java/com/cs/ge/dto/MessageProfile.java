package com.cs.ge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageProfile{
    private String id;
    private String civility;
    String firstName;
    String lastName;
    String email;
    String  phoneIndex;
    String phone;
    boolean trial;
}
