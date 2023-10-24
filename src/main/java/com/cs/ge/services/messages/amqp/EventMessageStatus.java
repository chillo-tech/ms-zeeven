package com.cs.ge.services.messages.amqp;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@AllArgsConstructor
@Slf4j
@Component
public class EventMessageStatus {

    @RabbitListener(
            queues = {"${app.messages.status.queue}"}
    )
    public void handleMessage(final Map<String, String> params) {
        log.info("Paramètres {}", params);
    }
}
