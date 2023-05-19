package com.cs.ge.services.notifications;

import com.cs.ge.entites.ApplicationMessage;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.feign.FeignNotifications;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Async
@AllArgsConstructor
@Service
public class SynchroniousNotifications {
    FeignNotifications feignNotifications;

    @Async
    public void sendEventMessage(Event event, ApplicationMessage applicationMessage) {
        Map<String, String> from = new HashMap();
        from.put("firstName", event.getAuthor().getFirstName());
        from.put("lastName", event.getAuthor().getLastName());
        from.put("email", event.getAuthor().getEmail());
        from.put("phoneIndex", event.getAuthor().getPhoneIndex());
        from.put("phone", event.getAuthor().getPhone());
        String formattedMessage = SynchroniousNotifications.messageAsString(applicationMessage);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("message", formattedMessage);
        params.put("eventId", event.getId());
        this.feignNotifications.message(
                "ZEEVEN",
                event.getChannels().stream()
                        .map(Enum::name)
                        .collect(Collectors.toList()),
                new HashMap<String, Object>() {
                    {
                        this.put("subject", event.getName());
                        this.put("applicationName", "ZEEVEN");
                        this.put("params", params);
                        this.put("from", from);
                        this.put("template", "event-applicationMessage.html");
                        this.put("to", event.getGuests());
                    }
                }
        );
    }


    private static String messageAsString(ApplicationMessage applicationMessage) {

        String textWithVariables = applicationMessage.getText();
        Pattern pattern = Pattern.compile("\\{\\{\\w+}}");
        Matcher matcher = pattern.matcher(textWithVariables);
        int index = 0;
        while (matcher.find()) {
            String group = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            textWithVariables = StringUtils.replaceOnce(textWithVariables, group, applicationMessage.getInformations().get(index));
            index++;
        }
        return textWithVariables;
    }

    @Async
    public void sendConfirmationMessage(Event event) {
        UserAccount author = event.getAuthor();
        Map<String, String> to = new HashMap();
        if (author.getCivility() != null) {
            to.put("civility", author.getCivility().name());
        }
        to.put("firstName", author.getFirstName());
        to.put("lastName", author.getLastName());
        to.put("email", author.getEmail());
        to.put("phone", author.getPhone());
        to.put("phoneIndex", author.getPhoneIndex());

        Map<String, String> from = new HashMap();
        from.put("firstName", "Marlene");
        from.put("lastName", "DE ZEEVEN");
        from.put("email", "bonjour.zeeven@gmail.com");

        this.feignNotifications.message(
                "ZEEVEN",
                List.of("MAIL"),
                new HashMap<String, Object>() {{
                    this.put("subject", "Votre demande a bien enregistrée");
                    this.put("applicationName", "ZEEVEN");
                    this.put("from", from);
                    this.put("template", "confirmation.html");
                    this.put("to", Set.of(to));
                }}
        );

        Map<String, String> params = new HashMap();
        params.put("name", String.format("%s %s", author.getFirstName(), author.getLastName()));

        this.feignNotifications.message(
                "ZEEVEN",
                List.of("MAIL"),
                new HashMap<String, Object>() {{
                    this.put("subject", "Nouvel évènement");
                    this.put("applicationName", "ZEEVEN");
                    this.put("from", from);
                    this.put("template", "new-message.html");
                    this.put("params", params);
                    this.put("to", Set.of(from));
                }}
        );
    }

    @Async
    public void sendEmail(UserAccount author, String code) {
        Map<String, String> to = new HashMap();
        if (author.getCivility() != null) {
            to.put("civility", author.getCivility().name());
        }
        to.put("firstName", author.getFirstName());
        to.put("lastName", author.getLastName());
        to.put("email", author.getEmail());
        to.put("phone", author.getPhone());
        to.put("phoneIndex", author.getPhoneIndex());

        Map<String, String> from = new HashMap();
        from.put("firstName", "Marlene");
        from.put("lastName", "DE ZEEVEN");
        from.put("email", "bonjour.zeeven@gmail.com");

        this.feignNotifications.message(
                "ZEEVEN",
                List.of("MAIL"),
                new HashMap<String, Object>() {{
                    this.put("subject", "Activez votre compte");
                    this.put("application", "ZEEVEN");
                    this.put("from", from);
                    this.put("template", "activation.html");
                    this.put("to", Set.of(to));
                    this.put("params", new HashMap<String, Object>() {{
                        this.put("code", code);
                    }});
                }}
        );

        Map<String, String> params = new HashMap();
        params.put("name", String.format("%s %s", author.getFirstName(), author.getLastName()));

        this.feignNotifications.message(
                "ZEEVEN",
                List.of("MAIL"),
                new HashMap<String, Object>() {{
                    this.put("subject", "Nouveau compte");
                    this.put("application", "ZEEVEN");
                    this.put("from", from);
                    this.put("template", "new-account.html");
                    this.put("params", params);
                    this.put("to", Set.of(from));
                }}
        );
    }

}
