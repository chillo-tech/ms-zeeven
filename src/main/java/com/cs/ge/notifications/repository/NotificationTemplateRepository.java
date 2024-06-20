package com.cs.ge.notifications.repository;

import com.cs.ge.enums.Channel;
import com.cs.ge.notifications.entity.NotificationTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findByApplicationAndName(String application, String name);

    Optional<NotificationTemplate> findByApplicationAndNameAndVersionAndType(String application, String name, int version, Channel type);

    List<NotificationTemplate> findByApplicationAndNameIn(String application, Iterable<String> names);

    List<NotificationTemplate> findByApplication(String application);

    NotificationTemplate findByName(String templateName);
}
