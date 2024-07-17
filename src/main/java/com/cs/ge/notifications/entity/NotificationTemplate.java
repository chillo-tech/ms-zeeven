package com.cs.ge.notifications.entity;

import com.cs.ge.enums.Channel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
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
