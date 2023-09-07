package com.cs.ge.entites;

import com.cs.ge.enums.QRCodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("QR_CODE")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class QRCodeEntity {
    @Id
    private String id;
    private boolean track = false;
    private boolean enabled = true;
    private QRCodeType type;
    private Map<String, String> data;
    private Map<String, String> attributes;
    private QRCodeParams params;
    private String publicId;
    private String author;
    private long scans;
    private String name;
    private String file;
    private String finalContent;
    private String tempContent;
    private String location;
    private String user;
    private Instant creation = Instant.now();
    private Instant update;

}
