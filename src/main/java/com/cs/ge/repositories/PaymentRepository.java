package com.cs.ge.repositories;


import com.cs.ge.entites.ApplicationPayment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentRepository extends MongoRepository<ApplicationPayment, String> {// Il VA STOCKER UN ELEMENT DE TYPE ADRESSE QUI A UNE CLE PRIMAIRE DE TYPE STRING

    Optional<ApplicationPayment> findByProviderSessionId(String id);
}

