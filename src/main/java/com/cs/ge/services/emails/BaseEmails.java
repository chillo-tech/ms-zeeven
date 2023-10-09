package com.cs.ge.services.emails;

import com.cs.ge.dto.Email;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.Profile;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Civility;
import com.cs.ge.services.ProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Boolean.TRUE;

@Component
public class BaseEmails {
    private static final String LIEN_URL = "activationLink";
    private static final String LIEN_LABEL = "activationLabel";
    private static final String NOM_DESTINATAIRE = "lastName";
    private static final String PRENOM_DESTINATAIRE = "firstName";
    private static final String EMAIL_PAR_DEFAUT = "bonjour.zeeven@gmail.com";
    private static final String APPLICATION_LINK = "applicationLink";
    private static final String TITRE = "titre";
    private static final String EMAIL_DESTINATAIRE = "email";

    private final EMailContentBuilder eMailContentBuilder;
    private final ProfileService profileService;
    private final String activationUrl;
    private final String applicationHost;
    private final String newPasswordUrl;

    public BaseEmails(
            final EMailContentBuilder eMailContentBuilder,
            final ProfileService profileService, @Value("${app.host}") final String applicationHost,
            @Value("${spring.mail.activation-url}") final String activationUrl,
            @Value("${spring.mail.new-password-url}") final String newPasswordUrl
    ) {
        this.eMailContentBuilder = eMailContentBuilder;
        this.profileService = profileService;
        this.activationUrl = activationUrl;
        this.applicationHost = applicationHost;
        this.newPasswordUrl = newPasswordUrl;
    }


    public Email newEvent(final Event event) {
        final Map<String, Object> replacements = new HashMap<>();
        final UserAccount author = this.profileService.findById(event.getAuthorId());
        replacements.put(TITRE, "Votre évènement a bien été enregistré");
        replacements.put(PRENOM_DESTINATAIRE, author.getFirstName());
        replacements.put(NOM_DESTINATAIRE, author.getLastName());
        replacements.put(LIEN_URL, this.applicationHost);
        replacements.put(LIEN_LABEL, "Connectez vous à votre compte");
        replacements.put(APPLICATION_LINK, this.applicationHost);
        replacements.put(EMAIL_DESTINATAIRE, "");// event.getAuthor().getEmail());

        return this.getEmail(replacements, "new-event");

    }


    public Email newGuest(final Profile guestProfile, final Event event, final String image) {
        final Map<String, Object> replacements = new HashMap<>();
        replacements.put(TITRE, "Votre invitation");
        replacements.put(PRENOM_DESTINATAIRE, guestProfile.getFirstName());
        replacements.put(NOM_DESTINATAIRE, guestProfile.getLastName());
        replacements.put(LIEN_URL, this.applicationHost);
        replacements.put(APPLICATION_LINK, this.applicationHost);
        replacements.put(EMAIL_DESTINATAIRE, guestProfile.getEmail());
        if (guestProfile.getCivility().equals(Civility.MR)) {
            replacements.put("civility", "Mr");
        }
        if (guestProfile.getCivility().equals(Civility.MR_MRS)) {
            replacements.put("civility", "Mr & Mme");
        }
        if (guestProfile.getCivility().equals(Civility.MRS)) {
            replacements.put("civility", "Mme");
        }
        replacements.put("event", String.format("au %s", event.getName().toLowerCase()));
        replacements.put("image", image);
        replacements.put("imageResourceName", "imageResourceName");

        return this.getEmail(replacements, "new-ticket");
    }

    private Email getEmail(final Map<String, Object> replacements, final String template) {
        final String message = this.eMailContentBuilder.getTemplate(template, replacements);
        final Email email = new Email(
                EMAIL_PAR_DEFAUT,
                replacements.get(EMAIL_DESTINATAIRE).toString(),
                replacements.get(TITRE).toString(),
                message
        );
        email.setHtml(TRUE);
        return email;
    }
}
