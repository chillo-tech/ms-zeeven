package com.cs.ge.notifications.web;

import com.cs.ge.notifications.entity.NotificationTemplate;
import com.cs.ge.notifications.service.NotificationTemplateService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@AllArgsConstructor
@RestController
@RequestMapping(path = "v1/notification-template", produces = APPLICATION_JSON_VALUE)
public class NotificationTemplateController {

    private NotificationTemplateService notificationTemplateService;

    @PostMapping
    public void create(@RequestParam("application") final String application,
                       @RequestBody final Set<NotificationTemplate> templates) {
        this.notificationTemplateService.create(application, templates);
    }

    @PutMapping(consumes = APPLICATION_JSON_VALUE, path = "{id}")
    public NotificationTemplate update(@PathVariable final String id, @RequestBody final NotificationTemplate template) {
        return this.notificationTemplateService.update(id, template);
    }

    @DeleteMapping(path = "{id}")
    public void delete(@PathVariable final String id) {
        this.notificationTemplateService.delete(id);
    }

    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public List<NotificationTemplate> update() {
        return this.notificationTemplateService.search();
    }

}
