package com.cs.ge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Scan {
    private String publicId;
    private Instant date;
    private String deventPublicId;
    private String dguestPublicId;
    private String invitationId;
    private String dtablePublicId;
}
