package com.cs.ge.entites;

import com.cs.ge.enums.QRCodeType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Document("QR_CODE")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class QRCodeEntity {
    @Id
    @JsonProperty(access = WRITE_ONLY)
    private String id;
    private boolean enabled = true;
    private QRCodeType type;
    private Map<String, String> data;
    private Map<String, String> attributes;
    private String publicId;
    private String name;
    private String finalContent;
    private String tempContent;
    private String user;
    private LocalDateTime creation = LocalDateTime.now();
    private LocalDateTime update;
}
