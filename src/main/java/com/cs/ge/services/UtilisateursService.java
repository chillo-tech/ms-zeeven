package com.cs.ge.services;

import com.cs.ge.entites.Guest;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.entites.Verification;
import com.cs.ge.enums.GuestType;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.repositories.UtilisateurRepository;
import com.cs.ge.services.google.GoogleContactService;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import com.cs.ge.utils.UtilitaireService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cs.ge.enums.Role.CUSTOMER;
import static com.cs.ge.utils.Data.DEFAULT_STOCK_SIZE;
import static com.cs.ge.utils.UtilitaireService.valEmail;
import static com.cs.ge.utils.UtilitaireService.valNumber;

@Slf4j
@Service
public class UtilisateursService {

    private final GoogleContactService googleContactService;
    private final UtilisateurRepository utilisateurRepository;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final StockService stockService;
    private final ProfileService profileService;
    private final ASynchroniousNotifications asynchroniousNotifications;
    private final UtilitaireService utilitaireService;

    public UtilisateursService(
            final GoogleContactService googleContactService, final UtilisateurRepository utilisateurRepository,
            final VerificationService verificationService,
            final PasswordEncoder passwordEncoder,
            final StockService stockService,
            final ProfileService profileService, final ASynchroniousNotifications aSynchroniousNotifications, final UtilitaireService utilitaireService) {
        this.googleContactService = googleContactService;
        this.utilisateurRepository = utilisateurRepository;
        this.verificationService = verificationService;
        this.passwordEncoder = passwordEncoder;
        this.stockService = stockService;
        this.profileService = profileService;

        this.asynchroniousNotifications = aSynchroniousNotifications;
        this.utilitaireService = utilitaireService;
    }


    public void updatePassword(final String code, final String password) {
        final Verification verification = this.verificationService.getByCode(code);

        UserAccount userAccount = verification.getUserAccount();
        userAccount = this.utilisateurRepository.findById(userAccount.getId()).orElseThrow(() -> new ApplicationException("aucun compte pour ce code"));
        userAccount.setEnabled(true);
        final String encodedPassword = this.passwordEncoder.encode(password);
        userAccount.setPassword(encodedPassword);

        verification.setActive(false);

        this.utilisateurRepository.save(userAccount);
        this.verificationService.updateCode(verification.getId(), verification);
    }

    public void resetPasswordLink(final String email) {
        final UserAccount userAccount = this.utilisateurRepository.findByEmail(email).orElseThrow(() -> new ApplicationException("Aucun compte ne correspond à cet identifiant"));
        final Verification verification = this.verificationService.createCode(userAccount);
        this.asynchroniousNotifications.sendEmail(
                null,
                userAccount,
                new HashMap<String, List<String>>() {{
                    this.put("code", List.of(verification.getCode()));
                }},

                "ZEEVEN",
                "activation.html",
                null,
                "Créez un nouveau mot de passe"
        );
    }

    public void activate(final String code) {
        final Verification verification = this.verificationService.getByCode(code);
        UserAccount userAccount = verification.getUserAccount();
        userAccount = this.utilisateurRepository.findById(userAccount.getId()).orElseThrow(() -> new ApplicationException("aucun compte pour ce code"));
        userAccount.setEnabled(true);
        userAccount.setTrial(true);

        final LocalDateTime expiration = verification.getDateExpiration().truncatedTo(ChronoUnit.MINUTES);
        final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (now.isAfter(expiration)) {
            throw new ApplicationException("Votre compte est déjà actif ou votre code a expiré");
        }
        this.utilisateurRepository.save(userAccount);
        verification.setActive(false);
        this.verificationService.updateCode(verification.getId(), verification);
        this.stockService.generateDefaultStocks(userAccount.getId());
        this.asynchroniousNotifications.sendEmail(
                null,
                userAccount,
                new HashMap<String, List<String>>() {{
                    this.put("stock", List.of("" + DEFAULT_STOCK_SIZE));
                }},

                "ZEEVEN",
                "welcome.html",
                null,
                "Notre cadeau de bienvenue"
        );

        final Map<String, List<String>> params = new HashMap<String, List<String>>();
        params.put("name", List.of(String.format("%s %s", userAccount.getFirstName(), userAccount.getLastName())));
        this.asynchroniousNotifications.sendEmail(
                null,
                null,
                params,

                "ZEEVEN",
                "new-account.html",
                null,
                "Nouveau compte"
        );
    }

    public void validationUsername(final String username) {
        final Optional<UserAccount> exist = this.utilisateurRepository.findByEmail(username);
        if (exist.isPresent()) {
            throw new ApplicationException("Username existe déjà");
        }
    }

    public UserAccount readOrSave(final UserAccount userAccount) {
        final Optional<UserAccount> exist = this.utilisateurRepository.findByPhoneOrMail(userAccount.getEmail(), userAccount.getPhoneIndex(), userAccount.getPhone());
        return exist.orElseGet(() -> this.utilisateurRepository.save(userAccount));
    }

    public void add(final UserAccount userAccount) { // en entrée je dois avoir quelque chose sous la forme d'un UserAccount de type userAccount
        this.validationUsername(userAccount.getUsername());
        String lastName = userAccount.getLastName();
        lastName = lastName.toUpperCase();
        userAccount.setLastName(lastName);
        this.utilisateurRepository.save(userAccount);
    }

    public List<UserAccount> search() {
        return this.utilisateurRepository.findAll();
    }

    public void deleteUtilisateur(final String id) {
        this.utilisateurRepository.deleteById(id);
    }

    public void updateUtilisateur(final String id, final UserAccount userAccount) {
        final Optional<UserAccount> current = this.utilisateurRepository.findById(id);
        if (current.isPresent()) {
            final UserAccount foundUser = current.get();
            foundUser.setId(id);
            foundUser.setCivility(userAccount.getCivility());
            foundUser.setFirstName(userAccount.getFirstName());
            foundUser.setLastName(userAccount.getLastName());
            foundUser.setEmail(userAccount.getEmail());
            foundUser.setPhoneIndex(userAccount.getPhoneIndex());
            foundUser.setPhone(userAccount.getPhone());
            this.utilisateurRepository.save(foundUser);
        }
    }

    public void updateUtilisateurStocks(final String id, final UserAccount userAccount) {
        final Optional<UserAccount> current = this.utilisateurRepository.findById(id);
        if (current.isPresent()) {
            final UserAccount foundUser = current.get();
            foundUser.setStocks(userAccount.getStocks());
            this.utilisateurRepository.save(foundUser);
        }
    }


    public void inscription(final UserAccount userAccount) throws ApplicationException {
        valEmail(userAccount.getUsername());
        valNumber(userAccount.getUsername());
        this.utilitaireService.checkAccount(userAccount);
        userAccount.setRole(CUSTOMER);
        final String encodedPassword = this.passwordEncoder.encode(userAccount.getPassword());
        userAccount.setPassword(encodedPassword);
        this.utilisateurRepository.save(userAccount);
        final Verification verification = this.verificationService.createCode(userAccount);

        if (userAccount.getEmail() != null) {
            this.asynchroniousNotifications.sendEmail(
                    null,
                    userAccount,
                    new HashMap<String, List<String>>() {{
                        this.put("code", List.of(verification.getCode()));
                    }},

                    "ZEEVEN",
                    "activation.html",
                    null,
                    "Activez votre compte"
            );
        }
    }


    public List<Guest> contacts(final GuestType guestType) {
        switch (guestType) {
            case LOCAL -> {
                return this.getLocalGuests();
            }
            case GOOGLE -> this.googleContactService.fetchContacts();
        }
        return new ArrayList<>();
    }

    private List<Guest> getLocalGuests() {
        final UserAccount authenticatedUser = this.profileService.getAuthenticateUser();
        final List<Guest> contacts = authenticatedUser.getContacts();

        return contacts.stream().map(userAccount -> {
            final Guest guest = new Guest();
            BeanUtils.copyProperties(userAccount, guest);
            return guest;
        }).collect(Collectors.toList());
    }

    public void deleteContact(final String id) {
        final UserAccount authenticatedUser = this.profileService.getAuthenticateUser();
        final List<Guest> userAccounts = authenticatedUser.getContacts().stream().filter(userAccount -> !userAccount.getPublicId().equals(id)).collect(Collectors.toList());
        authenticatedUser.setContacts(userAccounts);
        this.utilisateurRepository.save(authenticatedUser);
    }

    public void addGuest(final Guest contact) {
        final UserAccount authenticatedUser = this.profileService.getAuthenticateUser();
        final List<Guest> contacts = authenticatedUser.getContacts();
        this.utilitaireService.checkIfAccountIsInList(contacts, contact);


        final String publicId = RandomStringUtils.randomNumeric(8).toLowerCase(Locale.ROOT);
        final String id = UUID.randomUUID().toString();
        contact.setId(id);
        contact.setPublicId(publicId);
        contact.setTrial(true);

        contacts.add(contact);
        authenticatedUser.setContacts(contacts);
        this.utilisateurRepository.save(authenticatedUser);
    }

    public String getAuthorizations() {
        return this.googleContactService.getAuthorisations();
    }

    public void addGuests(final List<Guest> guests) {

        guests.forEach(guest -> {
            try {
                this.addGuest(guest);
            } catch (final Exception exception) {
                log.error(null, exception);
            }
        });
    }
}
