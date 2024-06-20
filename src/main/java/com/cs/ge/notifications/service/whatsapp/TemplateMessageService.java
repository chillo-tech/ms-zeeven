package com.cs.ge.notifications.service.whatsapp;

import com.cs.ge.notifications.entity.template.WhatsAppTemplate;
import com.cs.ge.notifications.service.whatsapp.dto.WhatsAppResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "whatsapp-template-messages", url = "${providers.whatsapp.template-host}")
public interface TemplateMessageService {

    @PostMapping("/message_templates")
    WhatsAppResponse template(@RequestBody WhatsAppTemplate whatsAppTemplate);

}
