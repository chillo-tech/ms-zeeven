package com.cs.ge.services.notifications;

import com.cs.ge.dto.ApplicationNotification;
import com.cs.ge.entites.ApplicationMessage;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.Profile;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@AllArgsConstructor
@Slf4j
public class ASynchroniousNotifications {
    private final RabbitTemplate rabbitTemplate;

    public void notify(final String message) {
        log.info("ApplicationNotification du message de {}", message);
        this.rabbitTemplate.convertAndSend(message);
    }

    public void sendEventMessage(Event event, ApplicationMessage applicationMessage) {
        log.info("ApplicationNotification du message de {}", applicationMessage.getText());

        String formattedMessage = this.messageAsString(applicationMessage);
        ApplicationNotification notification = new ApplicationNotification(
                "ZEEVEN",
                null,
                event.getName(),
                event.getId(),
                applicationMessage.getText(),
                messageParameters(applicationMessage),
                event.getChannels(),
                event.getAuthor(),
                event.getGuests()
        );

        MessageProperties properties = new MessageProperties();
        properties.setHeader("application", "ZEEVEN");
        properties.setHeader("type", "message");
        /*
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.rabbitTemplate.convertAndSend(new Message(objectMapper.writeValueAsBytes(notification), properties));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

         */
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
