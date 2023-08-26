package com.cs.ge.services;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Invitation;
import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.entites.Template;
import com.cs.ge.enums.QRCodeType;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.cs.ge.services.qrcode.QRCodeGeneratorService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

    public InvitationService(
            final EventRepository eventsRepository,
            final QRCodeGeneratorService qrCodeGeneratorService,
            final ASynchroniousNotifications aSynchroniousNotifications,
            @Value("${app.images-host}") final String applicationFilesHost
    ) {
        this.eventsRepository = eventsRepository;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.aSynchroniousNotifications = aSynchroniousNotifications;
        this.applicationFilesHost = applicationFilesHost;
    }

    @Scheduled(cron = "@hourly")
    public void sendInvitations() {
        final Stream<Event> events = this.eventsRepository
                .findByStatusIn(List.of(INCOMMING, ACTIVE));
        events.parallel()
                .filter(event -> event.getParams().isHasInvitation())
                .forEach(this::handleEvent);
    }

    private void handleEvent(final Event event) {
        final List<Guest> guests = event.getGuests();
        final Invitation invitation = event.getInvitation();
        if (invitation.getSend().isBefore(Instant.now())) {
            guests.parallelStream().forEach(guest -> {
                final QRCodeEntity qrCodeEntity = QRCodeEntity
                        .builder()
                        .type(QRCodeType.TEXT)
                        .data(Map.of("url", String.format("%s|%s|%s", event.getPublicId(), invitation.getPublicId(), guest.getPublicId())))
                        .build();

                try {
                    final String qrcode = this.qrCodeGeneratorService.generate(qrCodeEntity);
                    String image = this.generateTicket(event, guest, invitation, qrcode);
                    if (Strings.isNullOrEmpty(image)) {
                        image = qrcode;
                    }

                    final String filePath = String.format("zeeven/tickets/%s/%s.jpg", event.getPublicId(), guest.getPublicId());
                    if (!Strings.isNullOrEmpty(filePath) && !Strings.isNullOrEmpty(image)) {
                        this.aSynchroniousNotifications.senFile(
                                Map.of(
                                        "file", image,
                                        "path", filePath
                                )
                        );
                        final Map<String, Object> messageParameters = Map.of(
                                "eventId", event.getPublicId(),
                                "eventName", event.getName(),
                                "image", String.format("%s/%s", this.applicationFilesHost, filePath),
                                "guest", guest,
                                "author", event.getAuthor(),
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

    private String generateTicket(final Event event, final Guest guest, final Invitation invitation, final String qrcodeAsString) {
        try {

            final Template template = invitation.getTemplate();

            final String path = "/Users/chillo/projets/zeeven/data/tickets/";
            final BufferedImage ticketTemplate = ImageIO.read(new File("horizontal-template.png"));
            final int ticketWidth = ticketTemplate.getWidth();
            final int ticketHeight = ticketTemplate.getHeight();

            final byte[] bytes = Base64.getDecoder().decode(qrcodeAsString);
            final BufferedImage qrcode = ImageIO.read(new ByteArrayInputStream(bytes));

            final BufferedImage finalImage = new BufferedImage(ticketWidth, ticketHeight, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g2d = finalImage.createGraphics();

            final Font openSansFont = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("OpenSans-Light.ttf"));

            openSansFont.deriveFont(Font.PLAIN, 32);

            Font font = new Font("Open Sans Font", Font.BOLD, 80);
            g2d.setFont(font);

            g2d.drawImage(ticketTemplate, 0, 0, null);

            final Rectangle left = new Rectangle(28, 22, 1350, 600);

            g2d.setColor(new Color(2, 3, 59));
            this.centerString(g2d, left, event.getName(), font, 70, 250);
            font = new Font("Open Sans Font", Font.PLAIN, 32);
            g2d.setFont(font);


            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                    .withZone(ZoneId.systemDefault());
            this.centerString(g2d, left, template.getAddress(), font, 70, 310);
            final String mappedSchedules = invitation.getTemplate().getSchedules().stream()
                    .map(schedule -> formatter.format(schedule.getDate())).collect(Collectors.joining(" | "));
            this.centerString(g2d, left, mappedSchedules, font, 70, 350);

            font = new Font("Open Sans Font", Font.PLAIN, 20);
            g2d.setFont(font);
            this.centerString(g2d, left, invitation.getTemplate().getText(), font, 70, 420);
            font = new Font("Open Sans Font", Font.PLAIN, 32);
            g2d.setFont(font);

            final Rectangle right = new Rectangle(1430, 22, 540, 600);
            g2d.setColor(new Color(2, 3, 59));

            this.centerString(
                    g2d,
                    right,
                    String.format(
                            "%s %s%s %s",
                            CIVILITY_MAPPING.get(String.valueOf(guest.getCivility())),
                            String.valueOf(guest.getFirstName().charAt(0)).toUpperCase(),
                            guest.getFirstName().substring(1).toLowerCase(),
                            guest.getLastName().toUpperCase()
                    ),
                    font,
                    0,
                    80
            );
            g2d.drawImage(qrcode, 1500, 120, 400, 400, null);
            this.centerString(
                    g2d,
                    right,
                    "Ticket No",
                    font,
                    0,
                    510
            );

            font = new Font("Open Sans Font", Font.BOLD, 50);
            g2d.setFont(font);

            this.centerString(
                    g2d,
                    right,
                    guest.getPublicId(),
                    font,
                    0,
                    565
            );
            g2d.dispose();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(finalImage, IMAGE_FORMAT, baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (final IOException | FontFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void centerString(
            final Graphics g,
            final Rectangle r,
            final String s,
            final Font font,
            final int xPosition,
            final int yPosition
    ) {
        final FontRenderContext frc =
                new FontRenderContext(null, true, true);

        final Rectangle2D r2D = font.getStringBounds(s, frc);
        final int rWidth = (int) Math.round(r2D.getWidth());
        final int rHeight = (int) Math.round(r2D.getHeight());
        final int rX;
        final int a;
        if (xPosition != 0) {
            a = xPosition;
        } else {
            rX = (int) Math.round(r2D.getX());
            a = (r.width / 2) - (rWidth / 2) - rX;
        }
        final int rY;
        final int b;
        if (yPosition != 0) {
            b = yPosition;
        } else {
            rY = (int) Math.round(r2D.getY());
            b = (r.height / 2) - (rHeight / 2) - rY;
        }

        g.setFont(font);
        g.drawString(s, r.x + a, r.y + b);
    }
}
