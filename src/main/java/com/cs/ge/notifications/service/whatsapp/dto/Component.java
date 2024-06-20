package com.cs.ge.notifications.service.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Component {
    private String type;
    //private String text;
    private List<Parameter> parameters;

}
