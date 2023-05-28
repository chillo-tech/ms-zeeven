package com.cs.ge.services;

import com.cs.ge.entites.UserAccount;
import com.cs.ge.entites.Verification;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.repositories.UtilisateurRepository;
import com.cs.ge.services.notifications.ASynchroniousNotifications;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.cs.ge.enums.Role.CUSTOMER;
import static com.cs.ge.utils.UtilitaireService.valEmail;
import static com.cs.ge.utils.UtilitaireService.valNumber;
import static com.cs.ge.utils.UtilitaireService.validationChaine;

@Service
public class UtilisateursService {

    private static final String ACCOUNT_NOT_EXISTS = "Aucun compte ne correspond aux critères fournis";
    private final UtilisateurRepository utilisateurRepository;
    private final VerificationService verificationService;
    private final PasswordEncoder passwordEncoder;
    private final StockService stockService;
    private final ASynchroniousNotifications asynchroniousNotifications;

    public UtilisateursService(
            final UtilisateurRepository utilisateurRepository,
            final VerificationService verificationService,
            final PasswordEncoder passwordEncoder,
            final StockService stockService,
            ASynchroniousNotifications aSynchroniousNotifications) {
        this.utilisateurRepository = utilisateurRepository;
        this.verificationService = verificationService;
        this.passwordEncoder = passwordEncoder;
        this.stockService = stockService;

        this.asynchroniousNotifications = aSynchroniousNotifications;
    }


    public void activate(final String code) {
        final Verification verification = this.verificationService.getByCode(code);
        UserAccount userAccount = verification.getUserAccount();
        userAccount = this.utilisateurRepository.findById(userAccount.getId()).orElseThrow(() -> new ApplicationException("aucun userAccount pour ce code"));
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
    }

    public void validationUsername(final String username) {
        final Optional<UserAccount> exist = this.utilisateurRepository.findByEmail(username);
        if (exist.isPresent()) {
            throw new ApplicationException("Username existe déjà");
        }
    }

    public UserAccount readOrSave(UserAccount userAccount) {
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


    public void inscription(final UserAccount userAccount) throws MessagingException, IOException {
        valEmail(userAccount.getUsername());
        valNumber(userAccount.getUsername());
        this.checkAccount(userAccount);
        userAccount.setRole(CUSTOMER);
        final String encodedPassword = this.passwordEncoder.encode(userAccount.getPassword());
        userAccount.setPassword(encodedPassword);
        this.utilisateurRepository.save(userAccount);
        final Verification verification = this.verificationService.createCode(userAccount);

        if (userAccount.getEmail() != null) {
            //this.asynchroniousNotifications.sendEmail(userAccount, verification.getCode());
        }
    }

    private void checkAccount(final UserAccount userAccount) {
        if (
                (userAccount.getEmail() == null || userAccount.getEmail().trim().isEmpty())
                        && (userAccount.getPhone() == null || userAccount.getPhone().trim().isEmpty())
        ) {
            throw new ApplicationException("Veuillez saisir l'email ou votre téléphone");
        }

        validationChaine(userAccount.getFirstName());
        validationChaine(userAccount.getLastName());
        valEmail(userAccount.getEmail());
        valNumber(userAccount.getPhone());
        if (userAccount.getEmail() != null) {
            final Optional<UserAccount> userByEmail = this.utilisateurRepository.findByEmail(userAccount.getEmail());
            if (userByEmail.isPresent()) {
                throw new IllegalArgumentException("Cet email est déjà utilsé. Si vous avez déjà un compte, connectez vous.");
            }
        }
        if (userAccount.getPhoneIndex() != null && userAccount.getPhone() != null) {
            final Optional<UserAccount> userByPhone = this.utilisateurRepository.findByPhoneIndexAndPhone(userAccount.getPhoneIndex(), userAccount.getPhone());
            if (userByPhone.isPresent()) {
                throw new IllegalArgumentException("Ce téléphone est déjà utilsé. Si vous avez déjà un compte, connectez vous.");
            }
        }
    }
}
