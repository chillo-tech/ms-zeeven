package com.cs.ge.services.whatsapp;

import com.cs.ge.configuration.FeignConfiguration;
import com.cs.ge.services.whatsapp.dto.TextMessage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "whatsappmessages", url = "${providers.whatsapp.host}", configuration = FeignConfiguration.class)
public interface TextMessageService {
    @PostMapping("/messages")
    void message(@RequestBody TextMessage textMessage);
}
