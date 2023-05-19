package com.cs.ge.services;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.repositories.QRCodeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class QRCodeGeneratorService {
    private final int WIDTH = 500;
    private final int HEIGHT = 500;

    private final String imagesHost;
    private final String imagesFolder;
    private final String imagesRootfolder;
    private final QRCodeRepository qrCodeRepository;

    public QRCodeGeneratorService(
            QRCodeRepository qrCodeRepository,
            @Value("${resources.images.folder}") String imagesFolder,
            @Value("${resources.images.host}") String imagesHost,
            @Value("${resources.images.root}") String imagesRootfolder
    ) {
        this.qrCodeRepository = qrCodeRepository;
        this.imagesHost = imagesHost;
        this.imagesFolder = imagesFolder;
        this.imagesRootfolder = imagesRootfolder;
    }

    public void guestQRCODE(Event event, Guest guest) {
        Map<String, String> elements = new HashMap();
        elements.put("guest", "guest");
        elements.put("event", "event");
        //this.generateQRCODEWithQRCodeWriter(elements);
        this.generateQRCODEWithQRGen(event, guest);
    }

    private void generateQRCODEWithQRGen(Event event, Guest guest) {
        String location = String.format("%s/%s/events/%s/tickets/%s.jpg", this.imagesRootfolder, this.imagesFolder, event.getPublicId(), guest.getPublicId());
        log.info("IMAGE LOCATION " + location);
        String imageContent = String.format("event-%s,guest-%s", event.getPublicId(), guest.getPublicId());

        File file = QRCode.from(imageContent).to(ImageType.JPG).withSize(this.WIDTH, this.HEIGHT).file();
        //FileUtils.writeByteArrayToFile(new File(location), decodedBytes);
        log.info("IMAGE LOCATION " + file.getAbsolutePath());
        try {
            FileUtils.copyFile(file, new File(location));
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("IMAGE LOCATION " + file.getAbsolutePath());


    }

    private String generateQRCODEWithQRCodeWriter(Map<String, String> elements) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix bitMatrix = qrCodeWriter.encode(objectMapper.writeValueAsString(elements), BarcodeFormat.QR_CODE, this.WIDTH, this.HEIGHT);
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream);
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (final WriterException ex) {
            ex.printStackTrace();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String generate(QRCodeEntity qrCodeEntity) {
        String publicId = RandomStringUtils.random(10, true, true);
        String location = String.format("%s/%s/qr-code/%s.jpg", this.imagesRootfolder, this.imagesFolder, publicId);
        log.info("IMAGE LOCATION " + location);
        String imageContent = String.format("%s/qr-code/%s", this.imagesHost, publicId);
        qrCodeEntity.setPublicId(publicId);
        qrCodeEntity.setFinalContent(imageContent);
        this.qrCodeRepository.save(qrCodeEntity);


        File file = QRCode.from(imageContent).to(ImageType.JPG).withSize(this.WIDTH, this.HEIGHT).file();
        log.info("IMAGE LOCATION " + file.getAbsolutePath());
        try {
            FileUtils.copyFile(file, new File(location));
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("IMAGE LOCATION " + file.getAbsolutePath());
        return imageContent;
    }
}
