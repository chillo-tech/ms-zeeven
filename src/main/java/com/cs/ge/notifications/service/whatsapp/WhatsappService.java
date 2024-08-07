package com.cs.ge.notifications.service.whatsapp;

import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Invitation;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.notifications.entity.*;
import com.cs.ge.notifications.entity.template.Template;
import com.cs.ge.notifications.entity.template.TemplateComponent;
import com.cs.ge.notifications.entity.template.TemplateExample;
import com.cs.ge.notifications.entity.template.WhatsAppTemplate;
import com.cs.ge.notifications.repository.NotificationTemplateRepository;
import com.cs.ge.notifications.repository.TemplateRepository;
import com.cs.ge.notifications.repository.TemplateStatusRepository;
import com.cs.ge.notifications.service.NotificationMapper;
import com.cs.ge.notifications.service.whatsapp.dto.Component;
import com.cs.ge.notifications.service.whatsapp.dto.Image;
import com.cs.ge.notifications.service.whatsapp.dto.*;
import com.cs.ge.services.shared.SharedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cs.ge.enums.Channel.WHATSAPP;
import static com.cs.ge.notifications.data.ApplicationData.FOOTER_TEXT;
import static com.cs.ge.notifications.enums.TemplateCategory.UTILITY;
import static com.cs.ge.notifications.enums.TemplateComponentType.BODY;
import static com.cs.ge.notifications.enums.TemplateComponentType.FOOTER;

@Slf4j
@Service
public class WhatsappService extends NotificationMapper {

    private final TemplateRepository templateRepository;
    private final TemplateStatusRepository templateStatusRepository;
    private final String recipient;
    private final TextMessageService textMessageService;
    private final TemplateMessageService templateMessageService;
    private final NotificationTemplateRepository notificationTemplateRepository;
    private final SharedService sharedService;

    public WhatsappService(
            final TemplateRepository templateRepository, @Value("${application.recipient.sms:#{null}}") final String recipient,
            final TemplateStatusRepository templateStatusRepository,
            final TextMessageService textMessageService,
            final TemplateMessageService templateMessageService,
            final NotificationTemplateRepository notificationTemplateRepository, final SharedService sharedService
    ) {
        super(notificationTemplateRepository);
        this.templateRepository = templateRepository;
        this.templateStatusRepository = templateStatusRepository;
        this.recipient = recipient;
        this.textMessageService = textMessageService;
        this.templateMessageService = templateMessageService;
        this.notificationTemplateRepository = notificationTemplateRepository;
        this.sharedService = sharedService;
    }

    private static BufferedImage createImageFromBytes(final String image) {
        try {
            final byte[] bytes = Base64.getDecoder().decode(image);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (final IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void convertHtmlToImage(final String qr, final String htmlSource) {
        final BufferedImage bufferedImage = new BufferedImage(20000, 2500, BufferedImage.TYPE_INT_RGB);

        final Graphics2D g2d = bufferedImage.createGraphics();
        final Font font = new Font("Ticketing", Font.PLAIN, 120);

        g2d.setFont(font);
        g2d.setColor(Color.DARK_GRAY);
        final BufferedImage qrCode = WhatsappService.createImageFromBytes(qr);
        g2d.drawImage(qrCode, 0, 0, null);

        g2d.drawString("lkhpfe", 1550, 400);
        g2d.drawString("pk^^zjf^^j^", 1550, 800);
        g2d.drawString("jfpzhjpzozj", 1550, 1100);
        g2d.drawString("ko^^jz^^zj", 1550, 1400);
        g2d.dispose();
        try {
            ImageIO.write(bufferedImage, "png", new File("/Users/chillo/projets/zeeven/data/tickets/test.png"));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private List<Parameter> getBodyComponentParameters(final String notificationTemplate, final UserAccount sender, final Guest to, final String eventName, final List<String> mappedSchedules, final com.cs.ge.entites.Template template) {
        if (notificationTemplate.equals("ze_weeding_invitation")) {
            return List.of(
                    new Parameter(
                            "text",
                            this.formatName(to.getCivility(), to.getFirstName(), to.getLastName()).trim(),
                            null
                    ),
                    new Parameter(
                            "text",
                            eventName,
                            null
                    ),
                    new Parameter(
                            "text",
                            String.format(
                                    "%s",
                                    String.join(" | ", mappedSchedules)
                            ),
                            null
                    ),
                    new Parameter(
                            "text",
                            this.formatName(sender.getCivility(), sender.getFirstName(), sender.getLastName()).trim(),
                            null
                    )
            );
        }
        return List.of(
                new Parameter(
                        "text",
                        this.formatName(to.getCivility(), to.getFirstName(), to.getLastName()).trim(),
                        null
                ),
                new Parameter(
                        "text",
                        eventName,
                        null
                ),
                new Parameter(
                        "text",
                        String.format(
                                "%s",
                                String.join(" | ", mappedSchedules)
                        ),
                        null
                ),
                new Parameter(
                        "text",
                        template.getAddress(),
                        null
                ),
                new Parameter(
                        "text",
                        this.formatName(null, sender.getFirstName(), sender.getLastName()).trim(),
                        null
                )
        );
    }

    private List<NotificationStatus> disabledAccountComponents(final Notification notification) {

        final String templateName = "ze_test_template";
        return notification.getContacts().stream().map((final Recipient to) -> {
            final NotificationStatus notificationStatus = this.getNotificationStatus(
                    notification,
                    to.getId(),
                    WHATSAPP,
                    "WHATSAPP", //createdMessage.getSid(),
                    "SENT" //createdMessage.getStatus().name()
            );
            String phoneNumber = this.recipient;
            if (phoneNumber == null) {
                phoneNumber = String.format("+%s%s", to.getPhoneIndex(), to.getPhone());
            }
            try {
                if (this.sharedService.isPhoneNumberValid(to.getPhone())) {
                    final Component component = new Component();
                    component.setType("body");
                    final List<Parameter> parameters = List.of(
                            new Parameter("text", String.format("%s %s", notification.getFrom().getFirstName(), notification.getFrom().getLastName().toUpperCase()), null)
                    );
                    component.setParameters(parameters);

                    final WhatsappTemplate template = new WhatsappTemplate();
                    template.setName(templateName);
                    template.setLanguage(new Language("en"));
                    template.setComponents(List.of(component));

                    final TextMessage textMessage = new TextMessage();
                    textMessage.setTemplate(template);
                    textMessage.setMessaging_product("whatsapp");
                    textMessage.setType("template");

                    textMessage.setTo(String.format("+%s", phoneNumber));
                    final WhatsAppResponse response = this.textMessageService.message(textMessage);
                    notificationStatus.setProvider(response.getMessages().get(0).getId());
                    return notificationStatus;
                } else {
                    log.info("Aucun envoi pour {} {} son téléphone {}{} est null ou invalide", to.getFirstName(), to.getLastName(), to.getPhoneIndex(), to.getPhone());
                    notificationStatus.setStatus("NOT_SEND");
                }
            } catch (final Exception e) {
                log.error("ERROR", e);
                notificationStatus.setStatus("ERROR");
            }
            return notificationStatus;
        }).collect(Collectors.toList());
    }

    @Async
    public List<NotificationStatus> sendText(final Notification notification) {
        return notification.getContacts().parallelStream().map((final Recipient to) -> {
            final String messageToSend = String.valueOf(this.map(notification, to, WHATSAPP).get("message"));
            String phoneNumber = this.recipient;
            if (phoneNumber == null) {
                phoneNumber = String.format("+%s%s", to.getPhoneIndex(), to.getPhone());
            }

            final TextMessage textMessage = new TextMessage();
            textMessage.setMessaging_product("whatsapp");
            textMessage.setRecipient_type("individual");
            textMessage.setTo(phoneNumber);
            textMessage.setType("text");
            textMessage.setTo(phoneNumber);
            textMessage.setText(new Text(false, messageToSend));
            final WhatsAppResponse response = this.textMessageService.message(textMessage);
            return this.getNotificationStatus(
                    notification,
                    to.getId(),
                    WHATSAPP,
                    response.getMessages().get(0).getId(), //createdMessage.getSid(),
                    "SENT" //createdMessage.getStatus().name()
            );
        }).collect(Collectors.toList());
    }

    public List<NotificationStatus> send(final Notification notification) {
        try {
            if (notification.getFrom().isTrial()) {
                return this.disabledAccountComponents(notification);
            } else {
                return this.activeAccountComponents(notification);
            }
        } catch (final Exception e) {
            WhatsappService.log.error("ERREUR LORS DE L'ENVOI d'un message", e);
            e.printStackTrace();
        }
        return new ArrayList<>();

    }

    private List<NotificationStatus> activeAccountComponents(final Notification notification) {
        final String templateName = "ze_say_hello";
        final Template templateInBDD = this.templateRepository.findByName(templateName);
        return notification.getContacts().stream().map((final Recipient to) -> {
            final NotificationStatus notificationStatus = this.getNotificationStatus(
                    notification,
                    to.getId(),
                    WHATSAPP,
                    WHATSAPP.name(),
                    "SENT" //createdMessage.getStatus().name()
            );
            String phoneNumber = this.recipient;
            if (phoneNumber == null) {
                phoneNumber = String.format("%s%s", to.getPhoneIndex(), to.getPhone());
            }

            try {
                if (this.sharedService.isPhoneNumberValid(to.getPhone())) {
                    final Component component = new Component();
                    component.setType("body");
                    final Map<String, String> params = (Map<String, String>) this.map(notification, to, WHATSAPP).get("params");
                    final Map<Integer, String> templateInBDDParams = templateInBDD.getWhatsAppMapping();
                    final List<Parameter> parameters = templateInBDDParams.keySet()
                            .stream().map(param -> new Parameter("text", params.get(templateInBDDParams.get(param)), null))
                            .collect(Collectors.toList());
                    component.setParameters(parameters);

                    final WhatsappTemplate template = new WhatsappTemplate();
                    template.setName(templateName);
                    template.setLanguage(new Language("fr"));
                    template.setComponents(List.of(component));

                    final TextMessage textMessage = new TextMessage();
                    textMessage.setTemplate(template);
                    textMessage.setMessaging_product("whatsapp");
                    textMessage.setType("template");
                    textMessage.setTo(String.format("+%s", phoneNumber));

                    final WhatsAppResponse response = this.textMessageService.message(textMessage);
                    notificationStatus.setProvider(response.getMessages().get(0).getId());
                    return notificationStatus;
                } else {
                    log.info("Aucun envoi pour {} {} son téléphone {}{} est null ou invalide", to.getFirstName(), to.getLastName(), to.getPhoneIndex(), to.getPhone());
                    notificationStatus.setStatus("NOT_SEND");
                }
            } catch (final Exception e) {
                log.error("ERROR", e);
                notificationStatus.setStatus("ERROR");
            }
            return notificationStatus;
        }).collect(Collectors.toList());
    }

    public WhatsAppResponse createTemplate(final Template templateInBDD) {
        List<TemplateComponent> components = templateInBDD.getComponents();
        components.add(
                new TemplateComponent(
                        FOOTER,
                        null,
                        FOOTER_TEXT,
                        null,
                        null
                )
        );
        components = components.stream().peek(templateComponent -> {
            if (templateComponent.getType().equals(BODY)) {
                String text = templateComponent.getText();
                final Map<Integer, String> mappings = templateInBDD.getWhatsAppMapping();
                for (final Integer key : mappings.keySet()) {
                    text = text.replace(mappings.get(key), "" + key);
                }
                text = text.replaceAll("\\*\\*", "_");
                templateComponent.setText(text);
                final TemplateExample templateExample = new TemplateExample();
                templateExample.setBody_text(List.of(mappings.keySet().stream().map(key -> mappings.get(key)).toList()));
                templateComponent.setExample(templateExample);
            }
        }).toList();

        final WhatsAppTemplate whatsAppTemplate = new WhatsAppTemplate(
                templateInBDD.getName(),
                null,
                true,
                "fr",
                WHATSAPP,
                UTILITY,
                components
        );
        final WhatsAppResponse whatsAppResponse = this.templateMessageService.template(whatsAppTemplate);
        WhatsappService.log.info("{}", whatsAppResponse);
        this.templateStatusRepository.save(
                new TemplateStatus(
                        null,
                        templateInBDD.getName(),
                        whatsAppResponse.getId(),
                        templateInBDD.getId(),
                        whatsAppResponse.getStatus(),
                        whatsAppResponse.getCategory(),
                        Instant.now()
                )
        );
        return whatsAppResponse;
    }

    public List<NotificationStatus> sendFromParams(final String eventName, final Invitation invitation, final Guest to, final Map<String, Object> notificationParams, final Channel channel) {
        final com.cs.ge.entites.Template template = invitation.getTemplate();
        final String whatsappTemplateName = (String) notificationParams.get("whatsappTemplateName");

        final UserAccount sender = (UserAccount) notificationParams.get("author");

        final List<String> mappedSchedules = this.getMappedSchedules(template);

        final Component headerCompoenent = new Component();
        headerCompoenent.setType("header");
        final List<Parameter> headerComponentParameters = List.of(
                new Parameter(
                        "image",
                        null,
                        Image.builder().link(String.valueOf(notificationParams.get("image"))).build()
                ));
        headerCompoenent.setParameters(headerComponentParameters);

        final Component bodyComponent = new Component();
        bodyComponent.setType("body");
        final List<Parameter> bodyComponentParameters = this.getBodyComponentParameters(whatsappTemplateName, sender, to, eventName, mappedSchedules, template);
        bodyComponent.setParameters(bodyComponentParameters);


        final WhatsappTemplate whatsappTemplate = new WhatsappTemplate();
        whatsappTemplate.setName(whatsappTemplateName);
        whatsappTemplate.setLanguage(new Language("fr"));
        whatsappTemplate.setComponents(List.of(headerCompoenent, bodyComponent));

        final TextMessage textMessage = new TextMessage();
        textMessage.setMessaging_product("whatsapp");
        textMessage.setRecipient_type("individual");
        String phoneNumber = this.recipient;
        if (phoneNumber == null) {
            phoneNumber = String.format("+%s%s", to.getPhoneIndex(), to.getPhone());
        }
        textMessage.setTo(phoneNumber);
        textMessage.setType("template");
        textMessage.setTemplate(whatsappTemplate);

        final WhatsAppResponse whatsAppResponse = this.textMessageService.message(textMessage);

        final NotificationStatus notificationStatus = new NotificationStatus();
        notificationStatus.setEventId(String.format("%s", notificationParams.get("eventId")));
        notificationStatus.setUserId(to.getId());
        notificationStatus.setChannel(channel);
        notificationStatus.setPhone(phoneNumber);
        notificationStatus.setProviderNotificationId(whatsAppResponse.getMessages().get(0).getId());
        notificationStatus.setStatus(whatsAppResponse.getMessages().get(0).getMessage_status());
        notificationStatus.setCreation(Instant.now());
        return List.of(notificationStatus);
    }

    protected String processTemplate(final String application, final String template, final Map<String, List<Object>> params) {
        final String messageToSend;
        final NotificationTemplate notificationTemplate = this.notificationTemplateRepository
                .findByApplicationAndNameAndTypeIn(application, template, List.of(WHATSAPP))
                .orElseThrow(() -> new IllegalArgumentException(String.format("Aucun template %s n'existe pour %s", template, application)));
        messageToSend = this.processTemplate(params, notificationTemplate.getContent(), WHATSAPP);
        return messageToSend;
    }
}
