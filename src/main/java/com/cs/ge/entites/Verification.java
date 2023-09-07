package com.cs.ge.entites;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.google.gson.annotations.Expose;
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
    @Expose
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime dateCreation;

    @Expose
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    private LocalDateTime dateExpiration;
    private UserAccount userAccount;

    public Verification() {
    }

    public Verification(final boolean active, final String code, final String username, final LocalDateTime dateCreation, final LocalDateTime dateExpiration, final UserAccount userAccount) {
        this.code = code;
        this.active = active;
        this.username = username;
        this.dateCreation = dateCreation;
        this.dateExpiration = dateExpiration;
        this.userAccount = userAccount;
    }
}
