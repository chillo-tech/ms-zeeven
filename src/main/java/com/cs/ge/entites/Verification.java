package com.cs.ge.entites;

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
    private String username;
    private LocalDateTime dateCreation;
    private LocalDateTime dateExpiration;
    private UserAccount userAccount;

    public Verification() {
    }

    public Verification(String code, String username, LocalDateTime dateCreation, LocalDateTime dateExpiration, UserAccount userAccount) {
        this.code = code;
        this.username = username;
        this.dateCreation = dateCreation;
        this.dateExpiration = dateExpiration;
        this.userAccount = userAccount;
    }
}
