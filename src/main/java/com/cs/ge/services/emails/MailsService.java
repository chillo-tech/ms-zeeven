package com.cs.ge.services.emails;

import com.cs.ge.dto.Email;
import com.cs.ge.entites.Event;
import com.cs.ge.entites.Profile;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class MailsService {
    private final BaseEmails baseEmails;
    private final MailsSender mailSender;

    @Async
    public void newEvent(Event event) {
        Email email = this.baseEmails.newEvent(event);
        this.mailSender.send(email);
    }

    @Async
    public void newGuest(Profile guestProfile, Event event, String guestQRCODE) {
        Email email = this.baseEmails.newGuest(guestProfile, event, guestQRCODE);
        email.setImage(guestQRCODE);
        this.mailSender.send(email);
    }
}
