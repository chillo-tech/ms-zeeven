package com.cs.ge.notifications.entity;

import com.cs.ge.enums.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "NOTIFICATION_STATUS")
public class NotificationStatus {
    @Id
    private String id;
    private String eventId;
    private String recipient;
    private String phone;
    private String email;
    private String userId;
    private String applicationMessageId;
    private String providerNotificationId;
    private String localNotificationId;
    private Channel channel;
    private String status;
    private String code;
    private String provider;
    private String price;
    private Instant creation;
}
