package com.cs.ge.entites;

import com.cs.ge.enums.Channel;
import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Invitation {
    private String id;
    private String publicId;
    private List<Channel> channels;
    private Template template;
    private boolean isSent;
    private boolean hasParameters = false;
    @Expose
    private Instant send;
    @Expose
    private Instant active;
}
