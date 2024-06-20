package com.cs.ge.notifications.service.template;

import com.cs.ge.notifications.entity.template.Template;
import com.cs.ge.notifications.entity.template.TemplateComponent;
import com.cs.ge.notifications.repository.TemplateRepository;
import com.cs.ge.notifications.service.whatsapp.WhatsappService;
import com.cs.ge.services.shared.SharedService;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cs.ge.enums.Channel.WHATSAPP;
import static com.cs.ge.notifications.enums.TemplateComponentType.BODY;

@AllArgsConstructor
@Service
public class TemplateService {
    private final WhatsappService whatsappService;
    private final SharedService sharedService;
    private final TemplateRepository templateRepository;

    public void create(final Template template) {
        String name = template.getName();
        name = String.format("%s", name);
        final String slug = this.sharedService.toSlug(name.toLowerCase());
        template.setSlug(slug);
        template.setName(name);

        Template templateInBDD = this.templateRepository.findBySlug(slug);

        if (templateInBDD == null) {
            final TemplateComponent body = template.getComponents().stream().filter(templateComponent -> templateComponent.getType().equals(BODY)).findFirst().orElse(null);
            if (body != null && !Strings.isNullOrEmpty(body.getText())) {
                final Map<Integer, String> mappings = TemplateService.getMatchers(body.getText());
                template.setWhatsAppMapping(mappings);
            }

            template.setName(name.toLowerCase().replaceAll(" ", "_"));
            templateInBDD = this.templateRepository.save(template);
        }
        if (template.getTypes() != null && template.getTypes().contains(WHATSAPP)) {
            this.whatsappService.createTemplate(templateInBDD);
        }
    }

    private static Map<Integer, String> getMatchers(final String text) {
        final Map<Integer, String> mappers = new HashMap<>();
        final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}");
        final Matcher matcher = VARIABLE_PATTERN.matcher(text);
        int i = 1;
        while (matcher.find()) {
            final String item = matcher.group();
            mappers.put(
                    i,
                    item.replaceAll("\\{", "").replaceAll("\\}", "")
            );
            i++;
        }
        return mappers;
    }


}
