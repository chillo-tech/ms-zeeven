package com.cs.ge.notifications.entity.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TemplateExample {

    List<String> header_text;
    List<String> header_handle;
    List<List<String>> body_text;
}
