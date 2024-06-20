package com.cs.ge.notifications.records.whatsapp;

record WhatsappChangeValueMessageText(
        String body
) {

}

public record WhatsappChangeValueMessage(
        String from,
        String timestamp,
        String type,
        String id,
        WhatsappChangeValueMessageText text
) {
}
