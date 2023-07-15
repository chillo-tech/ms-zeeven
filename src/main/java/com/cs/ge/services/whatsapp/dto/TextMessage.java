package com.cs.ge.services.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TextMessage {
    private String messaging_product;
    private String recipient_type;
    private String to;
    private String type;
    private Text text;
    private Template template;
}
