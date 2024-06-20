package com.cs.ge.notifications.repository;

import com.cs.ge.notifications.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<Notification, String> {
}
