package com.cs.ge.notifications.service.sms;

import com.cs.ge.notifications.entity.Notification;
import com.cs.ge.notifications.entity.NotificationStatus;
import com.cs.ge.notifications.entity.Recipient;
import com.cs.ge.notifications.repository.NotificationTemplateRepository;
import com.cs.ge.notifications.service.NotificationMapper;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static com.cs.ge.enums.Channel.SMS;
import static com.cs.ge.notifications.data.ApplicationData.FOOTER_TEXT;

@Slf4j
@Service
public class TwilioSMSService extends NotificationMapper {
    private final String twilioServciceId;
    private final String callbackPath;
    private final String twilioAccountId;
    private final String twilioAlphaId;
    private final String recipient;
    private final String twilioAccountSecret;

    public TwilioSMSService(
            @Value("${application.recipient.sms:#{null}}") final String recipient,
            @Value("${providers.twilio.callback-path}") final String callbackPath,
            @Value("${providers.twilio.alpha-id}") final String twilioAlphaId,
            @Value("${providers.twilio.service-id}") final String twilioServciceId,
            @Value("${providers.twilio.account-id}") final String twilioAccountId,
            @Value("${providers.twilio.account-secret}") final String twilioAccountSecret,
            final NotificationTemplateRepository notificationTemplateRepository
    ) {
        super(notificationTemplateRepository);
        this.recipient = recipient;
        this.callbackPath = callbackPath;
        this.twilioServciceId = twilioServciceId;
        this.twilioAccountId = twilioAccountId;
        this.twilioAlphaId = twilioAlphaId;
        this.twilioAccountSecret = twilioAccountSecret;
        Twilio.init(this.twilioAccountId, this.twilioAccountSecret);
    }

    @Async
    public List<NotificationStatus> send(final Notification notification) {
        return notification.getContacts().parallelStream().map((final Recipient to) -> {
            String messageToSend = String.valueOf(this.map(notification, to, SMS).get("message"));

            String phoneNumber = this.recipient;
            if (phoneNumber == null) {
                phoneNumber = String.format("+%s%s", to.getPhoneIndex(), to.getPhone());
            }
            messageToSend = String.format("%s\n%s", messageToSend, FOOTER_TEXT);

            final Message createdMessage = Message.creator(
                            new com.twilio.type.PhoneNumber(phoneNumber),
                            this.twilioServciceId,
                            messageToSend
                    )
                    .setFrom(this.twilioAlphaId)
                    .setStatusCallback(URI.create(this.callbackPath))
                    .create();
            final NotificationStatus notificationStatus = this.getNotificationStatus(
                    notification,
                    to.getId(),
                    SMS,
                    createdMessage.getSid(),
                    createdMessage.getStatus().name()
            );
            notificationStatus.setProvider("TWILIO");
            return notificationStatus;

        }).collect(Collectors.toList());
    }

}
