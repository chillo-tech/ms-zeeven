package com.cs.ge.notifications.service.mail;

import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Invitation;
import com.cs.ge.entites.Template;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.notifications.entity.Notification;
import com.cs.ge.notifications.entity.NotificationStatus;
import com.cs.ge.notifications.entity.Recipient;
import com.cs.ge.notifications.entity.Sender;
import com.cs.ge.notifications.records.brevo.Contact;
import com.cs.ge.notifications.records.brevo.Message;
import com.cs.ge.notifications.repository.NotificationTemplateRepository;
import com.cs.ge.notifications.service.NotificationMapper;
import com.cs.ge.services.shared.SharedService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cs.ge.enums.Channel.EMAIL;
import static com.cs.ge.enums.Channel.MAIL;
import static com.cs.ge.notifications.data.ApplicationData.FOOTER_TEXT;
import static java.lang.String.format;

@Slf4j
@Service
public class MailService extends NotificationMapper {
    private final String recipient;
    private final SendinblueMessageService brevoMessageService;
    private final SharedService sharedService;

    public MailService(
            final NotificationTemplateRepository notificationTemplateRepository,
            final SendinblueMessageService brevoMessageService,
            @Value("${application.recipient.email:#{null}}") final String recipient, final SharedService sharedService) {
        super(notificationTemplateRepository);
        this.brevoMessageService = brevoMessageService;
        this.recipient = recipient;
        this.sharedService = sharedService;
    }

    public List<NotificationStatus> send(final Notification notification) {
        return notification.getContacts().stream().map((final Recipient to) -> {
            final NotificationStatus notificationStatus = this.getNotificationStatus(
                    notification,
                    to.getId(),
                    MAIL,
                    "", //result.get("messageId").toString(), // TODO decommenter
                    "INITIAL"
            );
            notificationStatus.setProvider("BREVO");
            notificationStatus.setEmail(to.getEmail());
            try {
                if (this.sharedService.isEmailValid(to.getEmail())) {
                    final String messageToSend = String.valueOf(this.map(notification, to, EMAIL).get("message"));
                    final Map<String, Object> result = this.sendMessageUsingSendinBlueAPI(notification, notification.getFrom(), messageToSend, to);
                    notificationStatus.setProviderNotificationId(result.get("messageId").toString());
                } else {
                    log.info("Aucun envoi pour {} {} son email {} est null ou invalide", to.getFirstName(), to.getLastName(), to.getEmail());
                    notificationStatus.setStatus("NOT_SEND");
                }
                return notificationStatus;
            } catch (final Exception e) {
                MailService.log.error("ERROR", e);
                notificationStatus.setStatus("ERROR");
            }
            return notificationStatus;
        }).collect(Collectors.toList());
    }


    private Map<String, Object>
    sendMessageUsingSendinBlueAPI(final Notification notification, final Sender from, final String messageToSend, final Recipient to) {
        final Parser parser = Parser.builder().build();
        final Node document = parser.parse(format("%s<p>%s</p>", messageToSend.replaceAll("\\n", "<br />"), FOOTER_TEXT));
        final HtmlRenderer renderer = HtmlRenderer.builder().build();

        String lastName = from.getLastName();
        if (lastName != null) {
            lastName = lastName.toUpperCase();
        }

        String firstName = from.getFirstName();
        if (firstName != null) {
            firstName = format("%s%s", firstName.substring(0, 1).toUpperCase(), firstName.substring(1).toLowerCase());
        }

        List<Map<String, String>> attachment = null;
        if (!Strings.isNullOrEmpty(notification.getImage())) {
            attachment = List.of(Map.of("url", notification.getImage()));
        }
        final Message message = new Message(
                notification.getSubject(),
                renderer.render(document),
                new Contact(format("%s %s VIA ZEEVEN", firstName, lastName), from.getEmail()),
                this.mappedContacts(Set.of(to)),
                attachment
        );
        return this.brevoMessageService.message(message);
    }

    private Set<Contact> mappedContacts(final Set<Recipient> recipients) {

        return recipients.stream().map(
                        (final Recipient to) -> {
                            String email = this.recipient;
                            if (this.recipient == null) {
                                email = to.getEmail();
                            }
                            return new Contact(format("%s %s", to.getFirstName(), to.getLastName()), email);
                        })
                .collect(Collectors.toSet());
    }


    public List<NotificationStatus> sendFromParams(final String eventName, final Invitation invitation, final Guest to, final Map<String, Object> notificationParams, final Channel channel) throws InvocationTargetException, IllegalAccessException {
        final Template template = invitation.getTemplate();
        final List<String> mappedSchedules = this.getMappedSchedules(template);

        final Recipient recipient = new Recipient();
        BeanUtils.copyProperties(recipient, to);

        final Sender sender = new Sender();
        final UserAccount userAccount = (UserAccount) notificationParams.get("author");
        BeanUtils.copyProperties(sender, userAccount);

        final Notification notification = Notification.builder()
                .eventId(format("%s", notificationParams.get("eventId")))
                .id(format("%s", notificationParams.get("getId")))
                .template(format("%s", notificationParams.get("whatsappTemplateName")))
                .application(format("%s", notificationParams.get("application")))
                .image(format("%s", notificationParams.get("image")))
                .from(sender)
                .subject(format("%s", eventName))
                .date(String.join(" | ", mappedSchedules))
                .eventId(format("%s", notificationParams.get("eventId")))
                .applicationMessageId(format("%s", notificationParams.get("applicationMessageId")))
                .build();

        final NotificationStatus notificationStatus = this.getNotificationStatus(
                notification,
                recipient.getId(),
                channel,
                "", //result.get("messageId").toString(), // TODO decommenter
                "INITIAL"
        );
        notificationStatus.setProvider("BREVO");
        notificationStatus.setEmail(recipient.getEmail());
        try {
            final String messageToSend = String.valueOf(this.map(notification, recipient, channel).get("message"));
            final Map<String, Object> result = this.sendMessageUsingSendinBlueAPI(notification, sender, messageToSend, recipient);
            notificationStatus.setProviderNotificationId(result.get("messageId").toString());
            return List.of(notificationStatus);
        } catch (final Exception e) {
            e.printStackTrace();
            log.error("ERROR {}", e.getMessage());
            notificationStatus.setStatus("ERROR");
        }
        return List.of(notificationStatus);
    }

}
