package com.cs.ge.services;

import com.cs.ge.entites.ApplicationMessage;
import com.cs.ge.entites.ApplicationMessageSchedule;
import com.cs.ge.entites.Category;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Schedule;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.EventStatus;
import com.cs.ge.enums.Role;
import com.cs.ge.enums.StockType;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.feign.FeignNotifications;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.cs.ge.services.qrcode.QRCodeGeneratorService;
import com.cs.ge.utils.UtilitaireService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.cs.ge.enums.EventStatus.ACTIVE;
import static com.cs.ge.enums.EventStatus.DISABLED;
import static com.cs.ge.enums.EventStatus.INCOMMING;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class EventService {
    private final FeignNotifications feignNotifications;
    private final EventRepository eventsRepository;
    private final ProfileService profileService;
    private final CategorieService categorieService;
    private final UtilisateursService utilisateursService;
    private final QRCodeGeneratorService qrCodeGeneratorService;
    private final ASynchroniousNotifications aSynchroniousNotifications;
    private final UtilitaireService utilitaireService;
    private final StockService stockService;

    public EventService(
            final FeignNotifications feignNotifications,
            final EventRepository eventsRepository,
            final ProfileService profileService,
            final CategorieService categorieService,
            final UtilisateursService utilisateursService,
            final QRCodeGeneratorService qrCodeGeneratorService,
            final ASynchroniousNotifications aSynchroniousNotifications,
            final UtilitaireService utilitaireService, final StockService stockService) {
        this.feignNotifications = feignNotifications;
        this.eventsRepository = eventsRepository;
        this.profileService = profileService;
        this.categorieService = categorieService;
        this.utilisateursService = utilisateursService;
        this.qrCodeGeneratorService = qrCodeGeneratorService;
        this.aSynchroniousNotifications = aSynchroniousNotifications;
        this.utilitaireService = utilitaireService;
        this.stockService = stockService;
    }

    public List<Event> search() {
        final UserAccount authenticatedUser = this.profileService.getAuthenticateUser();
        if (authenticatedUser.getRole().equals(Role.ADMIN)) {
            return this.eventsRepository.findAll();
        }

        final String id = authenticatedUser.getId();
        return this.eventsRepository.findByAuthorId(id).collect(Collectors.toList());
    }

    public void add(Event event) {

        if (event.getName() == null) {
            event.setName(String.format("%s %s %s", event.getCategory().getLabel(), event.getAuthor().getFirstName(), event.getAuthor().getLastName()).toLowerCase());
        }

        final UserAccount author = this.profileService.getAuthenticateUser();
        event.setAuthor(author);

        //final EventStatus status = eventStatus(event.getDates());
        event.setStatus(INCOMMING);

        final List<Guest> guestList = event.getGuests().parallelStream().map(guest -> {
            guest.setId(UUID.randomUUID().toString());
            guest.setPublicId(UUID.randomUUID().toString());
            guest.setTrial(true);
            return guest;
        }).collect(Collectors.toList());
        event.setGuests(guestList);

        final List<ApplicationMessage> updatedApplicationMessages = event.getMessages().parallelStream().map(applicationMessage -> {
            applicationMessage.setId(UUID.randomUUID().toString());
            return applicationMessage;
        }).collect(Collectors.toList());
        event.setMessages(updatedApplicationMessages);

        final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        event.setPublicId(publicId);
        final String slug = this.utilitaireService.makeSlug(event.getName());
        event.setSlug(format("%s-%s", slug, publicId));

        final Category category = this.categorieService.read(event.getCategory().getLabel());
        event.setCategory(category);

        event = this.eventsRepository.save(event);
        //this.synchroniousNotifications.sendConfirmationMessage(event);
        //this.handleEvent(event);
    }

    private static EventStatus eventStatus(final Set<Instant> dates) {
        final List<Instant> datesAsList = new ArrayList<>(dates);
        final List<Instant> sorted = datesAsList.stream()
                .distinct() // If you want only unique elements in the end List
                .sorted()
                .collect(Collectors.toList());
        final Date date = new Date();
        EventStatus status = EventStatus.DISABLED;
        final Instant now = Instant.now();
        if (sorted.get(0).isBefore(now)) {
            throw new ApplicationException("La date de votre évènement est invalide");
        }

        if (sorted.get(0).equals(now)) {
            status = ACTIVE;
        }

        if (sorted.get(sorted.size() - 1).isAfter(now)) {
            status = INCOMMING;
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

    public byte[] getGuestEventTicket(final String guestId, final String eventId) {
        final Event event = this.read(eventId);
        final Optional<Guest> optionalGuest = event.getGuests().stream().filter(g -> g.getPublicId().equals(guestId)).findFirst();
        final Guest guest = optionalGuest.orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Aucune enttité ne correspond au critères fournis"));

        return null; //Base64.getDecoder().decode(guest.getTicket());
    }

    public void addGuest(final String eventId, final Guest guestProfile) {
        final var event = this.read(eventId);
        ValidationService.checkEmail(guestProfile.getEmail());
        ValidationService.checkPhone(guestProfile.getPhone());
        final String guestId = UUID.
                randomUUID().toString();
        guestProfile.setId(guestId);
        guestProfile.setTrial(true);

        final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        guestProfile.setPublicId(publicId);
        final String slug = this.utilitaireService.makeSlug(format("%s %s", guestProfile.getFirstName(), guestProfile.getLastName()));
        guestProfile.setSlug(format("%s-%s", slug, publicId));
        this.qrCodeGeneratorService.guestQRCODE(event, guestProfile);
        //guest.setTicket(guestQRCODE);
        List<Guest> guests = event.getGuests();
        if (guests == null) {
            guests = new ArrayList<>();
        }
        guests.add(guestProfile);
        event.setGuests(guests);
        this.eventsRepository.save(event);
        //this.imageService.saveTicketImages(event, guest);
        /*
        if (guest.isSendInvitation()) {
            this.sendInvitation(event, guest);
        }

         */

    }

    /*
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
            ApplicationMessage message = null;
            try {

                message = ApplicationMessage.creator(
                                new com.twilio.type.PhoneNumber("whatsapp:+33761705745"),
                                new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
                                List.of(new URI(azureImage))).setBody("fpjzfpojzjjgpojrzfpojzpfzjpo")
                        .create();
                System.out.println(message.getSid());

                message = ApplicationMessage.creator(
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

            // this.textMessageService.mapMessage(objectMapper.writeValueAsString(textMessage));
        }

        private void sendWhatAppMessage(Event event, Guest guest) {
            String azureImage = "https://zeevenimages.blob.core.windows.net/images/eo9arfovirt6dxzrythf.jpeg?sp=r&st=2022-06-26T08:43:58Z&se=2022-06-26T16:43:58Z&spr=https&sv=2021-06-08&sr=b&sig=8Sr%2BcyxBFrucDoCl5l3uEC01WAtFvHz3Htvqimtqc6E%3D";
            String apiImage = "https://api.zeeven.chillo.fr/events/19508513/tickets/08535166.jpg";
            Profile guestProfile = guest.getProfile();


            Template template = new Template();
            template.setName("user_invitation");
            template.setLanguage(new Language("fr"));

            Image image = new Image();
            String link = String.format("%s/ticket?event=%s&guest=%s", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId());
            log.info("Link information " + link);
            //image.setId("775355320148280");

            String linkWithExtension = String.format("%s/events/%s/tickets/%s.jpg", this.imagesHost, event.getPublicId(), guest.getProfile().getPublicId());
            log.info("linkWithExtension " + linkWithExtension);
            image.setLink(apiImage);

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
    */
    public void deleteGuest(final String eventId, final String guestId) {
        final var event = this.read(eventId);
        List<Guest> guests = event.getGuests();
        guests = guests.stream().filter(currentGuest -> !currentGuest.getPublicId().equals(guestId)).collect(Collectors.toList());
        event.setGuests(guests);

        //TODO: Supprimper le QR CODE
        this.eventsRepository.save(event);
    }

    public List<Guest> guests(final String id) {
        final Event event = this.read(id);
        return event.getGuests();
    }

    public void addSchedule(final String eventId, final Schedule schedule) {
        final var event = this.read(eventId);
        final String guestId = UUID.randomUUID().toString();
        schedule.setId(guestId);

        final String publicId = RandomStringUtils.randomAlphanumeric(20).toLowerCase(Locale.ROOT);
        schedule.setPublicId(publicId);
        final String slug = this.utilitaireService.makeSlug(schedule.getTitle());
        schedule.setSlug(format("%s-%s", slug, publicId));

        List<Schedule> schedules = event.getSchedules();
        if (schedules == null) {
            schedules = new ArrayList<>();
        }
        schedules.add(schedule);
        event.setSchedules(schedules);
        this.eventsRepository.save(event);
    }

    public void deleteSchedule(final String eventId, final String scheduleId) {
        final var event = this.read(eventId);
        List<Schedule> schedules = event.getSchedules();
        schedules = schedules.stream().filter(currentSchedule -> !currentSchedule.getPublicId().equals(scheduleId)).collect(Collectors.toList());
        event.setSchedules(schedules);
        this.eventsRepository.save(event);
    }

    public List<Schedule> schedules(final String id) {
        final Event event = this.read(id);
        return event.getSchedules();
    }

    @Async
    public void sendInvitations(final String id, final Set<String> guestIds) {
        final Event event = this.read(id);
        final List<String> guestIdsAsList = Lists.newArrayList(guestIds);
        event.getGuests()
                .stream()
                .filter(guest -> guestIdsAsList.contains(guest.getPublicId()))
                .forEach(profile -> log.info("Send Invitation"));

    }

    private void handleEvent(final Event event) {
        log.info("Gestion des messages pour l'evenement {}", event.getName());

        final List<Guest> eventGuests = event.getGuests();
        final List<Channel> eventChannels = event.getChannels();
        final Map<Channel, Integer> channelsStatistics = this.stockService.getChannelsStatistics(event.getAuthor().getId(), eventChannels);
        final UserAccount author = event.getAuthor();
        final List<Channel> channelsToHandle = this.getChannelsToHandle(author.getEmail(), eventGuests, eventChannels, channelsStatistics);

        log.info("Envoi des messages pour l'evenement {} sur {}", event.getName(), channelsToHandle.toString());
        if (channelsToHandle.size() > 0) {
            final List<ApplicationMessage> messagesToSend = this.getEventMessagesToSend(event.getMessages());
            final List<ApplicationMessage> messagesToKeep = this.getEventMessagesToKeep(event.getMessages(), messagesToSend);
            final boolean isDisabled = event.getMessages().size() == messagesToSend.size();
            EventStatus eventStatus = ACTIVE;
            if (isDisabled) {
                eventStatus = DISABLED;
            }
            event.setStatus(eventStatus);
            if (messagesToSend.size() > 0) {
                final List<ApplicationMessage> sentMessages = this.sendMessages(event, messagesToSend, channelsToHandle);
                messagesToKeep.addAll(sentMessages);
                event.setMessages(messagesToKeep);
                this.updateStocks(event.getAuthor(), channelsToHandle, event.getGuests().size());
            }
            this.eventsRepository.save(event);
        }

    }

    private List<ApplicationMessage> getEventMessagesToKeep(final List<ApplicationMessage> messages, final List<ApplicationMessage> messagesToSend) {
        messages.removeAll(messagesToSend);
        return messages;
    }

    private void updateStocks(final UserAccount author, final List<Channel> channelsToHandle, final int consumed) {
        channelsToHandle.parallelStream()
                .forEach(channel -> this.stockService
                        .update(author.getId(), null, null, consumed, channel, StockType.DEBIT));
    }

    private List<Channel> getChannelsToHandle(final String email, final List<Guest> eventGuests, final List<Channel> eventChannels, final Map<Channel, Integer> channelsStatistics) {
        return eventChannels
                .parallelStream().filter(channel -> {
                    if (channelsStatistics != null && channelsStatistics.size() > 0) {
                        return channelsStatistics.get(channel) != null && channelsStatistics.get(channel) >= eventGuests.size();
                    } else {
                        return false;
                    }
                }).collect(Collectors.toList());
    }

    private List<ApplicationMessage> sendMessages(final Event event, final List<ApplicationMessage> messagesTosend, final List<Channel> channelsToHandle) {
        return messagesTosend
                .parallelStream()
                .peek(applicationMessage -> {
                    final List<ApplicationMessageSchedule> schedules = applicationMessage.getSchedules();
                    final ApplicationMessageSchedule firstScheduleToHandle = schedules.parallelStream().filter(schedule -> !schedule.isHandled()).findFirst().orElse(null);
                    if (firstScheduleToHandle != null) {
                        final int firstScheduleToHandleIndex = IntStream.range(0, schedules.size())
                                .filter(i -> firstScheduleToHandle.isHandled() == schedules.get(i).isHandled())
                                .findFirst().orElse(-1);
                        this.aSynchroniousNotifications.sendEventMessage(event, applicationMessage, channelsToHandle);

                        firstScheduleToHandle.setHandled(true);
                        firstScheduleToHandle.setHandledDate(LocalDateTime.now());
                        schedules.set(firstScheduleToHandleIndex, firstScheduleToHandle);
                        applicationMessage.setSchedules(schedules);
                    }

                })
                .collect(Collectors.toList());
    }

    private List<ApplicationMessage> getEventMessagesToSend(final List<ApplicationMessage> messages) {
        return messages
                .parallelStream()
                .filter(message -> {
                    if (message.getSchedules() == null) {
                        message.setSchedules(new ArrayList<>());
                    }
                    return this.isMessageTobeSend(message.getSchedules());
                })
                .collect(Collectors.toList());
    }

    private boolean isMessageTobeSend(final List<ApplicationMessageSchedule> schedules) {
        final ApplicationMessageSchedule firstScheduleToHandle = schedules.parallelStream().filter(schedule -> !schedule.isHandled()).findFirst().orElse(null);
        if (firstScheduleToHandle == null) {
            return false;
        }
        if (firstScheduleToHandle.getTimezone() == null) {
            firstScheduleToHandle.setTimezone(this.getTimeZone());
        }
        final TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of(firstScheduleToHandle.getTimezone()));
        log.info("Date du message {} position {}", firstScheduleToHandle.getDate(), timeZone.getDisplayName());

        final Instant firstScheduleToHandleDate = firstScheduleToHandle.getDate().atZone(timeZone.toZoneId()).toInstant().truncatedTo(ChronoUnit.MINUTES);

        final Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        final ZonedDateTime zonedDateTime = now.atZone(timeZone.toZoneId());

        boolean send = zonedDateTime.toInstant().truncatedTo(ChronoUnit.SECONDS).isAfter(firstScheduleToHandleDate);
        if (!send) {
            send = firstScheduleToHandleDate.equals(now);
        }
        return send;
    }

    public List<Object> statistics(final String id) {
        final Event event = this.read(id);
        final List<Map<String, String>> statistics = this.feignNotifications.getStatistic(event.getId());
        return statistics.stream()
                .filter(statistic -> List.of("QUEUED", "SENT", "DELIVERED", "OPENED", "UNIQUE_OPENED").contains(statistic.get("status").toString()))
                .map(entry -> new HashMap<String, String>() {
                    {
                        this.put("chanel", entry.get("channel"));
                        this.put("status", entry.get("status"));
                        this.put("eventId", event.getPublicId());
                    }
                }).collect(Collectors.toList());
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void sendMessages() {
        final Stream<Event> events = this.eventsRepository
                .findByStatusIn(List.of(INCOMMING, ACTIVE));
        events.parallel().forEach(this::handleEvent);
    }

    private String getTimeZone() {
        return ZonedDateTime.now(           // Capture the current moment in the wall-clock time used by the people of a certain region (a time zone).
                ZoneId.systemDefault()   // Get the JVM’s current default time zone. Can change at any moment during runtime. If important, confirm with the user.
        ).getZone().getId();
    }

}
