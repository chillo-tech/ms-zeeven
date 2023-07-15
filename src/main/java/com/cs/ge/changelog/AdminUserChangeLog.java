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

@ChangeUnit(order = "003", id = "createAdmin", author = "achille")
public class AdminUserChangeLog {
    private final MongoTemplate mongoTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public AdminUserChangeLog(final MongoTemplate mongoTemplate,
                              final BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.mongoTemplate = mongoTemplate;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }


    @Execution
    public void createUser() {
        final UserAccount admin = new UserAccount();
        admin.setRole(Role.ADMIN);
        admin.setFirstName("admin");
        admin.setLastName("admin");
        admin.setEmail("bonjour.zeeven@gmail.com");
        admin.setEnabled(TRUE);
        admin.setPassword(this.bCryptPasswordEncoder.encode("events"));
        this.mongoTemplate.save(admin);

    }

    @RollbackExecution
    public void rollback() {
    }

    @RollbackBeforeExecution
    public void rollbackBeforeExecution() {
    }

}
