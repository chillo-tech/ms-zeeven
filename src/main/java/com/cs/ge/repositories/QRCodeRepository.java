package com.cs.ge.repositories;


import com.cs.ge.entites.QRCodeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QRCodeRepository extends MongoRepository<QRCodeEntity, String> {// Il VA STOCKER UN ELEMENT DE TYPE ADRESSE QUI A UNE CLE PRIMAIRE DE TYPE STRING


}

