package com.cs.ge.repositories;


import com.cs.ge.entites.QRCodeStatistic;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QRCodeStatisticRepository extends MongoRepository<QRCodeStatistic, String> {
}

