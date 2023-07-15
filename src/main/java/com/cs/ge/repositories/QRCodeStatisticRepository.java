package com.cs.ge.repositories;


import com.cs.ge.entites.QRCodeStatistic;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.stream.Stream;

public interface QRCodeStatisticRepository extends MongoRepository<QRCodeStatistic, String> {

    long countByQrCode(String qrCode);

    Stream<QRCodeStatistic> findByQrCode(String qrCode);
}

