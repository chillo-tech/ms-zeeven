package com.cs.ge.entites;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("QR_CODE_STATISTIC")
@Builder
@Getter
public class QRCodeStatistic {

    @Id
    private String id;
    private String qrCode;
    private String host;
    private String agent;
    private String ip;
    private String city;
    private String zipcode;
    private String country;
    private String language;
    private String latitude;
    private String longitude;
    private Instant creation = Instant.now();
}
