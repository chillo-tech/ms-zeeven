package com.cs.ge.services.notifications;

import com.cs.ge.entites.ApplicationMessage;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.feign.FeignNotifications;
import com.cs.ge.services.ProfileService;
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
    ProfileService profileService;

    @Async
    public void sendEventMessage(final Event event, final ApplicationMessage applicationMessage) {
        final Map<String, String> from = new HashMap();
        from.put("firstName", ""); //event.getAuthor().getFirstName());
        from.put("lastName", ""); //event.getAuthor().getLastName());
        from.put("email", ""); //event.getAuthor().getEmail());
        from.put("phoneIndex", ""); //event.getAuthor().getPhoneIndex());
        from.put("phone", ""); // event.getAuthor().getPhone());
        final String formattedMessage = SynchroniousNotifications.messageAsString(applicationMessage);
        final Map<String, Object> params = new HashMap<String, Object>();
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
                        this.put("contacts", event.getGuests());
                    }
                }
        );
    }

    private static String messageAsString(final ApplicationMessage applicationMessage) {

        String textWithVariables = applicationMessage.getText();
        final Pattern pattern = Pattern.compile("\\{\\{\\w+}}");
        final Matcher matcher = pattern.matcher(textWithVariables);
        int index = 0;
        while (matcher.find()) {
            final String group = matcher.group();
            final int start = matcher.start();
            final int end = matcher.end();
            textWithVariables = StringUtils.replaceOnce(textWithVariables, group, applicationMessage.getInformations().get(index));
            index++;
        }
        return textWithVariables;
    }

    @Async
    public void sendConfirmationMessage(final Event event) {
        final UserAccount author = this.profileService.findById(event.getAuthorId());
        final Map<String, String> to = new HashMap();
        if (author.getCivility() != null) {
            to.put("civility", author.getCivility().name());
        }
        to.put("firstName", author.getFirstName());
        to.put("lastName", author.getLastName());
        to.put("email", author.getEmail());
        to.put("phone", author.getPhone());
        to.put("phoneIndex", author.getPhoneIndex());

        final Map<String, String> from = new HashMap();
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
                    this.put("contacts", Set.of(to));
                }}
        );

        final Map<String, String> params = new HashMap();
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
                    this.put("contacts", Set.of(from));
                }}
        );
    }

    @Async
    public void sendEmail(final UserAccount author, final String code) {
        final Map<String, String> to = new HashMap();
        if (author.getCivility() != null) {
            to.put("civility", author.getCivility().name());
        }
        to.put("firstName", author.getFirstName());
        to.put("lastName", author.getLastName());
        to.put("email", author.getEmail());
        to.put("phone", author.getPhone());
        to.put("phoneIndex", author.getPhoneIndex());

        final Map<String, String> from = new HashMap();
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
                    this.put("template", "ze_activation.html");
                    this.put("to", Set.of(to));
                    this.put("contacts", Set.of(to));
                    this.put("params", new HashMap<String, Object>() {{
                        this.put("code", code);
                    }});
                }}
        );

        final Map<String, String> params = new HashMap();
        params.put("name", String.format("%s %s", author.getFirstName(), author.getLastName()));

        this.feignNotifications.message(
                "ZEEVEN",
                List.of("MAIL"),
                new HashMap<String, Object>() {{
                    this.put("subject", "Nouveau compte");
                    this.put("application", "ZEEVEN");
                    this.put("from", from);
                    this.put("template", "ze_new-account.html");
                    this.put("params", params);
                    this.put("to", Set.of(from));
                    this.put("contacts", Set.of(from));
                }}
        );
    }

}
