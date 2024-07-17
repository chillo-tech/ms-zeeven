package com.cs.ge.notifications.service;

import com.cs.ge.notifications.entity.NotificationTemplate;
import com.cs.ge.notifications.repository.NotificationTemplateRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Service
@AllArgsConstructor
public class NotificationTemplateService {
    private NotificationTemplateRepository notificationTemplateRepository;

    private void checkTemplates(final NotificationTemplate template) {

        final Optional<NotificationTemplate> templateInBDD = this.notificationTemplateRepository
                .findByApplicationAndNameAndVersionAndType(
                        template.getApplication(),
                        template.getName(),
                        template.getVersion(),
                        template.getType()

                );
        if (templateInBDD.isPresent()) {
            throw new IllegalArgumentException(
                    format("un template existe déjà avec les paramètres application %s name %s version %s type %s", template.getApplication(),
                            template.getName(),
                            template.getVersion(),
                            template.getType()));
        }

    }

    public void create(final NotificationTemplate template) {
        this.checkTemplates(template);
        this.notificationTemplateRepository.save(template);
    }

    public NotificationTemplate update(final String id, final NotificationTemplate notificationTemplate) {
        final NotificationTemplate templateInBDD = this.notificationTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Aucun template n'existe avec les paramètres transmis"));

        templateInBDD.setContent(notificationTemplate.getContent());
        templateInBDD.setApplication(notificationTemplate.getApplication());
        templateInBDD.setName(notificationTemplate.getName());
        this.notificationTemplateRepository.deleteById(id);
        return this.notificationTemplateRepository.save(templateInBDD);
    }

    public List<NotificationTemplate> search() {
        final List<NotificationTemplate> notificationTemplates = this.notificationTemplateRepository.findAll();
        return notificationTemplates.stream().map(
                notificationTemplate -> NotificationTemplate.builder()
                        .id(notificationTemplate.getId())
                        .name(notificationTemplate.getName())
                        .application(notificationTemplate.getApplication())
                        .version(notificationTemplate.getVersion())
                        .type(notificationTemplate.getType())
                        .build()
        ).toList();
    }

    public void delete(final String id) {
        this.notificationTemplateRepository.deleteById(id);
    }
}
