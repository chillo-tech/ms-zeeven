package com.cs.ge.entites;

import com.cs.ge.enums.Channel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Document("EVENEMENT_MESSAGE")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class EventMessage {
    @Id
    @JsonProperty(access = WRITE_ONLY)
    private String id;
    private String publicId;
    private String authorId;
    private String eventId;
    private Channel channel;
    private Guest guest;
    private boolean isHandled;
    private BaseApplicationMessage message;
    private ApplicationMessageSchedule schedule;
    private Instant creation;
    private Instant handle;
    private List<EventMessageNotificationStatus> status;
}
