package com.cs.ge.services.shared;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SharedService {
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public String toSlug(final String input) {
        final String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        final String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        final String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
