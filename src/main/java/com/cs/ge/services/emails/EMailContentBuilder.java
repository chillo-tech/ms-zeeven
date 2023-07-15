package com.cs.ge.services.emails;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Component
public class EMailContentBuilder {
    private final TemplateEngine templateEngine;

    public EMailContentBuilder(final TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String getTemplate(final String template, final Map<String, Object> replacements) {
        final Context context = new Context();
        context.setVariables(replacements);
        return this.templateEngine.process(template, context);
    }


}
