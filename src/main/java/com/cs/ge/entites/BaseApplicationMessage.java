package com.cs.ge.entites;

import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BaseApplicationMessage {
    protected String id;
    protected String text;
    protected List<String> informations;
}
