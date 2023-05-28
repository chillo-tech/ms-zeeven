package com.cs.ge.entites;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("VERIFICATION")
@AllArgsConstructor
@Getter
@Setter
public class Verification {
    @Id
    private String id;
    private String code;
    private boolean active;
    private String username;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime dateCreation;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime dateExpiration;
    private UserAccount userAccount;

    public Verification() {
    }

    public Verification(boolean active, String code, String username, LocalDateTime dateCreation, LocalDateTime dateExpiration, UserAccount userAccount) {
        this.code = code;
        this.active = active;
        this.username = username;
        this.dateCreation = dateCreation;
        this.dateExpiration = dateExpiration;
        this.userAccount = userAccount;
    }
}
