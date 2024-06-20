package com.cs.ge.notifications.service;

import com.cs.ge.enums.Channel;
import com.cs.ge.notifications.entity.Notification;
import com.cs.ge.notifications.entity.NotificationStatus;
import com.cs.ge.notifications.repository.NotificationRepository;
import com.cs.ge.notifications.repository.NotificationStatusRepository;
import com.cs.ge.notifications.service.mail.MailService;
import com.cs.ge.notifications.service.sms.TwilioSMSService;
import com.cs.ge.notifications.service.sms.VonageSMSService;
import com.cs.ge.notifications.service.whatsapp.WhatsappService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class NotificationService {
    private final MailService mailService;
    private final WhatsappService whatsappService;
    private final TwilioSMSService twilioSmsService;
    private final VonageSMSService vonageSMSService;
    private final NotificationRepository notificationRepository;
    private final NotificationStatusRepository notificationStatusRepository;

    public NotificationService(final MailService mailService, final WhatsappService whatsappService, final TwilioSMSService twilioSmsService, final VonageSMSService vonageSMSService, final NotificationRepository notificationRepository, final NotificationStatusRepository notificationStatusRepository) {
        this.mailService = mailService;
        this.whatsappService = whatsappService;
        this.vonageSMSService = vonageSMSService;
        this.twilioSmsService = twilioSmsService;
        this.notificationRepository = notificationRepository;
        this.notificationStatusRepository = notificationStatusRepository;
    }

    public void send(final String application, final Notification notification, final List<Channel> types) {
        types.forEach(type -> {
            try {

                final List<NotificationStatus> notificationStatusList = new ArrayList<>();
                switch (type) {
                    case MAIL, EMAIL -> {
                        NotificationService.log.info("Message de type {}", type);
                        final List<NotificationStatus> mailStatusList = this.mailService.send(notification);
                        notificationStatusList.addAll(mailStatusList);
                    }
                    case WHATSAPP -> {
                        NotificationService.log.info("Message de type {}", type);
                        final List<NotificationStatus> whatsappStatusList = this.whatsappService.send(notification);
                        notificationStatusList.addAll(whatsappStatusList);
                    }
                    case SMS -> {
                        NotificationService.log.info("Message de type {}", type);
                        final List<NotificationStatus> smsStatusList = this.twilioSmsService.send(notification);
                        notificationStatusList.addAll(smsStatusList);
                    }
                    default -> NotificationService.log.info("type {} inconnu", type);
                }
                notification.setType(type);
                notification.setCreation(Instant.now());
                notification.setApplication(application);
                final Notification saved = this.notificationRepository.save(notification);
                notificationStatusList.parallelStream().forEach(notificationStatus -> notificationStatus.setLocalNotificationId(saved.getId()));

                this.notificationStatusRepository.saveAll(notificationStatusList);
            } catch (final Exception e) {
                NotificationService.log.error("ERREUR LORS DE L'ENVOI d'un message", e);
                e.printStackTrace();
            }
        });

    }

    public List<NotificationStatus> statistics(final String id) {
        return this.notificationStatusRepository.findByEventId(id);
    }

    public void sendInvitation(final Map<String, Object> notificationParams) {
        final Notification notification = new Notification();
        final String application = (String) notificationParams.get("application");
        //final List<Object> channelsAsObject = new ArrayList<>(notificationParams.get("channels"));
        final List<Channel> types = new ArrayList<>((Collection) notificationParams.get("channels"));
       /* final List<Channel> types = Stream.of(channels).map(item -> {
            NotificationService.log.info("item {}", item);
            return Channel.valueOf(String.valueOf(item));
        }).toList();*/
        types.forEach(type -> {
            try {

                final List<NotificationStatus> notificationStatusList = new ArrayList<>();
                switch (type) {
                    case WHATSAPP -> {
                        NotificationService.log.info("Message de type {}", type);
                        final List<NotificationStatus> whatsappStatusList = this.whatsappService.sendFromParams(notificationParams, type);
                        notificationStatusList.addAll(whatsappStatusList);
                    }
                    default -> NotificationService.log.info("type {} inconnu", type);
                }
                notification.setType(type);
                notification.setCreation(Instant.now());
                notification.setApplication(application);
                final Notification saved = this.notificationRepository.save(notification);
                notificationStatusList.parallelStream().forEach(notificationStatus -> notificationStatus.setLocalNotificationId(saved.getId()));

                this.notificationStatusRepository.saveAll(notificationStatusList);
            } catch (final Exception e) {
                NotificationService.log.info("ERREUR LORS DE L'ENVOI d'un message");
                NotificationService.log.error("ERREUR LORS DE L'ENVOI d'un message", e);
                e.printStackTrace();
            }
        });
    }


}
