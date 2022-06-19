package com.cs.ge.services.emails;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.util.Map;

@Component
public class EMailContentBuilder {
    private final SpringTemplateEngine templateEngine;

    public EMailContentBuilder(final SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String getTemplate(String template, Map<String, Object> replacements) {
        final Context context = new Context();
        context.setVariables(replacements);
        return this.templateEngine.process(template, context);
    }


}
