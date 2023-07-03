package com.cs.ge.services.qrcode;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.entites.QRCodeStatistic;
import com.cs.ge.enums.QRCodeType;
import com.cs.ge.repositories.QRCodeRepository;
import com.cs.ge.repositories.QRCodeStatisticRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Component
public class QRCodeGeneratorService {
    private final int WIDTH = 500;
    private final int HEIGHT = 500;

    private final String imagesHost;
    private final String imagesFolder;
    private final String imagesRootfolder;
    private final QRCodeRepository qrCodeRepository;
    private final QRCodeStatisticRepository qrCodeStatisticRepository;
    private final LinkQrCode linkQrCode;
    private final WIFIQrCode wifiQrCode;
    private final VcardQrCode vcardQrCode;

    public QRCodeGeneratorService(
            final QRCodeRepository qrCodeRepository,
            @Value("${resources.images.folder}") final String imagesFolder,
            @Value("${resources.images.host}") final String imagesHost,
            @Value("${resources.images.root}") final String imagesRootfolder,
            final QRCodeStatisticRepository qrCodeStatisticRepository, final LinkQrCode linkQrCode, final WIFIQrCode wifiQrCode, final VcardQrCode vcardQrCode) {
        this.qrCodeRepository = qrCodeRepository;
        this.imagesHost = imagesHost;
        this.imagesFolder = imagesFolder;
        this.imagesRootfolder = imagesRootfolder;
        this.qrCodeStatisticRepository = qrCodeStatisticRepository;
        this.wifiQrCode = wifiQrCode;
        this.linkQrCode = linkQrCode;
        this.vcardQrCode = vcardQrCode;
    }

    public void guestQRCODE(final Event event, final Guest guest) {
        final Map<String, String> elements = new HashMap();
        elements.put("guest", "guest");
        elements.put("event", "event");
        //this.generateQRCODEWithQRCodeWriter(elements);
        this.generateQRCODEWithQRGen(event, guest);
    }

    private void generateQRCODEWithQRGen(final Event event, final Guest guest) {
        final String location = String.format("%s/%s/events/%s/tickets/%s.jpg", this.imagesRootfolder, this.imagesFolder, event.getPublicId(), guest.getPublicId());
        log.info("IMAGE LOCATION " + location);
        final String imageContent = String.format("event-%s,guest-%s", event.getPublicId(), guest.getPublicId());

        final File file = QRCode.from(imageContent).to(ImageType.JPG).withSize(this.WIDTH, this.HEIGHT).file();
        //FileUtils.writeByteArrayToFile(new File(location), decodedBytes);
        log.info("IMAGE LOCATION " + file.getAbsolutePath());
        try {
            FileUtils.copyFile(file, new File(location));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        log.info("IMAGE LOCATION " + file.getAbsolutePath());


    }

    private String generateQRCODEWithQRCodeWriter(final Map<String, String> elements) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();

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

    public String generate(final QRCodeEntity qrCodeEntity) throws IOException {
        final Map<String, Object> params = this.qrCodeParamsFromType(qrCodeEntity);
        final String publicId = String.valueOf(params.get("publicId"));
        qrCodeEntity.setPublicId(publicId);
        qrCodeEntity.setName(String.valueOf(params.get("name")));
        qrCodeEntity.setFinalContent(String.valueOf(params.get("finalContent")));
        qrCodeEntity.setTempContent(String.valueOf(params.get("tempContent")));
        this.qrCodeRepository.findByPublicId(publicId).ifPresent(byPublicId -> qrCodeEntity.setId(byPublicId.getId()));
        this.qrCodeRepository.save(qrCodeEntity);
        final File file = QRCode.from(String.valueOf(params.get("tempContent"))).to(ImageType.JPG).withSize(this.WIDTH, this.HEIGHT).file();
        log.info("IMAGE LOCATION " + file.getAbsolutePath());
        try {
            FileUtils.copyFile(file, new File(String.valueOf(params.get("location"))));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        log.info("IMAGE LOCATION " + file.getAbsolutePath());
        return Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(file));
    }

    private Map<String, Object> qrCodeParamsFromType(final QRCodeEntity qrCodeEntity) {
        Map<String, Object> params = new HashMap<>();
        final QRCodeType type = qrCodeEntity.getType();
        switch (type) {
            case LINK -> params = this.linkQrCode.qrCodeParamsFromType(qrCodeEntity, this.imagesHost, this.imagesRootfolder, this.imagesFolder);
            case WIFI -> params = this.wifiQrCode.qrCodeParamsFromType(qrCodeEntity, this.imagesHost, this.imagesRootfolder, this.imagesFolder);
            case VCARD -> params = this.vcardQrCode.qrCodeParamsFromType(qrCodeEntity, this.imagesHost, this.imagesRootfolder, this.imagesFolder);
        }
        return params;
    }

    public String content(final String publicId) {
        final QRCodeEntity qrCodeEntity = this.qrCodeRepository.findByPublicId(publicId).orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Aucune enttité ne correspond au critères fournis"));

        String result = null;
        switch (qrCodeEntity.getType()) {
            case LINK -> result = this.linkQrCode.content(qrCodeEntity);
            case WIFI -> result = this.wifiQrCode.content(qrCodeEntity);
            case VCARD -> result = this.vcardQrCode.content(qrCodeEntity);

        }
        final QRCodeStatistic qrCodeStatistic = QRCodeStatistic.builder().qrCode(qrCodeEntity.getId()).build();
        this.qrCodeStatisticRepository.save(qrCodeStatistic);
        return result;

    }
}
