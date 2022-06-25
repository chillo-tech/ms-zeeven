package com.cs.ge.services;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Profile;
import com.cs.ge.entites.Schedule;
import com.cs.ge.entites.Utilisateur;
import com.cs.ge.enums.EventStatus;
import com.cs.ge.enums.Role;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.services.emails.MailsService;
import com.cs.ge.services.whatsapp.TextMessageService;
import com.cs.ge.services.whatsapp.dto.Component;
import com.cs.ge.services.whatsapp.dto.Image;
import com.cs.ge.services.whatsapp.dto.Language;
import com.cs.ge.services.whatsapp.dto.Parameter;
import com.cs.ge.services.whatsapp.dto.Template;
import com.cs.ge.services.whatsapp.dto.Text;
import com.cs.ge.services.whatsapp.dto.TextMessage;
import com.cs.ge.utilitaire.UtilitaireService;
import com.google.common.collect.Lists;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class EventService {
    private final EventRepository eventsRepository;
    private final MailsService mailsService;
    private final ImageService imageService;
    private final TextMessageService textMessageService;
    private final ValidationService validationService;
    private final ProfileService profileService;
    private final QRCodeGeneratorService qrCodeGeneratorService;
    private final String imagesHost;
    private final String accountSid;
    private final String authToken;

    public EventService(
            EventRepository eventsRepository,
            MailsService mailsService,
            ImageService imageService,
            TextMessageService textMessageService,
            ValidationService validationService,
            ProfileService profileService,
            QRCodeGeneratorService qrCodeGeneratorService,
            @Value("${resources.images.host}") String imagesHost,
            @Value("${providers.twilio.ACCOUNT_SID}") String accountSid,
            @Value("${providers.twilio.AUTH_TOKEN}") String authToken
    ) {
        this.eventsRepository = eventsRepository;
        this.mailsService = mailsService;
        this.imageService = imageService;
        this.textMessageService = textMessageService;
        this.validationService = validationService;
        this.profileService = profileService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.imagesHost = imagesHost;
        this.authToken = authToken;
        this.accountSid = accountSid;
    }

    public List<Event> search() {
        Utilisateur authenticatedUser = this.profileService.getAuthenticateUser();
        if (authenticatedUser.getRole().equals(Role.ADMIN)) {
            return this.eventsRepository.findAll();
        }

        String id = authenticatedUser.getId();
        return this.eventsRepository.findByAuthorId(id).collect(Collectors.toList());
    }

    public void add(Event event) {

        if (event.getName() == null || event.getName().trim().isEmpty()) {
            throw new ApplicationException("Champs obligatoire");
        }

        Utilisateur authenticatedUser = this.profileService.getAuthenticateUser();
        event.setAuthor(authenticatedUser);

        final EventStatus status = eventStatus(event.getDates());
        event.setStatus(status);
        String publicId = RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT);
        event.setPublicId(publicId);
        String slug = UtilitaireService.makeSlug(event.getName());
        event.setSlug(format("%s-%s", slug, publicId));

        event = this.eventsRepository.save(event);

        this.mailsService.newEvent(event);
    }

    private static EventStatus eventStatus(Set<Instant> dates) {
        List<Instant> datesAsList = new ArrayList<>(dates);
        List<Instant> sorted = datesAsList.stream()
                .distinct() // If you want only unique elements in the end List
                .sorted()
                .collect(Collectors.toList());
        final Date date = new Date();
        EventStatus status = EventStatus.DISABLED;
        Instant now = Instant.now();
        if (sorted.get(0).isBefore(now)) {
            throw new ApplicationException("La date de votre évènement est invalide");
        }

        if (sorted.get(0).equals(now)) {
            status = EventStatus.ACTIVE;
        }

        if (sorted.get(sorted.size() - 1).isAfter(now)) {
            status = EventStatus.INCOMMING;
        }

        return status;
    }

    public void delete(final String id) {
        this.eventsRepository.deleteById(id);
    }

    public void update(final String id, final Event event) {
        final Optional<Event> current = this.eventsRepository.findById(id);
        if (current.isPresent()) {
            final Event foundEvents = current.get();
            foundEvents.setId(id);
            foundEvents.setName(event.getName());
            foundEvents.setStatus(event.getStatus());
            this.eventsRepository.save(foundEvents);
        }
    }

    public Event read(final String id) {
        return this.eventsRepository.findByPublicId(id).orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Aucune enttité ne correspond au critères fournis")
        );
    }

    public byte[] getGuestEventTicket(String guestId, String eventId) {
        Event event = this.read(eventId);
        Optional<Guest> optionalGuest = event.getGuests().stream().filter(g -> g.getProfile().getPublicId().equals(guestId)).findFirst();
        Guest guest = optionalGuest.orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Aucune enttité ne correspond au critères fournis"));

        return Base64.getDecoder().decode(guest.getTicket());
    }

    public void addGuest(final String eventId, final Guest guest) {
        final var event = this.read(eventId);
        ValidationService.checkEmail(guest.getProfile().getEmail());
        ValidationService.checkPhone(guest.getProfile().getPhone());
        Profile guestProfile = guest.getProfile();
        String guestId = UUID.randomUUID().toString();
        guestProfile.setId(guestId);

        String publicId = RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT);
        guestProfile.setPublicId(publicId);
        String slug = UtilitaireService.makeSlug(format("%s %s", guestProfile.getFirstName(), guestProfile.getLastName()));
        guestProfile.setSlug(format("%s-%s", slug, publicId));
        String guestQRCODE = this.qrCodeGeneratorService.guestQRCODE(event.getPublicId(), guestProfile.getPublicId());
        guest.setTicket(guestQRCODE);
        guest.setProfile(guestProfile);
        List<Guest> guests = event.getGuests();
        if (guests == null) {
            guests = new ArrayList<>();
        }
        guests.add(guest);
        event.setGuests(guests);
        this.eventsRepository.save(event);
        this.imageService.saveTicketImages(event, guest);
        if (guest.isSendInvitation()) {
            this.sendInvitation(event, guest);
        }

    }

    private void sendInvitation(Event event, Guest guest) {
        Profile guestProfile = guest.getProfile();
        if (StringUtils.isNotBlank(guestProfile.getEmail()) && StringUtils.isNotEmpty(guestProfile.getEmail())) {
            this.mailsService.newGuest(guestProfile, event, guest.getTicket());
        }

        if (StringUtils.isNotBlank(guestProfile.getPhone()) && StringUtils.isNotEmpty(guestProfile.getPhone())) {
            this.sendWhatAppMessage(event, guest);
            this.sendTwilioMessage(event, guest);

        }
    }

    private void sendTwilioMessage(Event event, Guest guest) {
        String azureImage = "https://zeevenimages.blob.core.windows.net/images/eo9arfovirt6dxzrythf.jpeg?sp=r&st=2022-06-26T08:43:58Z&se=2022-06-26T16:43:58Z&spr=https&sv=2021-06-08&sr=b&sig=8Sr%2BcyxBFrucDoCl5l3uEC01WAtFvHz3Htvqimtqc6E%3D";
        String linkWithExtension = String.format("%s/events/%s/tickets/%s.jpg", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId());
        Twilio.init(this.accountSid, this.authToken);
        Message message = null;
        try {
            message = Message.creator(
                            new com.twilio.type.PhoneNumber("whatsapp:+33761705745"),
                            new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
                            List.of(new URI(azureImage))).setBody("fpjzfpojzjjgpojrzfpojzpfzjpo")
                    .create();
            System.out.println(message.getSid());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendWhatAppMessage(Event event, Guest guest) {
        String azureImage = "https://zeevenimages.blob.core.windows.net/images/eo9arfovirt6dxzrythf.jpeg?sp=r&st=2022-06-26T08:43:58Z&se=2022-06-26T16:43:58Z&spr=https&sv=2021-06-08&sr=b&sig=8Sr%2BcyxBFrucDoCl5l3uEC01WAtFvHz3Htvqimtqc6E%3D";

        Profile guestProfile = guest.getProfile();
        Text text = new Text();
        text.setBody("Votre invitation");
        text.setPreview_url(false);

        Template template = new Template();
        template.setName("user_invitation");
        template.setLanguage(new Language("fr"));

        Image image = new Image();
        String link = String.format("%s/ticket?event=%s&guest=%s", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId());
        log.info("Link information " + link);
        String encodedUrl = Base64.getUrlEncoder().encodeToString(link.getBytes());
        log.info("Encoded link " + encodedUrl);
        image.setLink(encodedUrl);

        String linkWithExtension = String.format("%s/events/%s/tickets/%s.jpg", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId());
        log.info("linkWithExtension " + linkWithExtension);
        String linkWithExtensionEncoded = Base64.getUrlEncoder().encodeToString(linkWithExtension.getBytes());
        log.info("Encoded linkWithExtension " + linkWithExtension);
        image.setLink(azureImage);

        //image.setLink("https://media.istockphoto.com/photos/taj-mahal-mausoleum-in-agra-picture-id1146517111?k=20&m=1146517111&s=612x612&w=0&h=vHWfu6TE0R5rG6DJkV42Jxr49aEsLN0ML-ihvtim8kk=");
        String uri = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(guest.getTicket().getBytes(StandardCharsets.UTF_8));
        //image.setLink("data:image/png;base64,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gL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgMDlERAgsHwELtlzh+VTWCUl0LHAin2PXq2lcy5Zrfw+vAOB1f0d7oBQEQgQGFPACP+YUDYjQIAAAQIECBAgQIAAAQKTJCDgn6TaklcCBAgQIECAAAECBAgQIDCmgIB/TCibESBAgAABAgQIECBAgACBSRIQ8E9SbckrAQIECBAgQIAAAQIECBAYU0DAPyaUzQgQIECAAAECBAgQIECAwCQJCPgnqbbklQABAgQIECBAgAABAgQIjCkg4B8TymYECBAgQIAAAQIECBAgQGCSBAT8k1Rb8kqAAAECBAgQIECAAAECBMYUEPCPCWUzAgQIECBAgAABAgQIECAwSQIC/kmqLXklQIAAAQIECBAgQIAAAQJjCgj4x4SyGQECBAgQIECAAAECBAgQmCQBAf8k1Za8EiBAgAABAgQIECBAgACBMQUE/GNC2YwAAQIECBAgQIAAAQIECEySgIB/kmpLXgkQIECAAAECBAgQIECAwJgCAv4xoWxGgAABAgQIECBAgAABAgQmSUDAP0m1Ja8ECBAgQIAAAQIECBAgQGBMAQH/mFA2I0CAAAECBAgQIECAAAECkyQg4J+k2pJXAgQIECBAgAABAgQIECAwpoCAf0womxEgQIAAAQIECBAgQIAAgUkSEPBPUm3JKwECBAgQIECAAAECBAgQGFNAwD8mlM0IECBAgAABAgQIECBAgMAkCQj4J6m25JXAEhT461//Wk455ZTyhz/8YcG5e+pTn1q22GKLcv755y/4GL3vePjhh5cjjzyy92Iq3zIX+MlPflJ+9KMfdaHgO9tFNSoEAQIEJl5AwD/xVagABFaPwLHHHlvuda97lSte8Yo1WF977bXL5ptvXr75zW/OK0MnnnhiOeSQQ8rDHvawcr3rXW9e+y6Xjf/5z3+WJz3pSWXXXXddLkVWzmUkcNFFF5WnPe1pZaONNio3vOENyyabbFKufvWrl+c85znl73//+0RK+M5OZLXJNAECBLoUuHyXpVIoAgQuVYEPf/jD5ZGPfGS59rWvXZ773OfWwP+MM84o++yzT7njHe9YPve5z5V73/veY+XhjW98Y9lqq63KS1/60rG2X44bXe5ylytf+cpXSh4lAj0J/Otf/ypbb711+da3vlXPGS9+8YtLOg/f+973loMOOqicfvrp5TOf+czEFdl3duKqTIYJECDQrcCKss9Rl3RbOgUjQGCawCV77jDt+UKeXHLJJeXGN75xnXb7P//zP+We97zn1GG+/vWvl7vc5S7lQQ96UPn4xz8+9Xr7Ixf3a6wxfWJRjrdixYq2ycjHcbbJjuNuN/JDBl5c6HHm2m+u91sWxt1uPtuPsm/7e5w8gRX7Hr1aM70qziUpwPve977yhCc8oXb6feELX5gqU0bIH/rQh5ZPfOIT5Tvf+U65zW1uM/Ve/hj3OzLOdnNtM9f70zI25pNxjjnONmN+3LTN5jruXO/nYM4n00gX9GR1f4cXlGk7ESAwkQLTr7wnsggyTYDAZSlw7rnn1mD/yU9+8rRgP3m4853vXB772MfWC/Tc03/xxReXm970puVNb3pT2X777esI9Ute8pKa3e9973vl0Y9+dLnGNa5Rp/I+61nPKr/73e+mFSUzCbbdZtvaSXCHO9yh7L///tPeT8fCTjvtVPbaa69ynetcp9z97nef9n6eJL/JQ25BGEx77LFH7ZzIay2fb33rW8tTnvKUsv7669cpxclf3psptc8/7LDDahnSmZHXvvSlL03bJaPzW265ZS1H8vmQhzyk/OxnP5vaph3n9a9/fZ3OnOPc7W53q6ObbaP4JTAaTPGIS7bPKGm8Wjr++ONrud///vfXcmbEcRJHSlt5PPYp0NrkoYceOq2Aaa97r5wxlFlECfpbynoh+V7mO5rv0uMe97hp35NXvepV9daitP/23cj3f/C7Mc42+bzZzlGZkZTjfvWrX21ZK5/61Kfqa60sw9/Z3LqQ81xuWch57wEPeMC0si30PJRzxYte9KKpfOSPD33oQzUvbT2EP/3pT/W2iete97r1fNFmYg3uNNv5tuVt1Ll88Bj+JkCAAIGlJyDgX3p1IkcElrTAd7/73Zq/+9///iPzecQRR5QLLrigXPWqV63335599tnlBS94QTn55JNLFue7053uVIPd7P/5z3++XrBnVkCC7Uc96lF15CgHznsPf/jDy3nnn1de+MIX1uMlSN97772nPvfHP/5xSbCdfbfddtvaqTD15r//+Mtf/lKSh9/+9rfT3vrpT386tThY7hPONuk4SD4TWF//+tcvRx11VHnXu941bb/BJ/n8D37wg7V897jHPWr5Etw/+MEPnjp2LrgT7J933v+VI+sefOxjH6sBejtWO04uptMZcN/73rdkbYPdd9+9bVKymFm2aylBSzzWWWed6hPzeH3xi1+sm/z+97+vZUoHTFICDWskVAr/WUICCapvdrOblY033vg/crX5LW9Zfv7zn5eXvexl9b18B7bZZpty3HHH1dH/dJSlMyDnktaB9otf/KKceuqp9Tt861vfunZA5rudDsq2HsA42+R4s52jsr5AguCnP/3p9bg5zzz72c8ul7/85es5LRke/M7ms7NOSc5Vma2QjsUzzzyzbLfddlPf2YWeh374wx+WCy+8cJpfOhdS7iyqmpRz2jvf+c6yww47lNe+9rV1kdWcZ84666z6/lzn25a34XN53dl/CBAgQGBJC7iHf0lXj8wRWHoCGWFLuuXKi/FxUy7ov/GNb9SgPftkga5coKbzIKvzJ93+9revgesJJ5xQ7ne/+9UL6Zvc5CYltwkkqM000wTSCcqf97znlStf+cp1v/wn22y2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MwgI+GeA8TIBAgQIECBAgAABAgQIEJhkAQH/JNeevBMgQIAAAQIECBAgQIAAgRkEBPwzwHiZAAECBAgQIECAAAECBAhMsoCAf5JrT94JECBAgAABAgQIECBAgMAMAgL+GWC8TIAAAQIECBAgQIAAAQIEJllAwD/JtSfvBAgQIECAAAECBAgQIEBgBgEB/wwwXiZAgAABAgQIECBAgAABApMsIOCf5NqTdwIECBAgQIAAAQIECBAgMIOAgH8GGC8TIECAAAECBAgQIECAAIFJFhDwT3LtyTsBAgQIECBAgAABAgQIEJhBQMA/A4yXCRAgQIAAAQIECBAgQIDAJAsI+Ce59uSdAAECBAgQIECAAAECBAjMICDgnwHGywQIECBAgAABAgQIECBAYJIFBPyTXHvyToAAAQIECBAgQIAAAQIEZhAQ8M8A42UCBAgQIECAAAECBAgQIDDJAgL+Sa49eSdAgAABAgQIECBAgAABAjMICPhngPEyAQIECBAgQIAAAQIECBCYZAEB/yTXnrwTWAICf/3rX8spp5xS/vCHP6zS3PziF78oBx98cDnnnHNW6XFzsG9961v12H//+99X+bFX1wF/8pOflP/6r/8q559//mrJwne/+91qmvbQa0rZ9t577/L1r3+91yKu9nKlHf/oRz9a5fn4xCc+UY4++uhVftyf/vSntd0n39L/Cazuc5F6IECAAIHpAgL+6R6eESAwpsCxxx5b7nWve5UrXvGKZYsttihrr7122Xzzzcs3v/nNMY8w+2a56N91113Lt7/97dk3XMC7H//4x+uxl2pw+vnPf77k33zSEUccUV75yleW//7v/57Pbqts2+OPP76a/v73v6/HfMtb3lJWrFhRvn/qqavsM1b3gb7zne+Uvfbaq7z2ta9d3Vnp6vMvuuii8rSnPa1stNFG5YY3vGHZZJNNytWvfvXynOc8p6yqTrmDDjyovOY1r1nlbmeddVZt99/73vdW+bEn9YCr+1w0qW7yTYAAgUtL4PKX1oEdlwCBfgU+/OEPl0c+8pHl2te+dnnuc59bA/8zzjij7LPPPuWOd7xj+dznPlfufe979wtwKZdsjz32qJ8wn86TZz3rWTVQeuADH3gp5268w6+55pp1w7X+/TjeXkt7qzvf+c7lgx/8YLn97W+/tDM6Qbn717/+Vbbeeus66ybnjBe/+MW1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q0WmCBAgQIAAAQIECBAgQIDA4gQE/IvzszcBAgQIECBAgAABAgQIEFiSAgL+JVktMkWAAAECBAgQIECAAAECBBYnIOBfnJ+9CRAgQIAAAQIECBAgQIDAkhQQ8C/JapEpAgQIECBAgAABAgQIECCwOAEB/+L87E2AAAECBAgQIECAAAECBJakgIB/SVaLTBEgQIAAAQIECBAgQIAAgcUJCPgX52dvAgQIECBAgAABAgQIECCwJAUE/EuyWmSKAAECBAgQIECAAAECBAgsTkDAvzg/exMgQIAAAQIECBAgQIAAgSUpIOBfktUiUwQIECBAgAABAgQIECBAYHECAv7F+dmbAAECBAgQIECAAAECBAgsSQEB/5KsFpkiQIAAAQIECBAgQIAAAQKLExDwL87P3gQIECBAgAABAgQIECBAYEkKCPiXZLXIFAECBAgQIECAAAECBAgQWJyAgH9xfvYmQIAAAQIECBAgQIAAAQJLUkDAvySrRaYITIbAxRdfXE455ZTy17/+dZVl+MILLyynn356+de//rXKjulABAgsbYFLLrmknHnmmeW8886bNaN/+tOf6nazbjTPN3OuOe2008rf//73ee5pcwIECBAgsPQFBPxLv47kkMCSE/jJT35S7nCHO5SrX/3qZYsttihXvOIVy1Oe8pT/uGD+/e9/Xx71qEeVFStWlJe97GWzluOzn/1sue51r1v/3fzmNy/XvOY1y8EHHzzrPt4kQGDyBfbbb79yjWtco2y22WblBje4QT0HnHjiidMKlo7AhzzkIeUqV7lK3W6TTTYp+++//7RtRj1JR8Lee+9dz0H3vOc9p22SAP/Zz3522WCDDcotbnGLst5665Wddtqp/OUvf5m2nScECBAgQGCSBQT8k1x78k5gNQhkNOwe97hHOfvss8uhhx5avvjFL5Ydd9yxvPvd7y4vfelLp3L0ve99r9zudrcrxxxzTH1tthH7jK5tvfXW5WpXu1r5+Mc/Xo477riy8cYbl1133bV8+tOfnjqmPwgQ6EvgkEMOKXvuuWe5173uVT73uc+VI444ovzxj38sd7vb3cpFF100VdhHPvKR5WMf+1jZfffd6zll/fXXL3vssUc5+uijp7YZ/uPXv/51eeADH1j22muv+tY//vGPaZu84hWvKG95y1vKlltuWT7ykY+UBz/4weWwww4rz33uc6dt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QIAAAQIECBAgQIAAgWUvIOBf9k0AAAECBAgQIECAAAECBAj0KCDg77FWlYkAAQIECBAgQIAAAQIElr2AgH/ZNwEABAgQIECAAAECBAgQINCjgIC/x1pVJgIECBAgQIAAAQIECBBY9gIC/mXfBAAQIECAAAECBAgQIECAQI8CAv4ea1WZCBAgQIAAAQIECBAgQGDZCwj4l30TAECAAAECBAgQIECAAAECPQoI+HusVWUiQIAAAQIECBAgQIAAgWUvIOBf9k0AAAECBAgQIECAAAECBAj0KCDg77FWlYkAAQIECBAgQIAAAQIElr3AirLPUZcsewUABAgQIECAAAECBAgQIECgMwEj/J1VqOIQIECAAAECBAgQIECAAIEICPi1AwIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgICAXxsgQIAAAQIECBAgQIAAAQIdCgj4O6xURSJAgAABAgQIECBAgAABAgJ+bYAAAQIECBAgQIAAAQIECHQoIODvsFIViQABAgQIECBAgAABAgQICPi1AQIECBAgQIAAAQIECBAg0KGAgL/DSlUkAgQIECBAgAABAgQIECAg4NcGCBAgQIAAAQIECBAgQIBAhwIC/g4rVZEIECBAgAABAgQIECBAgMD/A0jRJGpFU+PNAAAAAElFTkSuQmCC");
        //image.setLink(uri);
        Parameter parameter = new Parameter();
        parameter.setType("image");
        parameter.setImage(image);

        Component header = new Component();
        header.setType("header");
        header.setParameters(List.of(parameter));

        Component body = new Component();
        body.setType("body");
        body.setParameters(List.of(
                new Parameter("text", String.format("%s %s", guestProfile.getFirstName(), guestProfile.getLastName().toUpperCase()), null),
                new Parameter("text", event.getName(), null)
        ));

        template.setComponents(List.of(header, body));
        TextMessage textMessage = new TextMessage();
        textMessage.setTemplate(template);
        textMessage.setMessaging_product("whatsapp");
        textMessage.setRecipient_type("individual");
        textMessage.setType("template");
        textMessage.setTo(String.format("%s%s", guestProfile.getPhoneIndex(), guestProfile.getPhone()));
        this.textMessageService.message(textMessage);
    }

    public void deleteGuest(final String eventId, final String guestId) {
        final var event = this.read(eventId);
        List<Guest> guests = event.getGuests();
        guests = guests.stream().filter(currentGuest -> !currentGuest.getProfile().getPublicId().equals(guestId)).collect(Collectors.toList());
        event.setGuests(guests);
        this.eventsRepository.save(event);
    }

    public List<Guest> guests(final String id) {
        final Event event = this.read(id);
        return event.getGuests();
    }

    public void addSchedule(String eventId, Schedule schedule) {
        final var event = this.read(eventId);
        String guestId = UUID.randomUUID().toString();
        schedule.setId(guestId);

        String publicId = RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT);
        schedule.setPublicId(publicId);
        String slug = UtilitaireService.makeSlug(schedule.getTitle());
        schedule.setSlug(format("%s-%s", slug, publicId));

        List<Schedule> schedules = event.getSchedules();
        if (schedules == null) {
            schedules = new ArrayList<>();
        }
        schedules.add(schedule);
        event.setSchedules(schedules);
        this.eventsRepository.save(event);
    }

    public void deleteSchedule(String eventId, String scheduleId) {
        final var event = this.read(eventId);
        List<Schedule> schedules = event.getSchedules();
        schedules = schedules.stream().filter(currentSchedule -> !currentSchedule.getPublicId().equals(scheduleId)).collect(Collectors.toList());
        event.setSchedules(schedules);
        this.eventsRepository.save(event);
    }

    public List<Schedule> schedules(String id) {
        final Event event = this.read(id);
        return event.getSchedules();
    }

    @Async
    public void sendInvitations(String id, Set<String> guestIds) {
        Event event = this.read(id);
        List<String> guestIdsAsList = Lists.newArrayList(guestIds);
        event.getGuests()
                .stream()
                .filter(guest -> guestIdsAsList.contains(guest.getProfile().getPublicId()))
                .forEach(profile -> this.sendInvitation(event, profile));

    }
}
