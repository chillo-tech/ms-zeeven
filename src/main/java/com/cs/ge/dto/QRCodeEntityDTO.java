package com.cs.ge.dto;

import com.cs.ge.enums.QRCodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class QRCodeEntityDTO {
    private String id;
    private boolean track;
    private boolean enabled;
    private QRCodeType type;
    private Map<String, String> data;
    private Map<String, String> attributes;
    private String publicId;
    private String author;
    private String name;
    private String file;
    private String finalContent;
    private String tempContent;
    private String location;
    private String user;
    private Instant creation;
    private Instant update;

}
