package com.cs.ge.notifications.records.brevo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record Message(
        String subject,
        String htmlContent,
        Contact sender,
        Set<Contact> to,
        List<Map<String, String>> attachment
) {
}
