package com.cs.ge.changelog;

import com.cs.ge.entites.Secrets;
import com.cs.ge.entites.Utilisateur;
import com.cs.ge.enums.Role;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.passay.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Boolean.TRUE;

@ChangeUnit(order = "004", id = "createAPP", author = "achille")
public class AppZeevenChangeLog {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final MongoTemplate mongoTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final List<Rule> passwordRules;

    public AppZeevenChangeLog(
            final List<Rule> passwordRules,
            final MongoTemplate mongoTemplate,
            final BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.passwordRules = passwordRules;
        this.mongoTemplate = mongoTemplate;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Execution
    public void createUser() {
        int randomNum = ThreadLocalRandom.current().nextInt(2, 6);
        CharacterRule uppercase = new CharacterRule(EnglishCharacterData.UpperCase, 14);
        CharacterRule lowercase = new CharacterRule(EnglishCharacterData.LowerCase, 14);
        CharacterRule digits = new CharacterRule(EnglishCharacterData.Digit, 14);
        PasswordGenerator passwordGenerator = new PasswordGenerator();
        final Utilisateur app = new Utilisateur();
        app.setRole(Role.CUSTOMER);
        app.setFirstName("app");
        app.setLastName("zeeven");
        app.setEmail("app.maxime@zeeven.fr");
        app.setEnabled(TRUE);
        app.setPassword(this.bCryptPasswordEncoder.encode("APP@ZeeVen"));
        String serviceId = UUID.randomUUID().toString();
        this.logger.info("serviceId key {}", serviceId);
        Secrets secrets = new Secrets();
        secrets.setServiceId(serviceId);
        String serviceKey = passwordGenerator.generatePassword(46 + randomNum, uppercase, lowercase, digits);
        this.logger.info("serviceKey id {} key {}", serviceId, serviceKey);
        secrets.setServiceKey(serviceKey);
        app.setSecrets(secrets);

        this.mongoTemplate.save(app);

    }

    @RollbackExecution
    public void rollback() {
    }

    @RollbackBeforeExecution
    public void rollbackBeforeExecution() {
    }

}
