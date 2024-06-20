package com.cs.ge.notifications.repository;

import com.cs.ge.notifications.entity.NotificationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationStatusRepository extends MongoRepository<NotificationStatus, String> {

    NotificationStatus findFirstByProviderNotificationIdOrderByCreationDesc(String id);

    List<NotificationStatus> findByEventId(String id);
}
