package com.cs.ge.services;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.*;
import com.cs.ge.enums.QRCodeType;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.services.graphics.GraphicsService;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.cs.ge.services.qrcode.QRCodeGeneratorService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cs.ge.utils.Data.CIVILITY_MAPPING;
import static com.cs.ge.utils.Data.IMAGE_FORMAT;

@Slf4j
@Service
public class InvitationService {
    private final EventRepository eventsRepository;
    private final QRCodeGeneratorService qrCodeGeneratorService;
    private final ProfileService profileService;
    private final ASynchroniousNotifications aSynchroniousNotifications;
    private final String applicationFilesHost;
    private final GraphicsService graphicsService;
    private int nbInvitations;


    public InvitationService(
            final EventRepository eventsRepository,
            final QRCodeGeneratorService qrCodeGeneratorService,
            final ProfileService profileService, final ASynchroniousNotifications aSynchroniousNotifications,
            @Value("${app.images-host}") final String applicationFilesHost,
            @Value("${app.invitations.limit:0}") final int nbInvitations,
            final GraphicsService graphicsService) {
        this.eventsRepository = eventsRepository;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.profileService = profileService;
        this.aSynchroniousNotifications = aSynchroniousNotifications;
        this.applicationFilesHost = applicationFilesHost;
        this.graphicsService = graphicsService;
        this.nbInvitations = nbInvitations;
    }

    private static String generateTicket(final Event event, final Guest guest, final Invitation invitation,
                                         final String qrcodeAsString) {
        try {

            int ticketWidth = 2000; //ticketTemplate.getWidth();
            int ticketHeight = 647; //ticketTemplate.getHeight();
            int qrCodeWidth = 100;
            int qrCodeHeight = 100;
            int qrCodeX = ticketWidth / 2;
            int qrCodeY = ticketHeight / 2;
            int nameX = ticketWidth / 2;
            int nameY = ticketHeight / 2;

            final Template template = invitation.getTemplate();

            if (template.getParams() != null && !template.getParams().isEmpty()) {
                final Map<String, String> params = template.getParams();
                if (params.containsKey("ticketWidth")) {
                    ticketWidth = Integer.parseInt(params.get("ticketWidth"));
                }

                if (params.containsKey("ticketHeight")) {
                    ticketHeight = Integer.parseInt(params.get("ticketHeight"));
                }

                if (params.containsKey("qrCodeWidth")) {
                    qrCodeWidth = Integer.parseInt(params.get("qrCodeWidth"));
                }

                if (params.containsKey("qrCodeHeight")) {
                    qrCodeHeight = Integer.parseInt(params.get("qrCodeHeight"));
                }
                if (params.containsKey("qrCodeX")) {
                    qrCodeX = Integer.parseInt(params.get("qrCodeX"));
                }
                if (params.containsKey("qrCodeY")) {
                    qrCodeY = Integer.parseInt(params.get("qrCodeY"));
                }
                if (params.containsKey("nameX")) {
                    nameX = Integer.parseInt(params.get("nameX"));
                }
                if (params.containsKey("nameY")) {
                    nameY = Integer.parseInt(params.get("nameY"));
                }

            }
            BufferedImage ticketTemplate = null; //ImageIO.read(new File("horizontal-template.png"));

            final byte[] bytes = Base64.getDecoder().decode(qrcodeAsString);
            final BufferedImage qrcode = ImageIO.read(new ByteArrayInputStream(bytes));

            if (template != null && !Strings.isNullOrEmpty(template.getFile())) {
                String imageFromFile = event.getInvitation().getTemplate().getFile();
                if (!Strings.isNullOrEmpty(imageFromFile) && imageFromFile.contains("base64,")) {
                    imageFromFile = imageFromFile.split(",")[1];
                }
                final byte[] fileAsBytes = Base64.getDecoder().decode(imageFromFile);
                ticketTemplate = ImageIO.read(new ByteArrayInputStream(fileAsBytes));
            }

            final BufferedImage finalImage = new BufferedImage(ticketWidth, ticketHeight, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g2d = finalImage.createGraphics();

            g2d.drawImage(ticketTemplate, 0, 0, null);
            final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 34);
            g2d.setFont(font);
            g2d.setColor(new Color(0, 0, 0));

            final String firstName = guest.getFirstName();
            final String formattedFirstName = firstName.isEmpty() ? " " : firstName;


            Map<String, String> params = Map.of(
                    "qrCodeX", String.valueOf(qrCodeX),
                    "qrCodeY", String.valueOf(qrCodeY),
                    "qrCodeWidth", String.valueOf(qrCodeWidth),
                    "qrCodeHeight", String.valueOf(qrCodeHeight),
                    "nameX", String.valueOf(nameX),
                    "nameY", String.valueOf(nameY)
            );
            if (template != null && template.getParams() != null && !template.getParams().isEmpty()) {
                params = template.getParams();
            }

            g2d.drawImage(qrcode, Integer.parseInt(params.get("qrCodeX")), Integer.parseInt(params.get("qrCodeY")), Integer.parseInt(params.get("qrCodeWidth")), Integer.parseInt(params.get("qrCodeHeight")), null);
            final String name = String.format(
                    "%s %s%s %s",
                    String.valueOf(CIVILITY_MAPPING.get(String.valueOf(guest.getCivility())) == null ? "" : CIVILITY_MAPPING.get(String.valueOf(guest.getCivility()))),
                    String.valueOf(formattedFirstName.charAt(0)).toUpperCase(),
                    formattedFirstName.substring(1).toLowerCase(),

                    String.valueOf((guest.getLastName().isEmpty()) ? "" : guest.getLastName()).toUpperCase()
            );
            g2d.drawString(name.trim(), Integer.parseInt(params.get("nameX")), Integer.parseInt(params.get("nameY")));
            g2d.drawString("Ticket No", Integer.parseInt(params.get("qrCodeX")) + Integer.parseInt(params.get("qrCodeWidth")), Integer.parseInt(params.get("qrCodeY")) + 100);
            g2d.drawString(
                    guest.getPublicId(), Integer.parseInt(params.get("qrCodeX")) + Integer.parseInt(params.get("qrCodeWidth")), Integer.parseInt(params.get("qrCodeY")) + 140);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(finalImage, IMAGE_FORMAT, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void handleEvent(final Event event) {
        if (Strings.isNullOrEmpty(event.getAuthorId())) {
            return;
        }
        final List<Guest> guests = event.getGuests();
        final Invitation invitation = event.getInvitation();
        if (invitation == null || invitation.getSend() == null) {
            return;
        }
        if (invitation.isSent()) {
            return;
        }
        if (this.nbInvitations == 0) {
            this.nbInvitations = guests.size();
        }
        InvitationService.log.debug("Les invitations pour {} seront envoyées à {}", event.getName(), invitation.getSend());
        if (invitation.getSend().isBefore(Instant.now())) {
            guests.forEach(guest -> {
                this.sendGuestInvitation(event, guest);

            });
            invitation.setSent(true);
            event.setInvitation(invitation);
            this.eventsRepository.save(event);
        }
    }

    void sendGuestInvitation(final Event event, final Guest guest) {
        final UserAccount author = this.profileService.findById(event.getAuthorId());
        final Invitation invitation = event.getInvitation();

        if (Strings.isNullOrEmpty(guest.getPhoneIndex()) && Strings.isNullOrEmpty(guest.getPhone())) {
            log.info("#####################################");
            log.info("Pas d'envoi de l'invitation : {} à {} {} {} phone {}{} pour l'évènement {}", invitation.getId(), guest.getId(), guest.getFirstName(), guest.getLastName(), guest.getPhoneIndex(), guest.getPhone(), event.getName());
            log.info("#####################################");
        } else {
            log.info("Envoi de l'invitation : {} à {} {} {} phone {}{} pour l'évènement {}", invitation.getId(), guest.getId(), guest.getFirstName(), guest.getLastName(), guest.getPhoneIndex(), guest.getPhone(), event.getName());


            final String image = this.getInvitation(event, guest, invitation);
            try {
                final String templateName = invitation.getTemplate().getName();

                final byte[] decodedFile = Base64.getDecoder().decode(image);
                final File fullPathAsFile = new File("tmp.image.jpg");

                FileUtils.writeByteArrayToFile(fullPathAsFile, decodedFile);

                final String filePath = String.format("zeeven/tickets/%s/%s.jpg", event.getPublicId(), guest.getPublicId());
                if (!Strings.isNullOrEmpty(filePath) && !Strings.isNullOrEmpty(image)) {
                    this.aSynchroniousNotifications.sendFile(
                            Map.of(
                                    "file", image,
                                    "path", filePath
                            )
                    );

                    String template = event.getWhatsapp_template();

                    if (Strings.isNullOrEmpty(event.getWhatsapp_template())) {
                        template = "whatsappTemplateName";
                    }
                    final Map<String, Object> messageParameters = Map.of(
                            "eventId", event.getPublicId(),
                            "eventName", event.getName(),
                            "image", String.format("%s/%s", this.applicationFilesHost, filePath),
                            "guest", guest,
                            "author", author,
                            "application", "ZEEVEN",
                            "notificationTemplate", templateName,
                            "whatsappTemplateName", template,
                            "invitation", invitation,
                            "channels", invitation.getChannels()
                    );
                    this.aSynchroniousNotifications.sendInvitationMessage(messageParameters);

                }

            } catch (final IOException e) {
                e.printStackTrace();
            }

        }


    }

    public String getInvitation(final Event event, final Guest guest, final Invitation invitation) {
        final QRCodeEntity qrCodeEntity = QRCodeEntity
                .builder()
                .type(QRCodeType.TEXT)
                .data(Map.of("text", String.format("%s|%s|%s", event.getPublicId(), invitation.getPublicId(), guest.getPublicId())))
                .build();
        String image = null;
        try {
            final String qrcode = this.qrCodeGeneratorService.generate(qrCodeEntity, false, new HashMap<>());
            if (invitation.getTemplate() != null) {
                image = InvitationService.generateTicket(event, guest, invitation, qrcode);
            }
            if (Strings.isNullOrEmpty(image)) {
                image = qrcode;
            }

        } catch (final IOException e) {
            e.printStackTrace();
        }
        return image;
    }
}
