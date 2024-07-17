package com.cs.ge.notifications.service;

import com.cs.ge.entites.Schedule;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.Civility;
import com.cs.ge.notifications.entity.Notification;
import com.cs.ge.notifications.entity.NotificationStatus;
import com.cs.ge.notifications.entity.NotificationTemplate;
import com.cs.ge.notifications.entity.Recipient;
import com.cs.ge.notifications.repository.NotificationTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Strings;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.cs.ge.notifications.data.ApplicationData.CIVILITY_MAPPING;

@Slf4j
public abstract class NotificationMapper {
    private final NotificationTemplateRepository notificationTemplateRepository;

    public NotificationMapper(final NotificationTemplateRepository notificationTemplateRepository) {
        this.notificationTemplateRepository = notificationTemplateRepository;
    }

    protected NotificationStatus getNotificationStatus(final Notification notification, final String userId, final Channel channel, final String providerId, final String status) {
        final NotificationStatus notificationStatus = new NotificationStatus();
        notificationStatus.setEventId(notification.getEventId());
        notificationStatus.setLocalNotificationId(notification.getId());
        notificationStatus.setUserId(userId);
        notificationStatus.setApplicationMessageId(notification.getApplicationMessageId());
        notificationStatus.setChannel(channel);
        notificationStatus.setProviderNotificationId(providerId);
        notificationStatus.setStatus(status);
        notificationStatus.setCreation(Instant.now());
        return notificationStatus;
    }

    private String processTemplateWithValues(final Map<String, Object> model, final String template) {

        try {
            String templateHoHandle = template;
            final Matcher m = Pattern.compile("\\$\\{(.*?)}").matcher(template);
            while (m.find()) {
                final String initialVariable = m.group(1);
                final String finalVariable = initialVariable.replaceAll("\\s+", "");
                templateHoHandle = templateHoHandle.replaceAll(initialVariable, finalVariable);
            }

            final Template t = new Template("TemplateFromDBName", templateHoHandle, null);
            final Writer out = new StringWriter();
            t.process(model, out);
            return out.toString();

        } catch (final TemplateException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected Map<String, Object> map(final Notification notification, final Recipient to, final Channel channel) {
        try {

            // Paramètres transmis pour le message
            final Map<String, List<Object>> params = new HashMap<>();

            if (notification.getParams() != null) {
                params.putAll(notification.getParams());
            }

            if (to.getOthers() != null && !to.getOthers().isEmpty()) {
                final Map<String, List<Object>> othersParams = new HashMap<>();
                to.getOthers().forEach(other -> othersParams.put(other.getLabel().replaceAll("\\s+", ""), List.of(other.getValue())));
                for (final String otherParamKey : othersParams.keySet()) {
                    params.put(otherParamKey, othersParams.get(otherParamKey));
                }
            }

            final Object message = params.get("message");

            // Informations de l'utilisateur dans le template
            if (message != null) {
                String messageAsString = message.toString();
                final BeanInfo beanInfo = Introspector.getBeanInfo(Recipient.class);
                // Traitement de chaque propriété
                for (final PropertyDescriptor propertyDesc : beanInfo.getPropertyDescriptors()) {
                    final String propertyName = propertyDesc.getName();
                    final Object value = propertyDesc.getReadMethod().invoke(to);
                    if (!Strings.isNullOrEmpty(String.valueOf(value)) && value instanceof String) {
                        if (Objects.equals(propertyName, "phone")) {
                            messageAsString = messageAsString.replace(String.format("%s%s%s", "{{", propertyName, "}}"), String.format("00%s%s", to.getPhoneIndex(), to.getPhone()));
                        } else {
                            messageAsString = messageAsString.replace(String.format("%s%s%s", "{{", propertyName, "}}"), (CharSequence) value);
                        }
                    }
                }
                params.put("message", List.of(messageAsString));
            }
            params.put("firstName", List.of(to.getFirstName()));
            params.put("lastName", List.of(to.getLastName()));
            params.put("email", List.of(to.getEmail()));
            params.put("sender", List.of(this.formatName(null, notification.getFrom().getFirstName(), notification.getFrom().getLastName())));
            if (!Strings.isNullOrEmpty(to.getPhone())) {
                params.put("phone", List.of(to.getPhone()));
            }
            if (!Strings.isNullOrEmpty(notification.getSubject())) {
                params.put("eventName", List.of(notification.getSubject()));
            }
            if (!Strings.isNullOrEmpty(notification.getImage())) {
                params.put("image", List.of(notification.getImage()));
            }
            if (!Strings.isNullOrEmpty(notification.getDate())) {
                params.put("date", List.of(notification.getDate()));
            }
            if (!Strings.isNullOrEmpty(to.getPhoneIndex())) {
                params.put("phoneIndex", List.of(to.getPhoneIndex()));
            }
            if (to.getCivility() != null) {
                params.put("civility", List.of(to.getCivility()));
            }
            String messageToSend = notification.getMessage();

            if (!Strings.isNullOrEmpty(notification.getTemplate())) {
                final NotificationTemplate notificationTemplate = this.notificationTemplateRepository
                        .findByApplicationAndNameAndTypeIn(notification.getApplication(), notification.getTemplate(), List.of(channel))
                        .orElseThrow(() -> new IllegalArgumentException(String.format("Aucun template %s n'existe pour %s", notification.getTemplate(), notification.getApplication())));
                //final String template = this.textTemplateEngine.process(notificationTemplate.getContent(), context);
                messageToSend = this.processTemplate(params, notificationTemplate.getContent(), channel);
            } else {
                messageToSend = messageToSend.replaceAll(Pattern.quote("{{"), Matcher.quoteReplacement("${"))
                        .replaceAll(Pattern.quote("}}"), Matcher.quoteReplacement("}"));
                messageToSend = this.processTemplate(params, messageToSend, channel);
            }
            return Map.of("message", messageToSend, "params", params);
        } catch (final IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String formatName(final Civility civility, final String firstName, final String lastName) {
        String name = "";

        if (!Strings.isNullOrEmpty(firstName)) {
            name = String.format("%s%s%s", name, String.valueOf(firstName.charAt(0)).toUpperCase(), firstName.substring(1).toLowerCase());
        }

        if (!Strings.isNullOrEmpty(lastName)) {
            name = String.format("%s %s", name, lastName.toUpperCase());
        }

        if (civility != null && CIVILITY_MAPPING.get(civility) != null && !Strings.isNullOrEmpty(CIVILITY_MAPPING.get(civility))) {
            name = String.format("%s %s", CIVILITY_MAPPING.get(civility), name);
        }

        return name;
    }

    protected String processTemplate(final Map<String, List<Object>> model, final String template, final Channel channel) {
        final Map<String, Object> oneItemMap = new HashMap<>();
        final Map<String, List<Object>> moreThanOneItemMap = new HashMap<>();
        model.keySet()
                .forEach(key -> {
                    if (model.get(key).size() == 1) {
                        oneItemMap.put(key, model.get(key).get(0));
                    } else {
                        oneItemMap.put(key, "${" + key + "}");
                    }
                });
        model.keySet()
                .stream()
                .filter(key -> model.get(key).size() > 1)
                .forEach(key -> moreThanOneItemMap.put(key, model.get(key)));

        final String[] parsedTemplate = {this.processTemplateWithValues(oneItemMap, template)};
        moreThanOneItemMap.keySet().forEach(key -> {
            final List<Object> values = moreThanOneItemMap.get(key);
            for (final Object replacement : values) {
                String templateValue = (String) replacement;
                if (channel.equals(Channel.EMAIL) && key.equals("link")) {
                    templateValue = String.format("<a href='%s'>%s</a>", templateValue, templateValue);
                }
                parsedTemplate[0] = parsedTemplate[0].replaceFirst(String.format("%s%s%s", "\\$\\{", key, "}"), templateValue);
            }
        });
        return parsedTemplate[0];
    }

    protected List<String> getMappedSchedules(final com.cs.ge.entites.Template template) {
        final Set<Schedule> schedules = template.getSchedules();

        final ObjectMapper oMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return schedules.stream().map(schedule -> {
            final Map<String, Object> map = oMapper.convertValue(schedule, Map.class);
            final String invitationDate = String.valueOf(map.get("date"));

            final LocalDateTime dateTime = LocalDateTime.parse(invitationDate.substring(0, invitationDate.indexOf('Z')));
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return dateTime.format(formatter); // "1986-04-08 12:30"
        }).collect(Collectors.toList());

    }

}
