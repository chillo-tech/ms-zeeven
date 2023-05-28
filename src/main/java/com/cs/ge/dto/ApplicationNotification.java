package com.cs.ge.dto;

import com.cs.ge.entites.Profile;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationNotification implements Serializable {
    @JsonProperty("application")
    String application;
    @JsonProperty("template")
    String template;
    @JsonProperty("subject")
    String subject;
    @JsonProperty("eventId")
    String eventId;
    @JsonProperty("message")
    String message;
    @JsonProperty("params")
    Map<String, String> params;

    @JsonProperty("channels")
    List<Channel> channels;
    @JsonProperty("from")
    Profile from;
    @JsonProperty("contacts")
    List<UserAccount> contacts;
}
