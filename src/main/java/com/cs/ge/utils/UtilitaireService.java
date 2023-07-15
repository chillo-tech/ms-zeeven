package com.cs.ge.utils;

import com.cs.ge.exception.ApplicationException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UtilitaireService {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w_-]");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}&&[^-]]");

    public String makeSlug(String input) {
        String noseparators = SEPARATORS.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noseparators, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
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
}
