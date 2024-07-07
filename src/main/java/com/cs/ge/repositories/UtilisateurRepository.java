package com.cs.ge.repositories;

import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends MongoRepository<UserAccount, String> {
    Optional<UserAccount> findByEmail(String username);
    List<UserAccount> findByRole(Role role);

    Optional<UserAccount> findByPhoneIndexAndPhone(String string, String username);

    Optional<UserAccount> findByPhone(String phone);


    @Query("{$or : [{$and: [{phoneIndex: ?1},{phone: ?2}]}, {email : ?0}]}")
    Optional<UserAccount> findByPhoneOrMail(String mail, String phoneIndex, String phone);


    Optional<UserAccount> findBySecretsServiceId(String serviceId);
}


