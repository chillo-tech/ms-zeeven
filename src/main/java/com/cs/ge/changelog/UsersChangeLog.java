package com.cs.ge.changelog;

import com.cs.ge.entites.Utilisateur;
import com.cs.ge.enums.Role;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static java.lang.Boolean.TRUE;


@ChangeUnit(order = "001", id = "createUser", author = "athena")
public class UsersChangeLog {
    private final MongoTemplate mongoTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UsersChangeLog(final MongoTemplate mongoTemplate,
                          final BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Execution
    public void createUser() {
        final Utilisateur achille = new Utilisateur();
        achille.setRole(Role.ADMIN);
        achille.setFirstName("achille");
        achille.setLastName("Mbougueng");
        achille.setEmail("achille.mbougueng@gmail.com");
        achille.setEnabled(TRUE);
        achille.setPassword(this.bCryptPasswordEncoder.encode("events"));
        this.mongoTemplate.save(achille);

        final Utilisateur athena = new Utilisateur();
        athena.setRole(Role.ADMIN);
        athena.setFirstName("athena");
        athena.setLastName("Kuitsouc");
        athena.setEmail("kuitsdan@gmail.com");
        athena.setEnabled(TRUE);
        athena.setPassword(this.bCryptPasswordEncoder.encode("events"));
        this.mongoTemplate.save(athena);

    }

    @RollbackExecution
    public void rollback() {
    }

}
