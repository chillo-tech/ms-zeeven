package com.cs.ge.entites;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("QR_CODE")
@Builder
@Getter
public class QRCodeStatistic {

    @Id
    private String id;
    private String qrCode;
    private LocalDateTime creation = LocalDateTime.now();
}
