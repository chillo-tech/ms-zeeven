package com.cs.ge.notifications.records;

import com.cs.ge.enums.Channel;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record Notification(
        @JsonProperty("application") String application,
        @JsonProperty("template") String template,
        @JsonProperty("subject") String subject,
        @JsonProperty("eventId") String eventId,
        @JsonProperty("message") String message,
        @JsonProperty("channels") List<Channel> channels,
        @JsonProperty("from") MessageProfile from,
        @JsonProperty("contacts") List<MessageProfile> contacts,
        @JsonProperty("params") Map<String, List<Object>> params
) {
}
