package com.cs.ge.entites;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventMessageNotificationStatus {
    private String providerNotificationId;
    private String status;
    private String code;
    private String provider;
    private String price;
    private Instant creation;
}
