package com.cs.ge.entites;

import com.cs.ge.dto.Scan;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.EventStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Document("EVENEMENTS")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Event {
    @Id
    @JsonProperty(access = WRITE_ONLY)
    private String id;
    private String publicId;
    private String name;
    private EventStatus status;
    //private UserAccount author;
    private String authorId;
    private String whatsapp_template;
    private Category category;
    private String slug;
    private Plan plan;
    private Invitation invitation;
    private List<Guest> guests = new ArrayList<>();
    private List<Scan> scans = new ArrayList<>();
    private List<Channel> channels = new ArrayList<>();
    private List<ApplicationMessage> messages = new ArrayList<>();
    private List<Table> tables = new ArrayList<>();
    private List<Schedule> schedules = new ArrayList<>();
    private EventParams params = new EventParams();

}
