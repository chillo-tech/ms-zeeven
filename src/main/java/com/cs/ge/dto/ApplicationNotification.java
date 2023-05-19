package com.cs.ge.dto;

import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Profile;
import com.cs.ge.enums.Channel;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record ApplicationNotification(
        @JsonProperty("application") String application,
        @JsonProperty("template") String template,
        @JsonProperty("subject") String subject,
        @JsonProperty("eventId") String eventId,
        @JsonProperty("message") String message,
        @JsonProperty("params") Map<String, String> params,

        @JsonProperty("channels") List<Channel> channels,
        @JsonProperty("from") Profile from,
        @JsonProperty("contacts") List<Guest> contacts

) implements Serializable {
}
