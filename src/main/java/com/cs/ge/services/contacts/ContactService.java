package com.cs.ge.services.contacts;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.repositories.UtilisateurRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class ContactService {

    private final EventRepository eventsRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ContactService(final EventRepository eventsRepository, final UtilisateurRepository utilisateurRepository) {
        this.eventsRepository = eventsRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Scheduled(cron = "@hourly")
    public void updateUserContcats() {
        log.info("Mis à jour des contacts");
        final List<UserAccount> userAccounts = this.utilisateurRepository.findAll();
        userAccounts.forEach(userAccount -> {
            log.info("Mis à jour des contacts pour {}", userAccount.getPublicId());
            final Stream<Event> userEvents = this.eventsRepository.findByAuthorId(userAccount.getId());
            final List<Guest> guests = userEvents.parallel().flatMap(event -> event.getGuests().stream()).collect(Collectors.toList());
            userAccount.setContacts(guests);
            this.utilisateurRepository.save(userAccount);
        });
    }
}
