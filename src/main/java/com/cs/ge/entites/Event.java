package com.cs.ge.entites;

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
    private List<ApplicationMessage> messages = new ArrayList<>();
    private EventStatus status;
    private List<Guest> guests;
    private List<Table> tables = new ArrayList<>();
    private List<Channel> channels = new ArrayList<>();
    private UserAccount author;
    private Category category;
    private List<Profile> contacts = new ArrayList<>();
    private List<Schedule> schedules = new ArrayList<>();
    private String slug;
    private Plan plan;
}
