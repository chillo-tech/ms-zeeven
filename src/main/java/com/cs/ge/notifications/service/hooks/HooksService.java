package com.cs.ge.notifications.service.hooks;

import com.cs.ge.notifications.entity.NotificationStatus;
import com.cs.ge.notifications.records.whatsapp.WhatsappChangeValueStatus;
import com.cs.ge.notifications.records.whatsapp.WhatsappEntry;
import com.cs.ge.notifications.records.whatsapp.WhatsappNotification;
import com.cs.ge.notifications.repository.NotificationStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static com.cs.ge.enums.Channel.MAIL;
import static com.cs.ge.enums.Channel.SMS;
import static com.cs.ge.enums.Channel.WHATSAPP;


@Slf4j
@Service
@AllArgsConstructor
public class HooksService {
    private NotificationStatusRepository notificationStatusRepository;

    public void vonage(final Map<String, Object> params) {
        //log.info("vonage {}", params);
        final NotificationStatus notificationStatus = this.getNotificationStatus(params.get("MessageSid").toString());
        String status = params.get("status").toString();
        if (!Strings.isNullOrEmpty(status)) {
            status = status.toUpperCase();
        }
        notificationStatus.setStatus(status);
        notificationStatus.setPrice(params.get("price").toString());
        notificationStatus.setCode(params.get("err-code").toString());
        notificationStatus.setProvider("VONAGE");

        notificationStatus.setId(null);
        notificationStatus.setChannel(SMS);
        this.notificationStatusRepository.save(notificationStatus);
    }

    public void whatsapp(final WhatsappNotification notification) {
        final ObjectMapper objectMapper = new ObjectMapper();
        //log.info("whatsapp {}", objectMapper.convertValue(notification, Map.class));
        final List<WhatsappEntry> entry = notification.entry();
        entry.forEach(item -> {
            item.changes().forEach(whatsappChange -> {
                final List<WhatsappChangeValueStatus> statuses = whatsappChange.value().statuses();
                if (statuses != null) {
                    statuses.forEach(status -> {

                        final NotificationStatus notificationStatus = this.getNotificationStatus(status.id());
                        String messageStatus = status.status();
                        if (!Strings.isNullOrEmpty(messageStatus)) {
                            messageStatus = messageStatus.toUpperCase();
                        }
                        notificationStatus.setStatus(messageStatus);
                        notificationStatus.setProvider("WHATSAPP");
                        notificationStatus.setRecipient(status.recipient_id());

                        notificationStatus.setId(null);
                        notificationStatus.setChannel(WHATSAPP);
                        final NotificationStatus saved = this.notificationStatusRepository.save(notificationStatus);
                    });
                }
            });
        });
    }


    public void twilio(final MultiValueMap<String, Object> params) {
        //log.info("twilio params {} ", params);
        final NotificationStatus notificationStatus = this.getNotificationStatus("" + params.get("MessageSid").toArray()[0]);
        String status = String.format("%s", params.get("MessageStatus").toArray()[0]);
        if (!Strings.isNullOrEmpty(status)) {
            status = status.toUpperCase();
        }
        notificationStatus.setStatus(status);
        notificationStatus.setStatus(status);
        notificationStatus.setProvider("TWILIO");
        notificationStatus.setChannel(SMS);

        notificationStatus.setId(null);
        final NotificationStatus saved = this.notificationStatusRepository.save(notificationStatus);
    }

    public void brevo(final Map<String, Object> params) {
        // log.info("brevo params {} ", params);
        final NotificationStatus notificationStatus = this.getNotificationStatus("" + params.get("message-id"));
        String status = String.format("%s", params.get("event")).toUpperCase();
        if (!Strings.isNullOrEmpty(status)) {
            status = status.toUpperCase();
        }
        notificationStatus.setStatus(status);
        notificationStatus.setProvider("BREVO");
        notificationStatus.setChannel(MAIL);

        notificationStatus.setId(null);
        final NotificationStatus saved = this.notificationStatusRepository.save(notificationStatus);
        //this.aSynchroniousNotifications.sendMessageStatus(saved);
    }

    private NotificationStatus getNotificationStatus(final String field) {
        NotificationStatus notificationStatus = this.notificationStatusRepository.findFirstByProviderNotificationIdOrderByCreationDesc(field);
        if (notificationStatus == null) {
            notificationStatus = new NotificationStatus();
        }
        return notificationStatus;
    }
}
