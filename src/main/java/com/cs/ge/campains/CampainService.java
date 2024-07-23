package com.cs.ge.campains;

import com.cs.ge.entites.Guest;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Channel;
import com.cs.ge.enums.EventStatus;
import com.cs.ge.services.ProfileService;
import com.cs.ge.services.StockService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cs.ge.enums.EventStatus.ACTIVE;
import static com.cs.ge.enums.EventStatus.INCOMMING;

@Slf4j
@AllArgsConstructor
@Service
public class CampainService {
    private final CampainRepository campainRepository;
    private final ProfileService profileService;
    private final StockService stockService;

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

    @Scheduled(cron = "0 0/5 * * * *")
    public void sendInvitations() {
        final Stream<Campain> events = this.campainRepository
                .findByStatusIn(List.of(INCOMMING, ACTIVE));
        events
                .forEach(this::handleEvent);
    }

    private void handleEvent(final Campain campain) {
        final String authorId = campain.getAuthorId();
        final UserAccount author = this.profileService.findById(authorId);
        final List<Guest> contacts = campain.getContacts();
        final List<Channel> channels = campain.getChannels();
        final Map<Channel, Integer> channelsStatistics = this.stockService.getChannelsStatistics(authorId, channels);
        final List<Channel> channelsToHandle = this.getChannelsToHandle(author.getEmail(), contacts, channels, channelsStatistics);

        if (channelsToHandle.size() > 0) {
            log.info("Envoi des messages pour l'evenement {} sur {}", campain.getName(), channelsToHandle.toString());
            //this.invitationService.handleEvent(event);
            //this.eventMessageService.handleMessages(channelsToHandle, event);
        } else {
            // TODO ENVOYER UN MAIL
            //log.info("Pas assez de crédits pour envoyer des messages pour l'evenement {} sur {}", event.getName(), eventChannels.toString());
        }
    }
}
