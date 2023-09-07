package com.cs.ge.services;

import com.cs.ge.dto.ProfileDTO;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Invitation;
import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.entites.Template;
import com.cs.ge.enums.QRCodeType;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.services.graphics.GraphicsService;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.cs.ge.services.qrcode.QRCodeGeneratorService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cs.ge.enums.EventStatus.ACTIVE;
import static com.cs.ge.enums.EventStatus.INCOMMING;
import static com.cs.ge.utils.Data.CIVILITY_MAPPING;
import static com.cs.ge.utils.Data.IMAGE_FORMAT;
import static com.cs.ge.utils.Data.PATTERN_FORMAT;

@Slf4j
@Service
public class InvitationService {
    private final EventRepository eventsRepository;
    private final QRCodeGeneratorService qrCodeGeneratorService;
    private final ASynchroniousNotifications aSynchroniousNotifications;
    private final String applicationFilesHost;
    private final GraphicsService graphicsService;
    private int nbInvitations;


    public InvitationService(
            final EventRepository eventsRepository,
            final QRCodeGeneratorService qrCodeGeneratorService,
            final ASynchroniousNotifications aSynchroniousNotifications,
            @Value("${app.images-host}") final String applicationFilesHost,
            @Value("${app.invitations.limit:0}") final int nbInvitations,
            final GraphicsService graphicsService) {
        this.eventsRepository = eventsRepository;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.aSynchroniousNotifications = aSynchroniousNotifications;
        this.applicationFilesHost = applicationFilesHost;
        this.graphicsService = graphicsService;
        this.nbInvitations = nbInvitations;
    }

    @Scheduled(cron = "*/10 * * * * *")
    //@Scheduled(cron = "@hourly")
    public void sendInvitations() {
        final Stream<Event> events = this.eventsRepository
                .findByStatusIn(List.of(INCOMMING, ACTIVE));
        events.parallel()
                .filter(event -> event.getParams().isInvitation())
                .forEach(this::handleEvent);
    }

    private void handleEvent(final Event event) {
        final List<Guest> guests = event.getGuests();
        final Invitation invitation = event.getInvitation();
        if (invitation == null || invitation.getSend() == null) {
            return;
        }
        if (this.nbInvitations == 0) {
            this.nbInvitations = guests.size();
        }
        if (invitation.getSend().isBefore(Instant.now())) {
            guests.subList(0, 1).parallelStream().forEach(guest -> {
                final QRCodeEntity qrCodeEntity = QRCodeEntity
                        .builder()
                        .type(QRCodeType.TEXT)
                        .data(Map.of("text", String.format("%s|%s|%s", event.getPublicId(), invitation.getPublicId(), guest.getPublicId())))
                        .build();

                try {
                    final String qrcode = this.qrCodeGeneratorService.generate(qrCodeEntity, false);
                    String image = this.generateTicket(event, guest, invitation, qrcode);
                    if (Strings.isNullOrEmpty(image)) {
                        image = qrcode;
                    }

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
                        guest.setPhoneIndex("33");
                        guest.setPhone("761705745");
                        final Map<String, Object> messageParameters = Map.of(
                                "eventId", event.getPublicId(),
                                "eventName", event.getName(),
                                "image", String.format("%s/%s", this.applicationFilesHost, filePath),
                                "guest", new ProfileDTO(
                                        guest.getPublicId(),
                                        guest.getCivility(),
                                        guest.getFirstName(),
                                        guest.getLastName(),
                                        guest.getEmail(),
                                        guest.isTrial(),
                                        guest.getPhoneIndex(),
                                        guest.getPhone()
                                ),
                                "author", new ProfileDTO(
                                        event.getAuthor().getPublicId(),
                                        event.getAuthor().getCivility(),
                                        event.getAuthor().getFirstName(),
                                        event.getAuthor().getLastName(),
                                        event.getAuthor().getEmail(),
                                        event.getAuthor().isTrial(),
                                        event.getAuthor().getPhoneIndex(),
                                        event.getAuthor().getPhone()
                                ),
                                "application", "ZEEVEN",
                                "notificationTemplate", invitation.getTemplate().getName(),
                                "whatsappTemplateName", "ze_invitation",
                                "invitation", invitation,
                                "channels", event.getInvitation().getChannels()
                        );
                        this.aSynchroniousNotifications.sendInvitationMessage(messageParameters);

                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }

            });
        }
    }

    private String generateTicket(final Event event, final Guest guest, final Invitation invitation,
                                  final String qrcodeAsString) {
        try {

            final Template template = invitation.getTemplate();
            BufferedImage ticketTemplate = null; //ImageIO.read(new File("horizontal-template.png"));
            final int ticketWidth = 2000; //ticketTemplate.getWidth();
            final int ticketHeight = 647; //ticketTemplate.getHeight();

            final byte[] bytes = Base64.getDecoder().decode(qrcodeAsString);
            final BufferedImage qrcode = ImageIO.read(new ByteArrayInputStream(bytes));

            if (!Strings.isNullOrEmpty(event.getInvitation().getTemplate().getFile())) {
                String imageFromFile = event.getInvitation().getTemplate().getFile();
                if (!Strings.isNullOrEmpty(imageFromFile) && imageFromFile.contains("base64,")) {
                    imageFromFile = imageFromFile.split(",")[1];
                }
                final byte[] fileAsBytes = Base64.getDecoder().decode(imageFromFile);
                ticketTemplate = ImageIO.read(new ByteArrayInputStream(fileAsBytes));
            }

            final BufferedImage finalImage = new BufferedImage(ticketWidth, ticketHeight, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g2d = finalImage.createGraphics();

            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 90);
            g2d.setFont(font);

            g2d.drawImage(ticketTemplate, 0, 0, null);

            final Rectangle left = new Rectangle(28, 22, 1350, 600);

            g2d.setColor(new Color(210, 168, 40));
            this.graphicsService.centerString(g2d, left, event.getName().toUpperCase(), font, 15, 250);
            font = new Font(Font.SANS_SERIF, Font.BOLD, 34);
            g2d.setFont(font);


            g2d.setColor(new Color(255, 255, 255));
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                    .withZone(ZoneId.systemDefault());
            this.graphicsService.centerString(g2d, left, template.getAddress().toUpperCase(), font, 15, 350);
            final String mappedSchedules = invitation.getTemplate().getSchedules().stream()
                    .map(schedule -> formatter.format(schedule.getDate())).collect(Collectors.joining(" | "));
            this.graphicsService.centerString(g2d, left, mappedSchedules.toUpperCase(), font, 15, 400);

            this.graphicsService.centerString(g2d, left, invitation.getTemplate().getText().toUpperCase(), font, 15, 600);

            font = new Font(Font.SANS_SERIF, Font.BOLD, 32);
            g2d.setFont(font);

            final Rectangle right = new Rectangle(1425, 22, 540, 600);
            g2d.setColor(new Color(0, 0, 0));

            final String firstName = guest.getFirstName();
            final String formattedFirstName = firstName.isEmpty() ? " " : firstName;
            this.graphicsService.centerString(
                    g2d,
                    right,
                    String.format(
                            "%s %s%s %s",
                            CIVILITY_MAPPING.get(String.valueOf(guest.getCivility())),
                            String.valueOf(formattedFirstName.charAt(0)).toUpperCase(),
                            formattedFirstName.substring(1).toLowerCase(),
                            String.valueOf(guest.getLastName().isEmpty() ? "" : guest.getLastName()).toUpperCase()
                    ),
                    font,
                    0,
                    100
            );
            g2d.drawImage(qrcode, 1530, 130, 350, 350, null);
            this.graphicsService.centerString(
                    g2d,
                    right,
                    "Ticket No",
                    font,
                    0,
                    470
            );

            font = new Font(Font.SANS_SERIF, Font.BOLD, 50);
            g2d.setFont(font);

            this.graphicsService.centerString(
                    g2d,
                    right,
                    guest.getPublicId(),
                    font,
                    0,
                    520
            );
            g2d.dispose();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(finalImage, IMAGE_FORMAT, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
