package com.cs.ge.notifications.entity;

import com.cs.ge.enums.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode
@ToString(callSuper = true)
@Document("NOTIFICATION_TEMPLATE")
public class NotificationTemplate {

    @Id
    String id;
    String name;
    String application;
    int version;
    Channel type;
    String content;
    String injectHtmlSelector;

}
