package com.cs.ge.utils;

import com.cs.ge.entites.Guest;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.repositories.UtilisateurRepository;
import com.google.common.base.Strings;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class UtilitaireService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w_-]");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}&&[^-]]");
    private final UtilisateurRepository utilisateurRepository;

    public UtilitaireService(final UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public String makeSlug(final String input) {
        final String noseparators = SEPARATORS.matcher(input).replaceAll("-");
        final String normalized = Normalizer.normalize(noseparators, Normalizer.Form.NFD);
        final String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    public static void validationChaine(final String chaine) {
        if (chaine == null || chaine.trim().isEmpty()) {
            throw new ApplicationException("Champs obligatoire");
        }
    }


    public static boolean valEmail(final String username) {
        final String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";
        final Pattern pat = Pattern.compile(emailRegex);
        if (username == null) {
            return false;
        }

        final boolean resultat = pat.matcher(username).matches();
        return resultat;
    }

    public static boolean valNumber(final String username) {
        final String numberRegex = "(6|5|0|9)?[0-9]{9}";
        final Pattern pat = Pattern.compile(numberRegex);
        if (username == null) {
            return false;
        }
        final boolean resultat = pat.matcher(username).matches();
        return resultat;
    }

    public void checkAccount(final UserAccount userAccount) {
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
                throw new ApplicationException("Cet email est déjà utilsé. Si vous avez déjà un compte, connectez vous.");
            }
        }
        if (userAccount.getPhoneIndex() != null && userAccount.getPhone() != null) {
            final Optional<UserAccount> userByPhone = this.utilisateurRepository.findByPhoneIndexAndPhone(userAccount.getPhoneIndex(), userAccount.getPhone());
            if (userByPhone.isPresent()) {
                throw new ApplicationException("Ce téléphone est déjà utilsé. Si vous avez déjà un compte, connectez vous.");
            }
        }
    }


    public void checkIfAccountIsInList(final List<Guest> contacts, final Guest contact) {
        if (!Strings.isNullOrEmpty(contact.getEmail())) {
            final Optional<Guest> userByEmail = contacts.stream().filter(c -> !Strings.isNullOrEmpty(c.getEmail())).filter(c -> c.getEmail().equalsIgnoreCase(contact.getEmail())).findFirst();
            if (userByEmail.isPresent()) {
                throw new ApplicationException("Cet email est déjà utilsé");
            }
        }
        if (!Strings.isNullOrEmpty(contact.getPhoneIndex()) && !Strings.isNullOrEmpty(contact.getPhone())) {
            final Optional<Guest> userByPhone = contacts.stream().filter(c -> (!Strings.isNullOrEmpty(c.getPhoneIndex()) && !Strings.isNullOrEmpty(c.getPhone()))).filter(c -> (c.getPhone().equalsIgnoreCase(contact.getPhone()) && c.getPhoneIndex().equalsIgnoreCase(contact.getPhoneIndex()))).findFirst();
            if (userByPhone.isPresent()) {
                throw new ApplicationException("Ce téléphone est déjà utilsé");
            }
        }
    }
}
