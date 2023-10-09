package com.cs.ge.services.notifications;

import com.cs.ge.dto.ApplicationNotification;
import com.cs.ge.dto.MessageProfile;
import com.cs.ge.dto.ProfileParams;
import com.cs.ge.entites.ApplicationMessage;
import com.cs.ge.entites.BaseApplicationMessage;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.Profile;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.Civility;
import com.cs.ge.services.ProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@Slf4j
public class ASynchroniousNotifications {
    private final RabbitTemplate rabbitTemplate;
    private final String administratorFirstname;
    private final String administratorLastname;
    private final String applicationFilesExchange;
    private final String applicationFilesVhost;
    private final String applicationInvitationsExchange;
    private final String administratoremail;
    private final ProfileService profileService;

    public ASynchroniousNotifications(final RabbitTemplate rabbitTemplate,
                                      @Value("${app.administrator.firstname}") final String administratorFirstname,
                                      @Value("${app.administrator.lastname}") final String administratorLastname,
                                      @Value("${app.files.exchange}") final String applicationFilesExchange,
                                      @Value("${app.files.virtual-host}") final String applicationFilesVhost,
                                      @Value("${app.invitations.exchange}") final String applicationInvitationsExchange,
                                      @Value("${app.administrator.email}") final String administratoremail,
                                      final ProfileService profileService) {
        this.rabbitTemplate = rabbitTemplate;
        this.administratorFirstname = administratorFirstname;
        this.administratorLastname = administratorLastname;
        this.applicationFilesExchange = applicationFilesExchange;
        this.applicationFilesVhost = applicationFilesVhost;
        this.applicationInvitationsExchange = applicationInvitationsExchange;
        this.administratoremail = administratoremail;
        this.profileService = profileService;
    }


    @Async
    public void sendEmail(
            UserAccount author,
            UserAccount recipient,
            final Map<String, List<String>> parameters,
            final String appliation,
            final String template,
            final String message,
            final String subject
    ) {

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
        final MessageProfile expProfile = this.getUserInfos(
                author.getId(),
                author.getCivility(),
                author.getFirstName(),
                author.getLastName(),
                author.getEmail(),
                author.getPhone(),
                author.getPhoneIndex(),
                author.isTrial(),
                author.getOthers()
        );

        final MessageProfile to = this.getUserInfos(
                recipient.getId(),
                recipient.getCivility(),
                recipient.getFirstName(),
                recipient.getLastName(),
                recipient.getEmail(),
                recipient.getPhone(),
                recipient.getPhoneIndex(),
                recipient.isTrial(),
                author.getOthers()
        );


        final ApplicationNotification notification = new ApplicationNotification(
                appliation,
                template,
                subject,
                RandomStringUtils.random(8, true, true),
                message,
                parameters,
                List.of(Channel.EMAIL),
                expProfile,
                List.of(to)
        );

        final MessageProperties properties = new MessageProperties();
        properties.setHeader("application", appliation);
        properties.setHeader("type", "message");
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            this.rabbitTemplate.convertAndSend(new Message(objectMapper.writeValueAsBytes(notification), properties));
        } catch (final JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    public void sendEventMessage(
            final Event event,
            final UserAccount author,
            final BaseApplicationMessage applicationMessage,
            final List<Channel> channelsToHandle,
            final String template,
            final Map<String, List<String>> extraParams
    ) {
        log.info("ApplicationNotification du message de {}", applicationMessage.getId());

        final Map<String, List<String>> params = this.messageParameters(applicationMessage);
        params.put("trial", List.of(String.valueOf(author.isTrial())));
        final ApplicationNotification notification = new ApplicationNotification(
                "ZEEVEN",
                template,
                event.getName(),
                event.getId(),
                applicationMessage.getText(),
                params,
                channelsToHandle,
                this.getUserInfos(author.getId(), author.getCivility(), author.getFirstName(), author.getLastName(), author.getEmail(), author.getPhone(), author.getPhoneIndex(), author.isTrial(), author.getOthers()),
                event.getGuests().parallelStream().map(guest -> this.getUserInfos(guest.getId(), guest.getCivility(), guest.getFirstName(), guest.getLastName(), guest.getEmail(), guest.getPhone(), guest.getPhoneIndex(), guest.isTrial(), guest.getOthers())).collect(Collectors.toList()));

        final MessageProperties properties = new MessageProperties();
        properties.setHeader("application", "ZEEVEN");
        properties.setHeader("type", "message");
        properties.setHeader("message", applicationMessage.getText());
        final Gson gson = new Gson();
        final String jsonString = gson.toJson(notification);
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), properties));

    }

    public void sendPaymentConfirmationMessage(final Event event, final ApplicationMessage applicationMessage, final List<Channel> channelsToHandle) {
        log.info("ApplicationNotification du message de {}", applicationMessage.getId());

        final UserAccount author = this.profileService.findById(event.getAuthorId());

        final Map<String, List<String>> params = this.messageParameters(applicationMessage);
        params.put("trial", List.of(String.valueOf(author.isTrial())));
        final ApplicationNotification notification = new ApplicationNotification(
                "ZEEVEN",
                null,
                event.getName(),
                event.getId(),
                applicationMessage.getText(),
                params,
                channelsToHandle,
                this.getUserInfos(author.getId(), author.getCivility(), author.getFirstName(), author.getLastName(), author.getEmail(), author.getPhone(), author.getPhoneIndex(), author.isTrial(), null),
                event.getGuests().parallelStream().map(guest -> this.getUserInfos(guest.getId(), guest.getCivility(), guest.getFirstName(), guest.getLastName(), guest.getEmail(), guest.getPhone(), guest.getPhoneIndex(), guest.isTrial(), null)).collect(Collectors.toList()));

        final MessageProperties properties = new MessageProperties();
        properties.setHeader("application", "ZEEVEN");
        properties.setHeader("type", "message");
        final Gson gson = new Gson();
        final String jsonString = gson.toJson(notification);
        final ObjectMapper objectMapper = new ObjectMapper();
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), properties));

    }

    private MessageProfile getUserInfos(
            final String id,
            final Civility civility,
            final String firstName,
            final String lastName,
            final String email,
            final String phone,
            final String phoneIndex,
            final boolean trial,
            final List<ProfileParams> others
    ) {
        String finalCivility = "";
        if (civility != null) {
            finalCivility = civility.name();
        }
        return new MessageProfile(
                id,
                finalCivility,
                firstName,
                lastName,
                email,
                phoneIndex,
                phone,
                trial,
                others
        );
    }

    private Map<String, String> userAsMap(final Profile profile) {
        final Map<String, String> from = new HashMap();
        from.put("firstName", profile.getFirstName());
        from.put("lastName", profile.getLastName());
        from.put("email", profile.getEmail());
        from.put("phoneIndex", profile.getPhoneIndex());
        from.put("phone", profile.getPhone());
        return from;
    }

    private String messageAsString(final ApplicationMessage applicationMessage) {

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

    private Map<String, List<String>> messageParameters(final BaseApplicationMessage applicationMessage) {

        final String textWithVariables = applicationMessage.getText();
        final Pattern pattern = Pattern.compile("\\{\\{\\w+}}");
        final Matcher matcher = pattern.matcher(textWithVariables);
        int index = 0;
        final Map<String, List<String>> parameters = new HashMap<>();
        while (matcher.find()) {
            final String searchString = matcher.group();
            final String replacement = applicationMessage.getInformations().get(index);
            if (!searchString.equals(replacement)) {
                final String key = searchString
                        .replaceAll(Pattern.quote("{{"), Matcher.quoteReplacement(""))
                        .replaceAll(Pattern.quote("}}"), Matcher.quoteReplacement(""));
                if (parameters.containsKey(key)) {
                    parameters.put(
                            key,
                            Stream.of(parameters.get(key), List.of(replacement))
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toList())
                    );
                } else {
                    parameters.put(key, List.of(replacement));
                }
            }
            index++;
        }
        return parameters;
    }

    public void sendInvitationMessage(final Map<String, Object> messageParameters) throws JsonProcessingException {
        final MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("application", "ZEEVEN");
        messageProperties.setHeader("type", "invitation");

        final Gson gson = new Gson();
        final String jsonString = gson.toJson(messageParameters);


        this.rabbitTemplate.setExchange(this.applicationInvitationsExchange);
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), messageProperties));
    }

    public void sendFile(final Map<String, Object> messageParameters) {
        final MessageProperties messageProperties = new MessageProperties();
        final Gson gson = new Gson();
        final String jsonString = gson.toJson(messageParameters);
        this.rabbitTemplate.setExchange(this.applicationFilesExchange);
        this.rabbitTemplate.convertAndSend(new Message(jsonString.getBytes(), messageProperties));
    }
}
