package com.cs.ge.notifications.records.whatsapp;

import java.util.List;

public record WhatsappChangeValue(
        String messaging_product,
        WhatsappChangeValueMetadata metadata,
        List<WhatsappChangeValueStatus> statuses,
        List<WhatsappChangeValueContact> contacts,
        List<WhatsappChangeValueMessage> messages

) {
}
