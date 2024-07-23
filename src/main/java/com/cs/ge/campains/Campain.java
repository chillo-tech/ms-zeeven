package com.cs.ge.campains;

import com.cs.ge.entites.Guest;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.EventStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Document("CAMPAINS")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Campain {

    @Id
    @JsonProperty(access = WRITE_ONLY)
    private String id;
    private String publicId;
    private String authorId;
    private EventStatus status;
    private String name;
    private String message;
    private String time;
    private String date;
    private String[] informations;
    private List<Channel> channels;
    private List<Guest> contacts;
}
