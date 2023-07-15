package com.cs.ge.changelog;

import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Role;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackBeforeExecution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static java.lang.Boolean.TRUE;

@ChangeUnit(order = "004", id = "createKM", author = "achille")
public class KarenMaximeUserChangeLog {
    private final MongoTemplate mongoTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public KarenMaximeUserChangeLog(final MongoTemplate mongoTemplate,
                                    final BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Execution
    public void createUser() {
        final UserAccount km = new UserAccount();
        km.setRole(Role.CUSTOMER);
        km.setFirstName("KM");
        km.setLastName("WAMBE");
        km.setEmail("karen.maxime@gmail.com");
        km.setEnabled(TRUE);
        km.setPassword(this.bCryptPasswordEncoder.encode("karen&maxime"));
        this.mongoTemplate.save(km);

    }

    @RollbackExecution
    public void rollback() {
    }

    @RollbackBeforeExecution
    public void rollbackBeforeExecution() {
    }

}
