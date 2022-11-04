package com.cs.ge.changelog;

import com.cs.ge.entites.Utilisateur;
import com.cs.ge.enums.Role;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

import static java.lang.Boolean.TRUE;

@ChangeUnit(order = "004", id = "createAPP", author = "achille")
public class AppZeevenChangeLog {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final MongoTemplate mongoTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public AppZeevenChangeLog(final MongoTemplate mongoTemplate,
                              final BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Execution
    public void createUser() {
        final Utilisateur app = new Utilisateur();
        app.setRole(Role.CUSTOMER);
        app.setFirstName("app");
        app.setLastName("zeeven");
        app.setEmail("app.maxime@zeeven.fr");
        app.setEnabled(TRUE);
        app.setPassword(this.bCryptPasswordEncoder.encode("APP@ZeeVen"));
        UUID serviceId = UUID.randomUUID();
        this.logger.info("serviceId key {}", serviceId);
        app.setServiceId(serviceId);
        UUID serviceKey = UUID.randomUUID();
        this.logger.info("serviceKey key {}", serviceKey);
        app.setServiceKey(serviceKey);

        this.mongoTemplate.save(app);

    }

    @RollbackExecution
    public void rollback() {
    }

    @RollbackBeforeExecution
    public void rollbackBeforeExecution() {
    }

}
