package com.cs.ge.notifications.entity;

import com.cs.ge.enums.Channel;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Builder
@Getter
@Setter
@AllArgsConstructor
@Document(collection = "NOTIFICATION")
public class Notification {
    @Id
    private String id;
    private String message;
    private String image;
    private String subject;
    private String date;
    private String eventId;
    private String applicationMessageId;
    private String application;
    private Set<Channel> channels;
    private String template;
    private Sender from;
    private Set<Recipient> contacts;
    private Channel type;
    private Set<Recipient> cc;
    private Set<Recipient> cci;
    private Map<String, List<Object>> params;
    @JsonFormat(without = JsonFormat.Feature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
    private Instant creation;

    public Notification() {
        this.cc = new HashSet<>();
        this.cci = new HashSet<>();
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Instant getCreation() {
        return this.creation != null ? this.creation : null;
    }


}
