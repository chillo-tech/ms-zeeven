package com.cs.ge.notifications.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "TEMPLATE_STATUS")
public class TemplateStatus {
    private String id;
    private String name;
    private String providerTemplateId;
    private String localTemplateId;
    private String status;
    private String category;
    private Instant creation;
}
