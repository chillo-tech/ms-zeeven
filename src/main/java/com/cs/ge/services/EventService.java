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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
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
            throw new ApplicationException("La date de votre ??v??nement est invalide");
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
                () -> new ResponseStatusException(NOT_FOUND, "Aucune enttit?? ne correspond au crit??res fournis")
        );
    }

    public byte[] getGuestEventTicket(String guestId, String eventId) {
        Event event = this.read(eventId);
        Optional<Guest> optionalGuest = event.getGuests().stream().filter(g -> g.getProfile().getPublicId().equals(guestId)).findFirst();
        Guest guest = optionalGuest.orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Aucune enttit?? ne correspond au crit??res fournis"));

        return Base64.getDecoder().decode(guest.getTicket());
    }

    public void addGuest(final String eventId, final Guest guest) {
        final var event = this.read(eventId);
        ValidationService.checkEmail(guest.getProfile().getEmail());
        ValidationService.checkPhone(guest.getProfile().getPhone());
        Profile guestProfile = guest.getProfile();
        String guestId = UUID.randomUUID().toString();
        guestProfile.setId(guestId);

        String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        guestProfile.setPublicId(publicId);
        String slug = UtilitaireService.makeSlug(format("%s %s", guestProfile.getFirstName(), guestProfile.getLastName()));
        guestProfile.setSlug(format("%s-%s", slug, publicId));
        this.qrCodeGeneratorService.guestQRCODE(event, guest);
        //guest.setTicket(guestQRCODE);
        guest.setProfile(guestProfile);
        List<Guest> guests = event.getGuests();
        if (guests == null) {
            guests = new ArrayList<>();
        }
        guests.add(guest);
        event.setGuests(guests);
        this.eventsRepository.save(event);
        //this.imageService.saveTicketImages(event, guest);
        if (guest.isSendInvitation()) {
            this.sendInvitation(event, guest);
        }

    }

    private void sendInvitation(Event event, Guest guest) {
        Profile guestProfile = guest.getProfile();
        if (StringUtils.isNotBlank(guestProfile.getEmail()) && StringUtils.isNotEmpty(guestProfile.getEmail())) {
            //this.mailsService.newGuest(guestProfile, event, guest.getTicket());
        }

        if (StringUtils.isNotBlank(guestProfile.getPhone()) && StringUtils.isNotEmpty(guestProfile.getPhone())) {
            this.sendWhatAppMessage(event, guest);
            //this.sendTwilioMessage(event, guest);

        }
    }

    private void sendTwilioMessage(Event event, Guest guest) {
        String azureImage = "https://zeevenimages.blob.core.windows.net/images/eo9arfovirt6dxzrythf.jpeg?sp=r&st=2022-06-26T08:43:58Z&se=2022-06-26T16:43:58Z&spr=https&sv=2021-06-08&sr=b&sig=8Sr%2BcyxBFrucDoCl5l3uEC01WAtFvHz3Htvqimtqc6E%3D";
        String linkWithExtension = String.format("%s/events/%s/tickets/%s.jpg", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId());
        Twilio.init(this.accountSid, this.authToken);
        Message message = null;
        try {
            /*
            message = Message.creator(
                            new com.twilio.type.PhoneNumber("whatsapp:+33761705745"),
                            new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
                            List.of(new URI(azureImage))).setBody("fpjzfpojzjjgpojrzfpojzpfzjpo")
                    .create();
            System.out.println(message.getSid());
*/
            message = Message.creator(
                            new com.twilio.type.PhoneNumber("+33761705745"),
                            new com.twilio.type.PhoneNumber("+18455769979"),
                            "This is the ship that made the Kessel Run in fourteen parsecs?")
                    .setMediaUrl(
                            Arrays.asList(URI.create(azureImage)))
                    .create();

            System.out.println(message.getSid());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendMappedWhatAppMessage(Event event, Guest guest) {

        Map<String, Object> language = new HashMap();
        language.put("code", Collections.singletonMap("code", "fr"));

        Map<String, Object> headerParameter = new HashMap();
        headerParameter.put("type", "image");
        String linkWithExtension = String.format("%s/events/%s/tickets/%s.jpg", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId());
        log.info("linkWithExtension " + linkWithExtension);
        headerParameter.put("image", Collections.singletonMap("link", linkWithExtension));

        Map<String, Object> headerParameters = new HashMap();
        headerParameters.put("parameters", List.of(headerParameter));
        Map<String, Object> header = new HashMap();
        header.put("type", "header");
        header.put("parameters", headerParameters);

        Map<String, Object> fistText = new HashMap();
        fistText.put("type", "text");
        fistText.put("text", String.format("%s %s", guest.getProfile().getFirstName(), guest.getProfile().getLastName().toUpperCase()));
        Map<String, Object> secondText = new HashMap();
        secondText.put("type", "text");
        secondText.put("text", event.getName());

        Map<String, Object> bodyParameters = new HashMap();
        bodyParameters.put("parameters", List.of(fistText, secondText));
        Map<String, Object> body = new HashMap();
        header.put("type", "body");
        header.put("parameters", bodyParameters);

        Map<String, Object> components = new HashMap();
        components.put("header", header);
        components.put("body", body);

        Map<String, Object> template = new HashMap();
        template.put("name", "user_invitation");
        template.put("language", language);
        template.put("components", components);

        Map<String, Object> textMessage = new HashMap();
        textMessage.put("messaging_product", "whatsapp");
        textMessage.put("recipient_type", "individual");
        textMessage.put("to", String.format("%s%s", guest.getProfile().getPhoneIndex(), guest.getProfile().getPhone()));
        textMessage.put("type", "template");
        textMessage.put("template", template);
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            this.textMessageService.mapMessage(objectMapper.writeValueAsString(textMessage));
        } catch (JsonProcessingException e) {
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
        //image.setId("775355320148280");

        String linkWithExtension = String.format("%s/events/%s/tickets/%s.jpg", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId());
        log.info("linkWithExtension " + linkWithExtension);
        image.setLink(linkWithExtension);

        //image.setLink("https://media.istockphoto.com/photos/taj-mahal-mausoleum-in-agra-picture-id1146517111?k=20&m=1146517111&s=612x612&w=0&h=vHWfu6TE0R5rG6DJkV42Jxr49aEsLN0ML-ihvtim8kk=");
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
        //textMessage.setType("image");
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
