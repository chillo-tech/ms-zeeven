package com.cs.ge.services;

import com.cs.ge.entites.UserAccount;
import com.cs.ge.entites.Verification;
import com.cs.ge.exception.ApplicationException;
import com.cs.ge.repositories.VerificationRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring5.SpringTemplateEngine;

import java.time.LocalDateTime;

@Service
public class VerificationService {
    private final VerificationRepository verificationRepository;
    private final SpringTemplateEngine templateEngine;
    private final String accountLink;

    public VerificationService(
            @Value("${spring.mail.accountLink}") final String accountLink,
            final SpringTemplateEngine templateEngine,
            final VerificationRepository verificationRepository) {
        this.accountLink = accountLink;
        this.templateEngine = templateEngine;
        this.verificationRepository = verificationRepository;
    }

    public Verification getByCode(final String code) {
        return this.verificationRepository.findByCodeAndActive(code, true).orElseThrow(
                () -> new ApplicationException("le code d'activation n'est pas disponible"));
    }

    public Verification createCode(final UserAccount userAccount) {
        final Verification verification = new Verification();
        verification.setUsername(userAccount.getUsername());
        final String randomCode = RandomStringUtils.randomNumeric(6);
        verification.setCode(randomCode);
        verification.setDateCreation(LocalDateTime.now());
        verification.setDateExpiration(LocalDateTime.now().plusMinutes(15));
        verification.setUserAccount(userAccount);
        verification.setActive(true);
        return this.verificationRepository.save(verification);
    }

    public void updateCode(final String id, final Verification verification) {
        final Verification current = this.verificationRepository.findById(id).orElseThrow(
                () -> new ApplicationException("le code d'activation n'est pas disponible"));
        current.setId(id);
        current.setActive(verification.isActive());
        this.verificationRepository.save(current);
    }

}
