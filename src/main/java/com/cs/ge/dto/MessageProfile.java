package com.cs.ge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageProfile {
    private String id;
    private String civility;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneIndex;
    private String phone;
    private boolean trial;
    private List<ProfileParams> others = new ArrayList<>();
}
