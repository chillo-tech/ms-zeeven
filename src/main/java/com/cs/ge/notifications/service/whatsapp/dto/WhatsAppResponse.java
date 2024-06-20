package com.cs.ge.notifications.service.whatsapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WhatsAppResponse {
    String id;
    String status;
    String category;
    String messaging_product;
    List<WhatsappContact> contacts;
    List<WhatsAppResponseMessage> messages;
}
