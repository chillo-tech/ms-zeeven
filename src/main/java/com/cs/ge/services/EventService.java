package com.cs.ge.services;

import com.cs.ge.dto.Scan;
import com.cs.ge.entites.ApplicationMessage;
import com.cs.ge.entites.ApplicationMessageSchedule;
import com.cs.ge.entites.Category;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.EventMessage;
import com.cs.ge.entites.EventParams;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.Invitation;
import com.cs.ge.entites.Plan;
import com.cs.ge.entites.Schedule;
import com.cs.ge.entites.Table;
import com.cs.ge.entites.Template;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.EventStatus;
import com.cs.ge.enums.Role;
import com.cs.ge.enums.StockType;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.feign.FeignNotifications;
import com.cs.ge.repositories.EventMessageRepository;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.services.messages.EventMessageService;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.cs.ge.services.shared.SharedService;
import com.cs.ge.utils.UtilitaireService;
import com.google.api.client.util.Strings;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Function;
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
    public static final String DEFAULT_TABLE_NAME = "Contacts";
    private final FeignNotifications feignNotifications;
    private final EventRepository eventsRepository;
    private final ProfileService profileService;
    private final CategorieService categorieService;
    private final ASynchroniousNotifications aSynchroniousNotifications;
    private final UtilitaireService utilitaireService;
    private final StockService stockService;
    private final SharedService sharedService;
    private final EventMessageService eventMessageService;

    private final EventMessageRepository eventMessageRepository;

    public EventService(
            final FeignNotifications feignNotifications,
            final EventRepository eventsRepository,
            final ProfileService profileService,
            final CategorieService categorieService,
            final ASynchroniousNotifications aSynchroniousNotifications,
            final UtilitaireService utilitaireService, final StockService stockService, final SharedService sharedService, final EventMessageService eventMessageService, final EventMessageRepository eventMessageRepository) {
        this.feignNotifications = feignNotifications;
        this.eventsRepository = eventsRepository;
        this.profileService = profileService;
        this.categorieService = categorieService;
        this.aSynchroniousNotifications = aSynchroniousNotifications;
        this.utilitaireService = utilitaireService;
        this.stockService = stockService;
        this.sharedService = sharedService;
        this.eventMessageService = eventMessageService;
        this.eventMessageRepository = eventMessageRepository;
    }

    public List<Event> search() {
        final UserAccount authenticatedUser = this.profileService.getAuthenticateUser();
        if (authenticatedUser.getRole().equals(Role.ADMIN)) {
            return this.eventsRepository.findAll();
        }

        final String id = authenticatedUser.getId();
        return this.eventsRepository.findByAuthorId(id).collect(Collectors.toList());
    }

    private Map<String, Object> getDefaultData(final List<Guest> guestList) {
        final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        final String id = UUID.randomUUID().toString();
        final Table tableWithoutContact = this.getDefaultTable();
        final Plan plan = new Plan();
        plan.setId(id);
        plan.setPublicId(publicId);
        if (guestList == null) {
            plan.setContacts(new HashMap<>());
        } else {
            tableWithoutContact.setContactIds(guestList.stream().map(Guest::getPublicId).collect(Collectors.toSet()));
            final Map<String, Guest> newPlanContacts = guestList.stream().collect(Collectors.toMap(Guest::getPublicId, Function.identity()));
            plan.setContacts(newPlanContacts);
        }

        plan.setTables(Map.of(tableWithoutContact.getPublicId(), tableWithoutContact));
        plan.setTablesOrder(Set.of(tableWithoutContact.getPublicId()));

        return Map.of(
                "plan", plan,
                "table", tableWithoutContact
        );
    }

    private Table getDefaultTable() {
        final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        final String id = UUID.randomUUID().toString();
        final Table table = new Table();
        table.setId(id);
        table.setPublicId(publicId);
        table.setDeletable(false);
        table.setName(DEFAULT_TABLE_NAME);
        table.setType("CLASSIQUE");
        table.setSlug(this.sharedService.toSlug(table.getName()));
        return table;
    }

    public void add(Event event) {
        final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        final String id = UUID.randomUUID().toString();
        final UserAccount author = this.profileService.getAuthenticateUser();

        if (event.getName() == null) {
            event.setName(String.format("%s %s %s", event.getCategory().getLabel(), author.getFirstName(), author.getLastName()).toLowerCase());
        }

        //event.setAuthor(author);
        event.setAuthorId(author.getId());
        final List<Guest> guestList = event.getGuests().parallelStream().map(guest -> {
            guest.setPublicId(RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT));
            guest.setId(UUID.randomUUID().toString());
            guest.setTrial(true);
            return guest;
        }).collect(Collectors.toList());
        event.setGuests(guestList);

        final Map<String, Object> initialData = this.getDefaultData(guestList);

        //final EventStatus status = eventStatus(event.getDates());
        event.setStatus(INCOMMING);
        final Table initialTable = (Table) initialData.get("table");
        event.setTables(List.of(initialTable));
        event.setPlan((Plan) initialData.get("plan"));

        final List<ApplicationMessage> updatedApplicationMessages = event.getMessages().parallelStream().peek(applicationMessage -> applicationMessage.setId(id)).collect(Collectors.toList());
        event.setMessages(updatedApplicationMessages);

        event.setPublicId(publicId);
        final String slug = this.utilitaireService.makeSlug(event.getName());
        event.setSlug(format("%s-%s", slug, publicId));

        final Category category = this.categorieService.read(event.getCategory().getLabel());
        event.setCategory(category);

        event = this.eventsRepository.save(event);
        final String eventName = event.getName();
        this.eventMessageService.eventToEventMessages(event);
        /*
        //TODO
        this.aSynchroniousNotifications.sendEmail(
                null,
                event.getAuthor(),
                new HashMap<String, List<String>>() {{
                    this.put("event", Collections.singletonList(eventName));
                }},

                "ZEEVEN",
                "welcome.html",
                null,
                "Notre cadeau de bienvenue"
        );
         */
        //this.synchroniousNotifications.sendConfirmationMessage(event);
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

    public void addGuest(final String eventId, final Guest guest) throws IOException {
        final var event = this.read(eventId);

        final UserAccount userAccount = new UserAccount();
        BeanUtils.copyProperties(guest, userAccount);
        if (event.getParams().isContact()) {
            ValidationService.checkEmail(guest.getEmail());
            ValidationService.checkPhone(guest.getPhone());
            final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
            final String guestId = UUID.randomUUID().toString();
            guest.setId(guestId);
            guest.setTrial(true);

            guest.setPublicId(publicId);
            final String slug = this.utilitaireService.makeSlug(format("%s %s", guest.getFirstName(), guest.getLastName()));
            guest.setSlug(format("%s-%s", slug, publicId));

            List<Guest> guests = event.getGuests();
            this.utilitaireService.checkIfAccountIsInList(guests, guest);
            if (guests == null) {
                guests = new ArrayList<>();
            }
            guests.add(guest);
            event.setGuests(guests);

            // Mis à jour des plans
            Plan plan = event.getPlan();
            if (plan == null) {
                plan = (Plan) this.getDefaultData(null).get("plan");
            }

            final Map<String, Guest> planContacts = plan.getContacts();
            final List<Guest> guestList = planContacts.keySet().parallelStream().map(key -> planContacts.get(key)).collect(Collectors.toList());
            guestList.add(guest);
            final Map<String, Guest> newPlanContacts = guestList.stream().collect(Collectors.toMap(Guest::getPublicId, Function.identity()));
            plan.setContacts(newPlanContacts);

            final Map<String, Table> planTables = plan.getTables();
            final List<Table> tableList = planTables.keySet().parallelStream().map(key -> planTables.get(key)).collect(Collectors.toList());
            final int index = IntStream.range(0, tableList.size())
                    .filter(i -> DEFAULT_TABLE_NAME.equals(tableList.get(i).getName()))
                    .findFirst().orElse(-1);


            Table defaultTable = this.getDefaultTable();
            if (index > -1) {
                defaultTable = tableList.get(index);
            }
            Set<String> contactIds = defaultTable.getContactIds();
            if (contactIds == null) {
                contactIds = new HashSet<>();
            }
            contactIds.add(publicId);
            defaultTable.setContactIds(contactIds);
            if (index > -1) {
                tableList.set(index, defaultTable);
            } else {
                tableList.add(defaultTable);
            }

            final Map<String, Table> newPlanTables = tableList.stream().collect(Collectors.toMap(Table::getPublicId, Function.identity()));
            plan.setTables(newPlanTables);
            this.eventsRepository.save(event);
        }
    }

    public void deleteGuest(final String eventId, final String guestId) {
        final var event = this.read(eventId);
        List<Guest> guests = event.getGuests();
        guests = guests.stream().filter(currentGuest -> !currentGuest.getPublicId().equals(guestId)).collect(Collectors.toList());
        event.setGuests(guests);

        final Plan plan = event.getPlan();

        final Map<String, Guest> planContacts = plan.getContacts();
        planContacts.remove(guestId);
        plan.setContacts(planContacts);

        final Map<String, Table> planTables = plan.getTables();
        planTables.keySet().forEach(key -> {
            final Table table = planTables.get(key);
            final Set<String> contactIds = table.getContactIds();
            contactIds.remove(guestId);
            table.setContactIds(contactIds);
            planTables.replace(key, table);
        });
        plan.setTables(planTables);

        event.setPlan(plan);
        //TODO: Supprimper le QR CODE
        this.eventsRepository.save(event);
    }

    public List<Guest> guests(final String id) {
        final Event event = this.read(id);
        return event.getGuests();
    }

    public void addSchedule(final String eventId, final Schedule schedule) {
        final var event = this.read(eventId);
        if (event.getParams().isSchedule()) {
            final String guestId = UUID.randomUUID().toString();
            schedule.setId(guestId);

            final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
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
    }

    public void deleteSchedule(final String eventId, final String scheduleId) {
        final var event = this.read(eventId);
        List<Schedule> schedules = event.getSchedules();
        schedules = schedules.stream().filter(currentSchedule -> !currentSchedule.getPublicId().equals(scheduleId)).collect(Collectors.toList());
        event.setSchedules(schedules);
        this.eventsRepository.save(event);
    }

    public void addScan(final String eventId, final Scan scan) {
        log.info("Enregistrement du scan pour {}", eventId);
        final var event = this.read(eventId);

        final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        scan.setPublicId(publicId);

        List<Scan> scans = event.getScans();
        if (scans == null) {
            scans = new ArrayList<>();
        } else {
            scans = scans.stream().filter(current -> !current.getGuestPublicId().equals(scan.getPublicId())).collect(Collectors.toList());
        }
        scans.add(scan);
        event.setScans(scans);
        this.eventsRepository.save(event);
    }

    public void deleteScan(final String eventId, final String scanId) {
        final var event = this.read(eventId);
        List<Scan> scans = event.getScans();
        scans = scans.stream().filter(current -> !current.getGuestPublicId().equals(scanId)).collect(Collectors.toList());
        event.setScans(scans);
        this.eventsRepository.save(event);
    }

    public List<Schedule> schedules(final String id) {
        final Event event = this.read(id);
        return event.getSchedules();
    }

    public List<Table> tables(final String id) {
        final Event event = this.read(id);
        return event.getTables();
    }

    public void addTable(final String eventId, final Table table) {
        final var event = this.read(eventId);
        if (event.getParams().isTable()) {
            table.setId(UUID.randomUUID().toString());
            table.setSlug(this.sharedService.toSlug(table.getName()));
            final List<Table> tables = event.getTables();
            final Table exists = tables.stream().filter(currentTable -> currentTable.getSlug().equals(table.getSlug())).findFirst().orElse(null);
            if (exists == null) {
                final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
                table.setPublicId(publicId);

                tables.add(table);
                event.setTables(tables);

                Plan plan = event.getPlan();
                if (plan == null) {
                    plan = (Plan) this.getDefaultData(null).get("plan");
                }
                final Map<String, Table> planTables = plan.getTables();
                final List<Table> tableList = planTables.keySet().parallelStream().map(planTables::get).collect(Collectors.toList());
                tableList.add(table);
                final Map<String, Table> newPlanTables = tableList.stream().collect(Collectors.toMap(Table::getPublicId, Function.identity()));
                plan.setTables(newPlanTables);

                final Set<String> tablesOrder = plan.getTablesOrder();
                tablesOrder.add(table.getPublicId());
                plan.setTablesOrder(tablesOrder);
                event.setPlan(plan);
                this.eventsRepository.save(event);
            }

            //TODO Renvoyer et afficher un messaage indiquant que la table existe déjà
        }
    }

    public void deleteTable(final String eventId, final String tableId) {
        final var event = this.read(eventId);
        List<Table> tables = event.getTables();
        final Optional<Table> tableToDeleteAsOptional = tables.stream().filter(currentTable -> currentTable.getPublicId().equals(tableId)).findFirst();
        if (tableToDeleteAsOptional.isPresent() && tableToDeleteAsOptional.get().isDeletable()) {

            tables = tables.stream().filter(currentTable -> !currentTable.getPublicId().equals(tableId)).collect(Collectors.toList());
            event.setTables(tables);

            final Plan plan = event.getPlan();

            Set<String> tablesOrder = plan.getTablesOrder();
            tablesOrder = tablesOrder.stream().filter(itemId -> !itemId.equals(tableId)).collect(Collectors.toSet());
            plan.setTablesOrder(tablesOrder);

            final Map<String, Table> planTables = plan.getTables();
            final Table tableToDelete = planTables.get(tableId);
            final Set<String> contactIdsFromTableToDelete = tableToDelete.getContactIds();

            final Optional<String> defaultTableKey = planTables.entrySet()
                    .stream()
                    .filter(entry -> Objects.equals(entry.getValue().getName(), DEFAULT_TABLE_NAME))
                    .map(Map.Entry::getKey)
                    .findFirst();

            if (defaultTableKey.isPresent()) {
                final Table defaultTable = planTables.get(defaultTableKey.get());
                contactIdsFromTableToDelete.addAll(defaultTable.getContactIds());
                defaultTable.setContactIds(contactIdsFromTableToDelete);
                planTables.replace(defaultTableKey.get(), defaultTable);
            }
            planTables.remove(tableId);
            plan.setTables(planTables);
            event.setPlan(plan);

            //TODO: Supprimper le QR CODE
            this.eventsRepository.save(event);
        }
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
        final String authorId = event.getAuthorId();
        final UserAccount author = this.profileService.findById(authorId);
        final List<Guest> eventGuests = event.getGuests();
        final List<Channel> eventChannels = event.getChannels();
        final Map<Channel, Integer> channelsStatistics = this.stockService.getChannelsStatistics(authorId, eventChannels);
        final List<Channel> channelsToHandle = this.getChannelsToHandle(author.getEmail(), eventGuests, eventChannels, channelsStatistics);

        if (channelsToHandle.size() > 0) {
            log.info("Envoi des messages pour l'evenement {} sur {}", event.getName(), channelsToHandle.toString());
            this.eventMessageService.handleMessages(channelsToHandle, event);
        } else {
            // TODO ENVOYER UN MAIL
            //log.info("Pas assez de crédits pour envoyer des messages pour l'evenement {} sur {}", event.getName(), eventChannels.toString());
        }
    }

    private List<ApplicationMessage> getEventMessagesToKeep(final List<ApplicationMessage> messages, final List<ApplicationMessage> messagesToSend) {
        messages.removeAll(messagesToSend);
        return messages;
    }

    private void updateStocks(final String userId, final List<Channel> channelsToHandle, final int consumed) {
        channelsToHandle.parallelStream()
                .forEach(channel -> this.stockService
                        .update(userId, null, null, consumed, channel, StockType.DEBIT));
    }

    private List<Channel> getChannelsToHandle(final String email, final List<Guest> eventGuests, final List<Channel> eventChannels, final Map<Channel, Integer> channelsStatistics) {
        return eventChannels
                .stream().filter(channel -> {
                    if (channelsStatistics != null && channelsStatistics.size() > 0) {
                        return channelsStatistics.get(channel) != null && channelsStatistics.get(channel) >= eventGuests.size();
                    } else {
                        return false;
                    }
                }).collect(Collectors.toList());
    }

    private List<ApplicationMessage> sendMessages(final Event event, final List<ApplicationMessage> messagesTosend, final List<Channel> channelsToHandle) {
        final UserAccount author = this.profileService.findById(event.getAuthorId());
        return messagesTosend
                .parallelStream()
                .peek(applicationMessage -> {
                    final List<ApplicationMessageSchedule> schedules = applicationMessage.getSchedules();
                    final ApplicationMessageSchedule firstScheduleToHandle = schedules.parallelStream().filter(schedule -> !schedule.isHandled()).findFirst().orElse(null);
                    if (firstScheduleToHandle != null) {
                        final int firstScheduleToHandleIndex = IntStream.range(0, schedules.size())
                                .filter(i -> firstScheduleToHandle.isHandled() == schedules.get(i).isHandled())
                                .findFirst().orElse(-1);
                        this.aSynchroniousNotifications.sendEventMessage(
                                event,
                                author,
                                applicationMessage,
                                channelsToHandle,
                                null,
                                new HashMap<>()
                        );

                        firstScheduleToHandle.setHandled(true);
                        firstScheduleToHandle.setHandledDate(Instant.now());
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
        final List<Channel> channels = event.getChannels();

        final List<Object> queuedMessagesSatisticts = new ArrayList<>();
        channels.parallelStream().forEach(channel -> queuedMessagesSatisticts.addAll(
                Collections.nCopies(event.getGuests().size(), new HashMap<String, String>() {
                    {
                        this.put("chanel", channel.name());
                        this.put("status", "QUEUED");
                        this.put("eventId", event.getPublicId());
                    }
                })
        ));
        try {

            final List<Map<String, String>> statistics = this.feignNotifications.getStatistic(event.getId());

            final List<Object> staticsData = statistics.stream()
                    .filter(statistic -> List.of("QUEUED", "SENT", "DELIVERED", "OPENED", "UNIQUE_OPENED").contains(statistic.get("status").toString()))
                    .map(entry -> new HashMap<String, String>() {
                        {
                            this.put("chanel", entry.get("channel"));
                            this.put("status", entry.get("status"));
                            this.put("eventId", event.getPublicId());
                        }
                    }).collect(Collectors.toList());
            queuedMessagesSatisticts.addAll(staticsData);
        } catch (final Exception exception) {
            log.error("{}", exception);
        }

        return queuedMessagesSatisticts;
    }

    public void addPlan(final String id, final Plan plan) {
        final Event event = this.read(id);
        plan.setId(UUID.randomUUID().toString());
        plan.setPublicId(RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT));
        event.setPlan(plan);
        this.eventsRepository.save(event);
    }

    private String getTimeZone() {
        return ZonedDateTime.now(           // Capture the current moment in the wall-clock time used by the people of a certain region (a time zone).
                ZoneId.systemDefault()   // Get the JVM’s current default time zone. Can change at any moment during runtime. If important, confirm with the user.
        ).getZone().getId();
    }

    public void addInvitation(final String id, final Invitation invitation) {
        final Event event = this.read(id);
        if (event.getParams().isInvitation()) {
            final Template template = invitation.getTemplate();
            Set<Schedule> schedules = template.getSchedules();
            if (schedules != null) {
                schedules = schedules.parallelStream().peek(schedule -> {
                    schedule.setId(UUID.randomUUID().toString());
                    schedule.setPublicId(RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT));
                }).collect(Collectors.toSet());
                template.setSchedules(schedules);
            }

            template.setId(UUID.randomUUID().toString());
            template.setPublicId(RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT));
            invitation.setTemplate(template);

            invitation.setId(UUID.randomUUID().toString());
            invitation.setPublicId(RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT));
            event.setInvitation(invitation);
            this.eventsRepository.save(event);
        }
    }

    public void deleteInvitation(final String eventId, final String invitationPublicId) {
        final var event = this.read(eventId);
        event.setInvitation(new Invitation());
        this.eventsRepository.save(event);
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void sendMessages() {
        final Stream<Event> events = this.eventsRepository
                .findByStatusIn(List.of(INCOMMING, ACTIVE));
        events.filter(event -> !Strings.isNullOrEmpty(event.getAuthorId())).forEach(this::handleEvent);
    }

    @Scheduled(cron = "0 */1 * * * *")
    public void updateEventStatus() {
        final Stream<Event> events = this.eventsRepository
                .findByStatusIn(List.of(INCOMMING, ACTIVE));
        events.peek(event -> {
            final List<EventMessage> items = this.eventMessageRepository.findByEventIdAndIsHandled(event.getId(), false);
            if (items.isEmpty()) {
                event.setStatus(DISABLED);
                this.eventsRepository.save(event);
            }
        }).collect(Collectors.toList());
    }

    public void updateParams(final String eventId, final Map<String, Boolean> params) {
        final var event = this.read(eventId);
        final EventParams eventParams = event.getParams();
        params.keySet().forEach(param -> {
            try {
                org.apache.commons.beanutils.BeanUtils.setProperty(eventParams, param, params.get(param));
            } catch (final IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
        this.eventsRepository.save(event);
    }

    public void setTemplateParams(final String eventId, final String invitationPublicId, final Map<String, String> templateParams) {
        final var event = this.read(eventId);
        final var invitation = event.getInvitation();
        final var template = invitation.getTemplate();
        template.setParams(templateParams);
        invitation.setTemplate(template);
        event.setInvitation(invitation);
        this.eventsRepository.save(event);
    }
}
