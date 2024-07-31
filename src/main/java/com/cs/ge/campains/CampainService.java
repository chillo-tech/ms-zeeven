package com.cs.ge.campains;

import com.cs.ge.dto.ApplicationNotification;
import com.cs.ge.dto.MessageProfile;
import com.cs.ge.entites.BaseApplicationMessage;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.EventStatus;
import com.cs.ge.enums.Role;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.notifications.entity.Notification;
import com.cs.ge.notifications.service.NotificationService;
import com.cs.ge.services.ProfileService;
import com.cs.ge.services.StockService;
import com.cs.ge.services.shared.SharedService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cs.ge.enums.EventStatus.*;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Slf4j
@AllArgsConstructor
@Service
public class CampainService {
    private final CampainRepository campainRepository;
    private final ProfileService profileService;
    private final StockService stockService;
    private final SharedService sharedService;
    private final NotificationService notificationService;

    private static List<Channel> getChannelsToHandle(final String email, final List<Guest> eventGuests, final List<Channel> eventChannels, final Map<Channel, Integer> channelsStatistics) {
        return eventChannels
                .stream().filter(channel -> {
                    if (channelsStatistics != null && channelsStatistics.size() > 0) {
                        return channelsStatistics.get(channel) != null && channelsStatistics.get(channel) >= eventGuests.size();
                    } else {
                        return false;
                    }
                }).collect(Collectors.toList());
    }

    public void add(final Campain campain) {
        final UserAccount author = this.profileService.getAuthenticateUser();
        final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        campain.setPublicId(publicId);
        campain.setAuthorId(author.getId());
        campain.setStatus(EventStatus.INCOMMING);
        this.campainRepository.save(campain);
    }

    public List<Campain> search() {
        return this.campainRepository.findAll();
    }

    @Scheduled(cron = "0 0/2 * * * *")
    public void sendInvitations() {
        final Stream<Campain> campains = this.campainRepository
                .findByStatusIn(List.of(INCOMMING, ACTIVE));
        campains
                .filter(campain -> {
                    boolean isToBehandled = !campain.getStatus().equals(DISABLED);
                    final String zoneId = "Europe/Paris";
                    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
                    final String dateTime = String.format("%s %s:00 %s", campain.getDate(), campain.getTime(), zoneId);
                    final ZonedDateTime eventZonedDateTime = ZonedDateTime.parse(dateTime, formatter);

                    final ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of(zoneId));
                    if (eventZonedDateTime.toInstant().isBefore(now.toInstant())) {
                        isToBehandled = true;
                    }
                    return isToBehandled;
                })
                .forEach((campain) -> this.handleEvent(campain, FALSE));
    }

    private void handleEvent(final Campain campain, final boolean simulate) {
        final String authorId = campain.getAuthorId();
        List<Guest> contacts = campain.getGuests();
        if (simulate) {
            contacts = this.profileService.findByRole(Role.ADMIN).stream().map(user -> {
                final Guest guest = new Guest();
                guest.setTrial(true);
                guest.setId(UUID.randomUUID().toString());
                guest.setPublicId(RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT));
                guest.setCivility(user.getCivility());
                guest.setFirstName(user.getFirstName());
                guest.setLastName(user.getLastName());
                guest.setPhoneIndex(user.getPhoneIndex());
                guest.setPhone(user.getPhone());
                guest.setEmail(user.getEmail());
                return guest;
            }).toList();
        }
        final UserAccount author = this.profileService.findById(authorId);
        final List<Channel> channels = campain.getChannels();
        final Map<Channel, Integer> channelsStatistics = this.stockService.getChannelsStatistics(authorId, channels);
        final List<Channel> channelsToHandle = this.getChannelsToHandle(author.getEmail(), contacts, channels, channelsStatistics);

        final BaseApplicationMessage baseApplicationMessage = BaseApplicationMessage.builder().informations(List.of(campain.getInformations())).text(campain.getMessage()).build();
        final Map<String, List<Object>> params = this.sharedService.messageParameters(baseApplicationMessage);
        params.put("trial", List.of(String.valueOf(author.isTrial())));
        if (channelsToHandle.isEmpty()) {
            // TODO ENVOYER UN MAIL
            log.info("Pas assez de crédits pour envoyer des messages pour la campagne {} sur {}", campain.getName(), campain.getChannels().toString());
        } else {
            log.info("Envoi des messages pour la campagne {} sur {}", campain.getName(), channelsToHandle.toString());
            final List<MessageProfile> messageProfiles = contacts.stream().map(to -> this.sharedService.guestToMessageProfile(to)).collect(Collectors.toUnmodifiableList());
            final ApplicationNotification applicationNotification = new ApplicationNotification(
                    "ZEEVEN",
                    null,
                    campain.getName(),
                    RandomStringUtils.random(8, true, true),
                    RandomStringUtils.random(8, true, true),
                    campain.getMessage(),
                    params,
                    campain.getChannels(),
                    this.sharedService.userAccountToMessageProfile(author),
                    messageProfiles
            );
            final Notification notification = this.sharedService.applicationNotificationToNotification(applicationNotification);
            this.notificationService.send(notification.getApplication(), notification, notification.getChannels().stream().toList());
            if (!simulate) {
                campain.setStatus(DISABLED);
                this.campainRepository.save(campain);
            }
        }
    }

    void simulate(final String id) {
        final Campain campain = this.campainRepository.findByPublicId(id).orElseThrow(
                () -> new ApplicationException("La campagne n'existe pas"));
        this.handleEvent(campain, TRUE);
    }

    public void delete(final String id) {
        final UserAccount userAccount = this.profileService.getAuthenticateUser();
        if (userAccount.getRole().equals(Role.ADMIN) || userAccount.getRole().equals(Role.SUPER_ADMIN)) {
            this.campainRepository.deleteByPublicId(id);
        }
    }
}
