package com.cs.ge.utilitaire;

import com.cs.ge.exception.ApplicationException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class UtilitaireService {
    // private final UtilisateurRepository utilisateurRepository;

    //public UtilitaireService(final UtilisateurRepository utilisateurRepository) {
    //     this.utilisateurRepository = utilisateurRepository;
    //   }


    public static void validationChaine(final String chaine) {
        if (chaine == null || chaine.trim().isEmpty()) {
            throw new ApplicationException("Champs obligatoire");
        }
    }

    private static final Pattern NONLATIN = Pattern.compile("[^\\w_-]");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\p{Punct}&&[^-]]");

    public static String makeSlug(String input) {
        String noseparators = SEPARATORS.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noseparators, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }
}
