package com.cs.ge.repositories;


import com.cs.ge.entites.Stock;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface StockRepository extends MongoRepository<Stock, String> {// Il VA STOCKER UN ELEMENT DE TYPE ADRESSE QUI A UNE CLE PRIMAIRE DE TYPE STRING

    List<Stock> findByUser(String user);
}

