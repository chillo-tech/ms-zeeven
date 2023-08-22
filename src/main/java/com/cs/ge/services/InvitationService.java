package com.cs.ge.services;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Invitation;
import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.enums.QRCodeType;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.services.files.TicketFileService;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.cs.ge.services.qrcode.QRCodeGeneratorService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.cs.ge.enums.EventStatus.ACTIVE;
import static com.cs.ge.enums.EventStatus.INCOMMING;

@AllArgsConstructor
@Service
public class InvitationService {
    private final EventRepository eventsRepository;
    private final TicketFileService ticketFileService;
    private final QRCodeGeneratorService qrCodeGeneratorService;
    private final ASynchroniousNotifications aSynchroniousNotifications;

    @Scheduled(cron = "0 */10 * * * *")
    public void sendInvitations() {
        final Stream<Event> events = this.eventsRepository
                .findByStatusIn(List.of(INCOMMING, ACTIVE));
        events.parallel().forEach(this::handleEvent);
    }

    private void handleEvent(final Event event) {
        final List<Guest> guests = event.getGuests();
        final Invitation invitation = event.getInvitation();
        if (invitation.getSend().isAfter(Instant.now())) {
            guests.subList(0, 1).parallelStream().forEach(guest -> {
                final QRCodeEntity qrCodeEntity = QRCodeEntity
                        .builder()
                        .type(QRCodeType.TEXT)
                        .data(Map.of("url", String.format("%s|%s|%s", event.getPublicId(), invitation.getPublicId(), guest.getPublicId())))
                        .build();

                try {
                    final String qrcode = this.qrCodeGeneratorService.generate(qrCodeEntity);
                    final Map<String, Object> messageParameters = Map.of(
                            "eventId", event.getPublicId(),
                            "eventName", event.getName(),
                            "image", qrcode,
                            "guest", guest,
                            "author", event.getAuthor(),
                            "application", "ZEEVEN",
                            "notificationTemplate", invitation.getTemplate().getName(),
                            "whatsappTemplateName", "ze_invitation",
                            "invitation", invitation,
                            "channels", event.getInvitation().getChannels()
                    );
                    this.generateTicket(qrcode);
                    //this.aSynchroniousNotifications.sendInvitationMessage(messageParameters);
                } catch (final IOException e) {
                    e.printStackTrace();
                }

            });
        }
    }

    private void generateTicket(final String qrcodeAsTring) {
        try {
            final String path = "/Users/chillo/projets/zeeven/data/tickets/";
            final BufferedImage ticketTemplate = ImageIO.read(new File("horizontal-template.png"));
            final int ticketWidth = ticketTemplate.getWidth();
            final int ticketHeight = ticketTemplate.getHeight();

            final byte[] bytes = Base64.getDecoder().decode(qrcodeAsTring);
            final BufferedImage qrcode = ImageIO.read(new ByteArrayInputStream(bytes));

            final BufferedImage finalImage = new BufferedImage(ticketWidth, ticketHeight, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g2d = finalImage.createGraphics();

            final Font openSansFont = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("OpenSans-Light.ttf"));

            openSansFont.deriveFont(Font.PLAIN, 120);

            final Font font = new Font("Open Sans Font", Font.PLAIN, 60);
            g2d.setFont(font);
            g2d.setColor(new Color(0, 1, 57));

            g2d.drawImage(ticketTemplate, 0, 0, null);
            g2d.drawImage(qrcode, 1500, 120, 400, 400, null);

            //  g2d.drawString(text, 1800, 400);
            g2d.drawString("contestant", 100, 120);
            g2d.drawString("contestant", 1500, 80);
            g2d.drawString("contestant", 1500, 100);
            g2d.drawString("contestant", 1550, 1100);
            g2d.drawString("contestant", 1550, 1400);
            g2d.dispose();

            final File file = new File(path + "generated.png");
            ImageIO.write(finalImage, "png", file);

        } catch (final IOException | FontFormatException e) {
            e.printStackTrace();
        }


    }
}
