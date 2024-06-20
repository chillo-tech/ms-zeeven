package com.cs.ge.notifications.entity.template;

import com.cs.ge.enums.Channel;
import com.cs.ge.notifications.enums.TemplateCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class WhatsAppTemplate {
    String name;
    String application;
    boolean allow_category_change;
    String language;
    Channel type;
    TemplateCategory category;
    List<TemplateComponent> components;
}
