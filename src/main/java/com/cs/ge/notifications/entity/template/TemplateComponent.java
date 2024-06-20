package com.cs.ge.notifications.entity.template;

import com.cs.ge.notifications.enums.TemplateComponentDataFormat;
import com.cs.ge.notifications.enums.TemplateComponentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TemplateComponent {

    TemplateComponentType type;
    TemplateComponentDataFormat format;
    String text;
    TemplateExample example;
    List<TemplateButton> buttons;
}
