package com.cs.ge.notifications.repository;

import com.cs.ge.notifications.entity.template.Template;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TemplateRepository extends MongoRepository<Template, String> {
    Template findBySlug(String name);

    Template findByName(String name);
}
