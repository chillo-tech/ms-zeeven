package com.cs.ge.services;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.repositories.EventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
public class ImageService {

    private final String imagesFolder;
    private final String imagesHost;

    public ImageService(
            EventRepository eventRepository,
            @Value("${resources.images.folder}") String imagesFolder,
            @Value("${resources.images.host}") String imagesHost
    ) {
        this.imagesHost = imagesHost;
        this.imagesFolder = imagesFolder;
    }

    @Async
    public void saveTicketImages(Event event, Guest guest) {
        try {
            String location = String.format("%s/%s/events/%s/tickets/%s.jpg", System.getProperty("user.home"), this.imagesFolder, event.getPublicId(), guest.getProfile().getPublicId());
            log.info("IMAGE LOCATION " + location);

            byte[] decodedBytes = Base64.getDecoder().decode(guest.getTicket());
            FileUtils.writeByteArrayToFile(new File(location), decodedBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
