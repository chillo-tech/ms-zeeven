package com.cs.ge.services.shared;

import com.cs.ge.dto.ApplicationNotification;
import com.cs.ge.dto.MessageProfile;
import com.cs.ge.entites.BaseApplicationMessage;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.notifications.entity.Notification;
import com.cs.ge.notifications.entity.Recipient;
import com.cs.ge.notifications.entity.Sender;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SharedService {
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static Map<String, List<Object>> messageParameters(final BaseApplicationMessage applicationMessage) {

        final String textWithVariables = applicationMessage.getText();
        final Pattern pattern = Pattern.compile("\\{\\{\\w+}}");
        final Matcher matcher = pattern.matcher(textWithVariables);
        int index = 0;
        final Map<String, List<Object>> parameters = new HashMap<>();
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

    public String toSlug(final String input) {
        final String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        final String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        final String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    public Notification applicationNotificationToNotification(final ApplicationNotification applicationNotification) {
        final Notification notification = new Notification();
        notification.setApplication(applicationNotification.getApplication());
        notification.setTemplate(applicationNotification.getTemplate());
        notification.setSubject(applicationNotification.getSubject());
        notification.setEventId(applicationNotification.getEventId());
        notification.setApplicationMessageId(applicationNotification.getApplicationMessageId());
        notification.setMessage(applicationNotification.getMessage());
        notification.setParams(applicationNotification.getParams());
        notification.setChannels(ImmutableSet.copyOf(applicationNotification.getChannels()));
        notification.setFrom(this.messageProfileToSender(applicationNotification.getFrom()));
        notification.setContacts(
                applicationNotification
                        .getContacts()
                        .stream()
                        .map((MessageProfile pro) -> this.messageProfileToRecipient(pro)).collect(Collectors.toSet())
        );
        return notification;
    }

    private Sender messageProfileToSender(final MessageProfile messageProfile) {
        final Sender sender = new Sender();
        sender.setId(messageProfile.getId());
        sender.setCivility(messageProfile.getCivility());
        sender.setEmail(messageProfile.getEmail());
        sender.setFirstName(messageProfile.getFirstName());
        sender.setLastName(messageProfile.getLastName());
        sender.setPhoneIndex(messageProfile.getPhoneIndex());
        sender.setPhone(messageProfile.getPhone());
        sender.setOthers(messageProfile.getOthers());
        return sender;
    }

    private Recipient messageProfileToRecipient(final MessageProfile messageProfile) {
        final Recipient recipient = new Recipient();
        recipient.setId(messageProfile.getId());
        recipient.setCivility(messageProfile.getCivility());
        recipient.setEmail(messageProfile.getEmail());
        recipient.setFirstName(messageProfile.getFirstName());
        recipient.setLastName(messageProfile.getLastName());
        recipient.setPhoneIndex(messageProfile.getPhoneIndex());
        recipient.setPhone(messageProfile.getPhone());
        recipient.setOthers(messageProfile.getOthers());
        return recipient;
    }

    public MessageProfile guestToMessageProfile(final Guest user) {
        String finalCivility = "";
        if (user.getCivility() != null) {
            finalCivility = user.getCivility().name();
        }
        return new MessageProfile(
                user.getId(),
                finalCivility,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneIndex(),
                user.getPhone(),
                user.isTrial(),
                user.getOthers()
        );
    }

    public MessageProfile userAccountToMessageProfile(final UserAccount user) {
        String finalCivility = "";
        if (user.getCivility() != null) {
            finalCivility = user.getCivility().name();
        }
        return new MessageProfile(
                user.getId(),
                finalCivility,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneIndex(),
                user.getPhone(),
                user.isTrial(),
                user.getOthers()
        );
    }

    public boolean isEmailValid(final String email) {
        final String pattern = "^(.+)@(\\S+)$";
        return Pattern.compile(pattern)
                .matcher(email)
                .matches();
    }


    public boolean isPhoneNumberValid(final String phoneNumber) {
        final String pattern = "\\d{10}|\\d{9}|(?:\\d{3}-){2}\\d{4}|\\(\\d{3}\\)\\d{3}-?\\d{4}";
        String phone = phoneNumber;
        if (Strings.isNullOrEmpty(phone)) {
            return false;
        }
        phone = phoneNumber.trim().replaceAll("null", "");
        return Pattern.compile(pattern)
                .matcher(phone)
                .matches();
    }
}
