package com.cs.ge.entites;

import com.cs.ge.enums.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Invitation {
    private String id;
    private String publicId;
    private Channel[] channels;
    private Template template;
    private Instant send;
    private Instant active;
}
