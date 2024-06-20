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
public class WhatsappTemplate {
    private String name;
    //private String namespace;
    private Language language;
    private List<Component> components;
}
