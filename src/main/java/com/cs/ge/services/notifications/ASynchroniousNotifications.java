package com.cs.ge.services.notifications;

import com.cs.ge.dto.ApplicationNotification;
import com.cs.ge.entites.ApplicationMessage;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Profile;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.services.StockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@AllArgsConstructor
@Slf4j
public class ASynchroniousNotifications {
    private final RabbitTemplate rabbitTemplate;
    private final StockService stockService;

    public void notify(final String message) {
        log.info("ApplicationNotification du message de {}", message);
        this.rabbitTemplate.convertAndSend(message);
    }


    public void sendEmail(UserAccount author, String code) {
        Guest to = new Guest();
        if (author.getCivility() != null) {
            to.setCivility(author.getCivility());
        }
        to.setFirstName(author.getFirstName());
        to.setLastName(author.getLastName());
        to.setEmail(author.getEmail());
        to.setPhone(author.getPhone());
        to.setPhoneIndex(author.getPhoneIndex());


        Guest exp = new Guest();
        exp.setLastName("DE ZEEVEN");
        exp.setFirstName("Marlene");
        exp.setEmail("bonjour.zeeven@gmail.com");

        ApplicationNotification notification = new ApplicationNotification(
                "ZEEVEN",
                "activation.html",
                "Activez votre compte",
                RandomStringUtils.random(8, true, true),
                null,
                new HashMap<String, String>() {{
                    this.put("code", code);
                }},
                List.of(Channel.EMAIL),
                exp,
                List.of(to)
        );

        MessageProperties properties = new MessageProperties();
        properties.setHeader("application", "ZEEVEN");
        properties.setHeader("type", "notification");
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.rabbitTemplate.convertAndSend(new Message(objectMapper.writeValueAsBytes(notification), properties));

            notification = new ApplicationNotification(
                    "ZEEVEN",
                    "new-account.html",
                    "Nouveau compte",
                    RandomStringUtils.random(8, true, true),
                    null,
                    new HashMap<String, String>() {{
                        this.put("name", String.format("%s %s", author.getFirstName(), author.getLastName()));
                    }},
                    List.of(Channel.EMAIL),
                    exp,
                    List.of(exp)
            );
            this.rabbitTemplate.convertAndSend(new Message(objectMapper.writeValueAsBytes(notification), properties));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    public void sendEventMessage(Event event, ApplicationMessage applicationMessage, List<Channel> channelsToHandle) {
        log.info("ApplicationNotification du message de {}", applicationMessage.getId());

        UserAccount author = event.getAuthor();
        Map<String, String> params = messageParameters(applicationMessage);
        params.put("trial", String.valueOf(author.isTrial()));
        ApplicationNotification notification = new ApplicationNotification(
                "ZEEVEN",
                null,
                event.getName(),
                event.getId(),
                applicationMessage.getText(),
                params,
                channelsToHandle,
                event.getAuthor(),
                event.getGuests()
        );

        MessageProperties properties = new MessageProperties();
        properties.setHeader("application", "ZEEVEN");
        properties.setHeader("type", "message");
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.rabbitTemplate.convertAndSend(new Message(objectMapper.writeValueAsBytes(notification), properties));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private Map<String, String> userAsMap(Profile profile) {
        Map<String, String> from = new HashMap();
        from.put("firstName", profile.getFirstName());
        from.put("lastName", profile.getLastName());
        from.put("email", profile.getEmail());
        from.put("phoneIndex", profile.getPhoneIndex());
        from.put("phone", profile.getPhone());
        return from;
    }

    private String messageAsString(ApplicationMessage applicationMessage) {

        String textWithVariables = applicationMessage.getText();
        Pattern pattern = Pattern.compile("\\{\\{\\w+}}");
        Matcher matcher = pattern.matcher(textWithVariables);
        int index = 0;
        while (matcher.find()) {
            String searchString = matcher.group();
            String replacement = applicationMessage.getInformations().get(index);
            int start = matcher.start();
            int end = matcher.end();
            textWithVariables = StringUtils.replaceOnce(textWithVariables, searchString, replacement);
            index++;
        }
        return textWithVariables;
    }

    private Map<String, String> messageParameters(ApplicationMessage applicationMessage) {

        String textWithVariables = applicationMessage.getText();
        Pattern pattern = Pattern.compile("\\{\\{\\w+}}");
        Matcher matcher = pattern.matcher(textWithVariables);
        int index = 0;
        Map<String, String> parameters = new HashMap<>();
        while (matcher.find()) {
            String searchString = matcher.group();
            String replacement = applicationMessage.getInformations().get(index);
            if (!searchString.equals(replacement)) {
                parameters.put(
                        searchString.replaceAll(Pattern.quote("{{"), Matcher.quoteReplacement(""))
                                .replaceAll(Pattern.quote("}}"), Matcher.quoteReplacement("")),
                        replacement
                );
            }
            index++;
        }
        return parameters;
    }

}
