package com.cs.ge.repositories;


import com.cs.ge.entites.QRCodeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QRCodeRepository extends MongoRepository<QRCodeEntity, String> {// Il VA STOCKER UN ELEMENT DE TYPE ADRESSE QUI A UNE CLE PRIMAIRE DE TYPE STRING

    Optional<QRCodeEntity> findByPublicId(String publicId);

    List<QRCodeEntity> findByAuthor(String author);
}

