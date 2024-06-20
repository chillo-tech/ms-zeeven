package com.cs.ge.notifications.entity.template;

import com.cs.ge.notifications.enums.TemplateButtonType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TemplateButton {
    TemplateButtonType type;
    String text;
    String phone_number;
    List<String> example;
}
