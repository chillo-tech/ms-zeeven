package com.cs.ge.services;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.services.whatsapp.TextMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ImageService {
    private final String imagesFolder;
    private final String imagesHost;
    private final String imagesRootfolder;
    private final TextMessageService textMessageService;

    public ImageService(
            @Value("${resources.images.folder}") String imagesFolder,
            @Value("${resources.images.host}") String imagesHost,
            @Value("${resources.images.root}") String imagesRootfolder,
            TextMessageService textMessageService
    ) {
        this.imagesHost = imagesHost;
        this.imagesFolder = imagesFolder;
        this.imagesRootfolder = imagesRootfolder;
        this.textMessageService = textMessageService;
    }

    public void saveTicketImages(Event event, Guest guest) {
        try {
            String location = String.format("%s/%s/events/%s/tickets/%s.png", this.imagesRootfolder, this.imagesFolder, event.getPublicId(), guest.getProfile().getPublicId());
            log.info("IMAGE LOCATION " + location);
            byte[] decodedBytes = Base64.getDecoder().decode(guest.getTicket());
            FileUtils.writeByteArrayToFile(new File(location), decodedBytes);
            Map<String, String> form = new HashMap();
            form.put("messaging_product", "whatsapp");
            form.put("type", "image/png");
            form.put("file", "@" + location);
            //Map<String, String> imageId = this.textMessageService.image("@" + location, "image/png", "whatsapp");
            //Map<String, String> imageId = this.textMessageService.image(form);
            //return imageId;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
