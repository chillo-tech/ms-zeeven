package com.cs.ge.services;

import com.cs.ge.entites.Adresse;
import com.cs.ge.entites.Category;
import com.cs.ge.entites.Evenement;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.EventStatus;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.repositories.AdresseRepository;
import com.cs.ge.repositories.EvenementsRepository;
import com.cs.ge.services.notifications.SynchroniousNotifications;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class EvenementsService {
    private final EvenementsRepository evenementsRepository;
    private final AdresseRepository adresseRepository;
    private final UtilisateursService utilisateursService;
    private final CategorieService categorieService;
    private final SynchroniousNotifications synchroniousNotifications;

    public List<Evenement> search() {
        return this.evenementsRepository.findAll();
    }

    public void add(final Evenement evenement) {

        if (evenement.getNom() == null || evenement.getNom().trim().isEmpty() || evenement.getUserAccount() == null) {
            throw new ApplicationException("Champs obligatoire");
        }
        EventStatus status = EvenementsService.eventStatus(evenement.getDateDebut(), evenement.getDateFin());
        evenement.setStatut(status);

        if (evenement.getAdresse() != null) {
            Adresse adresse = this.adresseRepository.save(evenement.getAdresse());
            evenement.setAdresse(adresse);
        }
        Category category = this.categorieService.read(evenement.getCategory().getLabel());
        evenement.setCategory(category);
        UserAccount userAccount = this.utilisateursService.readOrSave(evenement.getUserAccount());
        evenement.setUserAccount(userAccount);
        Evenement saved = this.evenementsRepository.save(evenement);
        //this.synchroniousNotifications.sendConfirmationMessage(saved);
    }

    public void deleteEvenement(final String id) {
        this.evenementsRepository.deleteById(id);
    }

    public void updateEvenement(final String id, final Evenement evenement) {
        final Optional<Evenement> current = this.evenementsRepository.findById(id);
        if (current.isPresent()) {
            final Evenement foundEvents = current.get();
            foundEvents.setId(id);
            foundEvents.setNom(evenement.getNom());
            foundEvents.setStatut(evenement.getStatut());
            foundEvents.setAdresse(evenement.getAdresse());
            foundEvents.setDateDebut(evenement.getDateDebut());
            foundEvents.setHeureDebut(evenement.getHeureDebut());
            foundEvents.setHeureFin(evenement.getHeureFin());
            this.evenementsRepository.save(foundEvents);
        }
    }

    public Evenement read(final String id) {
        return this.evenementsRepository.findById(id).orElseThrow(
                () -> new ApplicationException("Aucune enttité ne correspond au critères fournis")
        );
    }

    public void addInvites(final String id, final Guest guest) {
        final Evenement evenement = this.read(id);
        List<Guest> guests = evenement.getInvites();
        if (guests == null) {
            guests = new ArrayList<>();
        }
        guests.add(guest);
        evenement.setInvites(guests);
        this.evenementsRepository.save(evenement);
    }


    private static EventStatus eventStatus(final Date dateDebut, final Date dateFin) {
        final Date date = new Date();
        EventStatus status = EventStatus.DISABLED;

        if (dateDebut.before(date)) {
            throw new ApplicationException("La date de votre évènement est invalide");
        }

        if (dateDebut.equals(date)) {
            status = EventStatus.ACTIVE;
        }

        if (dateFin.after(date)) {
            status = EventStatus.INCOMMING;
        }

        return status;
    }

}
