package com.cs.ge.services;

import com.cs.ge.exception.ApplicationException;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@NoArgsConstructor
@Component
public class ValidationService {
    public static void validationChaine(final String chaine) {
        if (chaine == null || chaine.trim().isEmpty()) {
            throw new ApplicationException("Champs obligatoire");
        }
    }

    public static boolean checkEmail(final String username) {
        final String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
                "[a-zA-Z0-9_+&*-]+)*@" +
                "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                "A-Z]{2,7}$";
        final Pattern pat = Pattern.compile(emailRegex);
        if (username == null) {
            return false;
        }

        return pat.matcher(username).matches();
    }

    public static boolean checkPhone(final String username) {
        final String numberRegex = "(6|5|0|9)?[0-9]{9}";
        final Pattern pat = Pattern.compile(numberRegex);
        if (username == null) {
            return false;
        }
        return pat.matcher(username).matches();
    }

}
