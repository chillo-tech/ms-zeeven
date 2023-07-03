package com.cs.ge.entites;

import com.cs.ge.enums.Channel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@Document("APPLICATION_PAYMENT")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ApplicationPayment {
    @Id
    @JsonProperty(access = WRITE_ONLY)
    private String id;
    private String publicId;
    private String productId;
    private String productName;
    private String optionId;
    private String optionName;
    private Long amountHT;
    private Long tva = 20L;
    private String type;
    private String userId;
    private String userName;
    private String userEmail;
    private String providerSessionUrl;
    private String providerSessionId;
    private String providerSessionStatus;
    private Channel channel;
    private String frequence;
    private int credits;
    private Instant creation = Instant.now();
}
