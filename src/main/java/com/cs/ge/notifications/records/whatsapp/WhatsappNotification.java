package com.cs.ge.notifications.records.whatsapp;


import java.util.List;

public record WhatsappNotification(
        String object,
        List<WhatsappEntry> entry
) {
}
