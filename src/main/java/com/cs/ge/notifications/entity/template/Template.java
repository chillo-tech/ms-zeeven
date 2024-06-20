package com.cs.ge.notifications.entity.template;

import com.cs.ge.enums.Channel;
import com.cs.ge.notifications.enums.Application;
import com.cs.ge.notifications.enums.TemplateCategory;
import com.cs.ge.notifications.enums.TemplateState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document("TEMPLATE")
public class Template {
    @Id
    private String id;
    TemplateState whatsAppState;
    String name;
    String slug;
    Application application;
    TemplateCategory category;
    List<TemplateComponent> components;
    List<Channel> types;

    Map<Integer, String> whatsAppMapping;
}
