package com.cs.ge.notifications.repository;

import com.cs.ge.notifications.entity.TemplateStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TemplateStatusRepository extends MongoRepository<TemplateStatus, String> {

    TemplateStatus findFirstByProviderTemplateIdOrderByCreationDesc(String id);
}
