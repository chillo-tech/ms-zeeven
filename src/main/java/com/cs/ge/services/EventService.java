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
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@Service
public class EventService {
    private final EventRepository eventsRepository;
    private final MailsService mailsService;
    private final ImageService imageService;
    private final TextMessageService textMessageService;
    private final ValidationService validationService;
    private final ProfileService profileService;
    private final QRCodeGeneratorService qrCodeGeneratorService;
    @Value("${resources.images.host}")
    private final String imagesHost;

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
        String publicId = RandomStringUtils.randomAlphanumeric(20).toUpperCase();
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
            Text text = new Text();
            text.setBody("Votre invitation");
            text.setPreview_url(false);

            Template template = new Template();
            template.setName("zeeven");
            template.setLanguage(new Language());

            Image image = new Image();
            image.setLink(String.format("%s/events/%s/tickets/%s.jpg", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId()));
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
            textMessage.setType("template");
            textMessage.setTo(String.format("237%s", guestProfile.getPhone()));
            this.textMessageService.message(textMessage);
        }
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

        String publicId = RandomStringUtils.randomAlphanumeric(20).toUpperCase();
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
