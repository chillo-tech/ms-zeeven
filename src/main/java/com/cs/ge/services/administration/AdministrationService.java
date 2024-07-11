package com.cs.ge.services.administration;

import com.cs.ge.dto.ProfileDTO;
import com.cs.ge.dto.StockDTO;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.Stock;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.repositories.EventRepository;
import com.cs.ge.repositories.StockRepository;
import com.cs.ge.repositories.UtilisateurRepository;
import com.google.common.base.Strings;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.cs.ge.enums.Channel.EMAIL;
import static com.cs.ge.enums.Channel.QRCODE;
import static com.cs.ge.enums.Channel.SMS;
import static com.cs.ge.enums.Channel.WHATSAPP;
import static com.cs.ge.enums.StockType.CREDIT;

@Service
public class AdministrationService {
    private final UtilisateurRepository utilisateurRepository;
    private final StockRepository stockRepository;
    private final EventRepository eventRepository;

    public AdministrationService(final EventRepository eventRepository,
            final UtilisateurRepository utilisateurRepository, final StockRepository stockRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.stockRepository = stockRepository;
        this.eventRepository =eventRepository;
    }


    public void updateUserRole(final ProfileDTO profileDTO) {
        final UserAccount userAccount = this.utilisateurRepository.findByEmail(profileDTO.email()).orElseThrow(() -> new ApplicationException("Aucun compte pour cet email"));
        userAccount.setRole(profileDTO.role());
        this.utilisateurRepository.save(userAccount);
    }

    public void updateUserStock(final ProfileDTO profileDTO) {
        final UserAccount userAccount = this.utilisateurRepository.findByEmail(profileDTO.email()).orElseThrow(() -> new ApplicationException("Aucun compte pour cet email"));
        final List<Stock> stocks = new ArrayList<>();
        final StockDTO stockDTO = profileDTO.stock();
        if (stockDTO.email() > 0) {
            stocks.add(
                    new Stock(
                            null,
                            userAccount.getId(),
                            "default",
                            "",
                            EMAIL,
                            CREDIT,
                            stockDTO.email(),
                            LocalDateTime.now()
                    ));
        }
        if (stockDTO.sms() > 0) {
            stocks.add(
                    new Stock(
                            null,
                            userAccount.getId(),
                            "default",
                            "",
                            SMS,
                            CREDIT,
                            stockDTO.sms(),
                            LocalDateTime.now()
                    ));
        }
        if (stockDTO.qrcode() > 0) {
            stocks.add(
                    new Stock(
                            null,
                            userAccount.getId(),
                            "default",
                            "",
                            QRCODE,
                            CREDIT,
                            stockDTO.qrcode(),
                            LocalDateTime.now()
                    ));
        }
        if (stockDTO.whatsapp() > 0) {
            stocks.add(
                    new Stock(
                            null,
                            userAccount.getId(),
                            "default",
                            "",
                            WHATSAPP,
                            CREDIT,
                            stockDTO.whatsapp(),
                            LocalDateTime.now()
                    ));
        }

        this.stockRepository.saveAll(stocks);
    }

    public void updateEventParameters(Map<String, String> parameters) {
        String eventPublic = parameters.get("event");

        if(Strings.isNullOrEmpty(eventPublic)) {
            throw new ApplicationException("L'identifiant est requis");
        }

        Event event = this.eventRepository.findByPublicId(parameters.get("event")).orElseThrow(() -> new ApplicationException("Aucun évènement ne correspond à cet ID"));
        String author = parameters.get("author");
        if(!Strings.isNullOrEmpty(eventPublic)) {
            final UserAccount userAccount = this.utilisateurRepository.findByEmail(author).orElseThrow(() -> new ApplicationException("Aucun compte pour cet email"));
            event.setAuthorId(userAccount.getId());
        }

        this.eventRepository.save(event);
    }
}
