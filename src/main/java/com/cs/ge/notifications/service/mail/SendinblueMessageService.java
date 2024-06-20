package com.cs.ge.notifications.service.mail;

import com.cs.ge.notifications.records.brevo.Message;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "brevomessages", url = "${providers.brevo.host}")
public interface SendinblueMessageService {
    @PostMapping(path = "/${providers.brevo.path}")
    Map<String, Object> message(@RequestBody Message message);
}
