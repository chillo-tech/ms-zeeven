package com.cs.ge.services.notifications;

import com.cs.ge.dto.ApplicationNotification;
import com.cs.ge.dto.MessageProfile;
import com.cs.ge.entites.*;
import com.cs.ge.enums.Channel;
import com.cs.ge.feign.FileHandler;
import com.cs.ge.notifications.entity.Notification;
import com.cs.ge.notifications.service.NotificationService;
import com.cs.ge.services.ProfileService;
import com.cs.ge.services.shared.SharedService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ASynchroniousNotifications {
    private final NotificationService notificationService;
    private final String administratorFirstname;
    private final String administratorLastname;
    private final String administratoremail;
    private final FileHandler fIleHandler;
    private final ProfileService profileService;
    private final SharedService sharedService;

    public ASynchroniousNotifications(
            final FileHandler fIleHandler,
            final NotificationService notificationService,
            @Value("${app.administrator.firstname}") final String administratorFirstname,
            @Value("${app.administrator.lastname}") final String administratorLastname,
            @Value("${app.administrator.email}") final String administratoremail,
            final ProfileService profileService, final SharedService sharedService) {
        this.fIleHandler = fIleHandler;
        this.administratorFirstname = administratorFirstname;
        this.administratorLastname = administratorLastname;
        this.administratoremail = administratoremail;
        this.profileService = profileService;
        this.notificationService = notificationService;
        this.sharedService = sharedService;
    }


    private static Map<String, String> userAsMap(final Profile profile) {
        final Map<String, String> from = new HashMap();
        from.put("firstName", profile.getFirstName());
        from.put("lastName", profile.getLastName());
        from.put("email", profile.getEmail());
        from.put("phoneIndex", profile.getPhoneIndex());
        from.put("phone", profile.getPhone());
        return from;
    }

    private static String messageAsString(final ApplicationMessage applicationMessage) {

        String textWithVariables = applicationMessage.getText();
        final Pattern pattern = Pattern.compile("\\{\\{\\w+}}");
        final Matcher matcher = pattern.matcher(textWithVariables);
        int index = 0;
        while (matcher.find()) {
            final String searchString = matcher.group();
            final String replacement = applicationMessage.getInformations().get(index);
            textWithVariables = StringUtils.replaceOnce(textWithVariables, searchString, replacement);
            index++;
        }
        return textWithVariables;
    }

    @Async
    public void sendEmail(
            UserAccount author,
            UserAccount recipient,
            final Map<String, List<Object>> parameters,
            final String appliation,
            final String template,
            final String message,
            final String subject
    ) {
        ASynchroniousNotifications.log.info("[CONTROLLER] Envoi d'un code pour le nouveau mot de passe {}", template);
        final UserAccount exp = new UserAccount();
        exp.setLastName(this.administratorLastname);
        exp.setFirstName(this.administratorFirstname);
        exp.setEmail(this.administratoremail);
        if (author == null) {
            author = exp;
        }
        if (recipient == null) {
            recipient = exp;
        }
        final MessageProfile expProfile = this.sharedService.userAccountToMessageProfile(author);
        final MessageProfile to = this.sharedService.userAccountToMessageProfile(recipient);


        final ApplicationNotification applicationNotification = new ApplicationNotification(
                appliation,
                template,
                subject,
                RandomStringUtils.random(8, true, true),
                RandomStringUtils.random(8, true, true),
                message,
                parameters,
                List.of(Channel.EMAIL),
                expProfile,
                List.of(to)
        );

        /*
        final MessageProperties properties = new MessageProperties();
        properties.setHeader("application", appliation);
        properties.setHeader("type", "message");
        final ObjectMapper objectMapper = new ObjectMapper();
        ASynchroniousNotifications.log.info("Envoi de mail {}", appliation);
        this.rabbitTemplate.convertAndSend(new Message(objectMapper.writeValueAsBytes(notification), properties));
        ASynchroniousNotifications.log.info("Fin envoi de mail {}", appliation);
        */

        final Notification notification = this.sharedService.applicationNotificationToNotification(applicationNotification);
        this.notificationService.send(notification.getApplication(), notification, notification.getChannels().stream().toList());
    }

    @Async
    public void sendEventMessage(
            final Event event,
            final UserAccount author,
            final BaseApplicationMessage applicationMessage,
            final List<Channel> channelsToHandle,
            final String template,
            final Map<String, List<String>> extraParams
    ) {
        ASynchroniousNotifications.log.info("ApplicationNotification du message de {}", applicationMessage.getId());

        final Map<String, List<Object>> params = this.sharedService.messageParameters(applicationMessage);
        params.put("trial", List.of(String.valueOf(author.isTrial())));
        final ApplicationNotification applicationNotification = new ApplicationNotification(
                "ZEEVEN",
                template,
                event.getName(),
                event.getId(),
                applicationMessage.getId(),
                applicationMessage.getText(),
                params,
                channelsToHandle,
                this.sharedService.userAccountToMessageProfile(author),
                event.getGuests().parallelStream().map(guest -> this.sharedService.guestToMessageProfile(guest)).collect(Collectors.toList()));
/*
        final MessageProperties properties = new MessageProperties();
        properties.setHeader("action", "send");
        final Gson gson = new Gson();
        final String jsonString = gson.toJson(notification);
       log.info("Envoi du message {}", jsonString);
        this.rabbitTemplate.setExchange(this.applicationMessagesSendExchange);
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), properties));
*/
        final Notification notification = this.sharedService.applicationNotificationToNotification(applicationNotification);

        this.notificationService.send(notification.getApplication(), notification, notification.getChannels().stream().toList());
    }


    public void sendEventMessageToContact(
            final Event event,
            final UserAccount author,
            final Guest guest,
            final BaseApplicationMessage applicationMessage,
            final List<Channel> channelsToHandle,
            final String template,
            final Map<String, List<String>> extraParams
    ) {
        log.info("ApplicationNotification du message de {}", applicationMessage.getId());

        final Map<String, List<Object>> params = this.sharedService.messageParameters(applicationMessage);
        params.put("trial", List.of(String.valueOf(author.isTrial())));
        final ApplicationNotification applicationNotification = new ApplicationNotification(
                "ZEEVEN",
                template,
                event.getName(),
                event.getId(),
                applicationMessage.getId(),
                applicationMessage.getText(),
                params,
                channelsToHandle,

                this.sharedService.userAccountToMessageProfile(author),
                List.of(this.sharedService.guestToMessageProfile(guest))
        );
/*
        final MessageProperties properties = new MessageProperties();
        properties.setHeader("action", "send");
        final Gson gson = new Gson();
        final String jsonString = gson.toJson(notification);
        ASynchroniousNotifications.log.info("Envoi du message {}", jsonString);
        this.rabbitTemplate.setExchange(this.applicationMessagesSendExchange);
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), properties));
*/

        final Notification notification = this.sharedService.applicationNotificationToNotification(applicationNotification);

        this.notificationService.send(notification.getApplication(), notification, notification.getChannels().stream().toList());
    }

    public void sendPaymentConfirmationMessage(final Event event, final ApplicationMessage applicationMessage, final List<Channel> channelsToHandle) {
        log.info("ApplicationNotification du message de {}", applicationMessage.getId());

        final UserAccount author = this.profileService.findById(event.getAuthorId());

        final Map<String, List<Object>> params = this.sharedService.messageParameters(applicationMessage);
        params.put("trial", List.of(String.valueOf(author.isTrial())));
        final ApplicationNotification applicationNotification = new ApplicationNotification(
                "ZEEVEN",
                null,
                event.getName(),
                event.getId(),
                applicationMessage.getId(),
                applicationMessage.getText(),
                params,
                channelsToHandle,

                this.sharedService.userAccountToMessageProfile(author),
                event.getGuests().parallelStream().map(guest ->
                        this.sharedService.guestToMessageProfile(guest)).collect(Collectors.toList()));
/*
        final MessageProperties properties = new MessageProperties();
        properties.setHeader("application", "ZEEVEN");
        properties.setHeader("type", "message");
        final Gson gson = new Gson();
        final String jsonString = gson.toJson(notification);
        final ObjectMapper objectMapper = new ObjectMapper();
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), properties));
*/

        final Notification notification = this.sharedService.applicationNotificationToNotification(applicationNotification);

        this.notificationService.send(notification.getApplication(), notification, notification.getChannels().stream().toList());
    }

    public void sendInvitationMessage(final Map<String, Object> messageParameters) throws JsonProcessingException {
        /*
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("application", "ZEEVEN");
        messageProperties.setHeader("type", "invitation");

        final Gson gson = new Gson();
        final String jsonString = gson.toJson(messageParameters);
        this.rabbitTemplate.setExchange(this.applicationInvitationsExchange);
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), messageProperties));
*/
        this.notificationService.sendInvitation(messageParameters);
    }

    public void sendFile(final Map<String, Object> params) {
        ASynchroniousNotifications.log.info("Create file");
        this.fIleHandler.send(params);
        /*
        boolean error = false;
        final FTPClient ftp = new FTPClient();
        try {
            final String filePath = String.valueOf(params.get("path"));
            if (filePath != null && !filePath.equals("null") && Strings.isNotEmpty(filePath)) {
                final String fullPath = String.format("%s/%s", this.basePath, filePath);
                final Path folder = Paths.get(fullPath).getParent();
                Files.createDirectories(folder);

                final String fileAsString = String.valueOf(params.get("file"));
                final byte[] decodedFile = Base64.getDecoder().decode(fileAsString);
                final File fullPathAsFile = new File(fullPath);
                if (Files.exists(Paths.get(fullPath))) {
                    FileUtils.delete(fullPathAsFile);
                }
                FileUtils.writeByteArrayToFile(fullPathAsFile, decodedFile);
                final ByteArrayInputStream local = new ByteArrayInputStream(decodedFile);

                ASynchroniousNotifications.log.info("Create file at " + fullPath);
                final int reply;
                final String server = "192.168.1.200";
                ftp.connect(server);
                ASynchroniousNotifications.log.info("Connected to " + server + ".");
                ASynchroniousNotifications.log.info(ftp.getReplyString());

                reply = ftp.getReplyCode();

                if (!FTPReply.isPositiveCompletion(reply)) {
                    ftp.disconnect();
                    ASynchroniousNotifications.log.error("FTP server refused connection.");
                    System.exit(1);
                }
                ftp.appendFile(fullPath, local);
                ftp.logout();
                ASynchroniousNotifications.log.info("File created at " + fullPath);
            }
        } catch (final IOException e) {
            error = true;
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (final IOException ioe) {
                    // do nothing
                }
            }
            System.exit(error ? 1 : 0);
        }

         */
    }
}
