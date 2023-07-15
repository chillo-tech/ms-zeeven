package com.cs.ge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Mail {

    private String mailFrom;
    private String mailTo;
    private String subject;
    private Map<String, Object> props;


}
