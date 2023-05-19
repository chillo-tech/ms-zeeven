package com.cs.ge.entites;

import com.cs.ge.enums.QRCodeType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private QRCodeType type;
    private String text;
    private String publicId;
    private String finalContent;
}
