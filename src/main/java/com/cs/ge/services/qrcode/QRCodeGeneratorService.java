package com.cs.ge.services.qrcode;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.entites.QRCodeStatistic;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.QRCodeType;
import com.cs.ge.feign.FeignIPGeolocation;
import com.cs.ge.repositories.QRCodeRepository;
import com.cs.ge.repositories.QRCodeStatisticRepository;
import com.cs.ge.services.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cs.ge.utils.Data.QRCODE_STATISTICS_KEY;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Component
public class QRCodeGeneratorService {
    private final int WIDTH = 500;
    private final int HEIGHT = 500;

    private final ProfileService profileService;
    private final String imagesHost;
    private final String imagesFolder;
    private final String imagesRootfolder;
    private final QRCodeRepository qrCodeRepository;
    private final QRCodeStatisticRepository qrCodeStatisticRepository;
    private final LinkQrCode linkQrCode;
    private final WIFIQrCode wifiQrCode;
    private final VcardQrCode vcardQrCode;
    private final FeignIPGeolocation ipGeolocation;

    public QRCodeGeneratorService(
            @Value("${resources.images.folder}") final String imagesFolder,
            @Value("${resources.images.host}") final String imagesHost,
            @Value("${resources.images.root}") final String imagesRootfolder,
            final QRCodeStatisticRepository qrCodeStatisticRepository,
            final ProfileService profileService,
            final QRCodeRepository qrCodeRepository,
            final LinkQrCode linkQrCode,
            final WIFIQrCode wifiQrCode,
            final VcardQrCode vcardQrCode,
            final FeignIPGeolocation ipGeolocation
    ) {
        this.profileService = profileService;
        this.qrCodeRepository = qrCodeRepository;
        this.imagesHost = imagesHost;
        this.imagesFolder = imagesFolder;
        this.imagesRootfolder = imagesRootfolder;
        this.qrCodeStatisticRepository = qrCodeStatisticRepository;
        this.wifiQrCode = wifiQrCode;
        this.linkQrCode = linkQrCode;
        this.vcardQrCode = vcardQrCode;
        this.ipGeolocation = ipGeolocation;
    }

    public void guestQRCODE(final Event event, final Guest guest) {
        final Map<String, String> elements = new HashMap();
        elements.put("guest", "guest");
        elements.put("event", "event");
        //this.generateQRCODEWithQRCodeWriter(elements);
        this.generateQRCODEWithQRGen(event, guest);
    }

    private void generateQRCODEWithQRGen(final Event event, final Guest guest) {
        final String location = format("%s/%s/events/%s/tickets/%s.jpg", this.imagesRootfolder, this.imagesFolder, event.getPublicId(), guest.getPublicId());
        log.info("IMAGE LOCATION " + location);
        final String imageContent = format("event-%s,guest-%s", event.getPublicId(), guest.getPublicId());

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
        } catch (final WriterException | IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String generate(final QRCodeEntity qrCodeEntity) throws IOException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Map<String, Object> params = this.qrCodeParamsFromType(qrCodeEntity);
        final String publicId = valueOf(params.get("publicId"));
        qrCodeEntity.setPublicId(publicId);
        qrCodeEntity.setLocation(params.get("location").toString());
        qrCodeEntity.setName(valueOf(params.get("name")));
        qrCodeEntity.setFinalContent(valueOf(params.get("finalContent")));
        File file = QRCode.from(valueOf(params.get("finalContent"))).to(ImageType.JPG).withSize(this.WIDTH, this.HEIGHT).file();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            final UserAccount author = this.profileService.loadUser(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + authentication.getName()));
            qrCodeEntity.setTempContent(valueOf(params.get("tempContent")));
            qrCodeEntity.setTrack(true);
            qrCodeEntity.setAuthor(author.getId());
            this.qrCodeRepository.findByPublicId(publicId).ifPresent(byPublicId -> qrCodeEntity.setId(byPublicId.getId()));
            this.qrCodeRepository.save(qrCodeEntity);
            file = QRCode.from(valueOf(params.get("tempContent"))).to(ImageType.JPG).withSize(this.WIDTH, this.HEIGHT).file();
        }
        return this.getFileContent(params.get("location").toString(), file);
    }

    private String getFileContent(final String location, final File file) throws IOException {
        if (!Strings.isNullOrEmpty(location)) {
            try {
                FileUtils.copyFile(file, new File(location));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
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

    public String content(final String publicId, final Map<String, String> headers) {
        final QRCodeEntity qrCodeEntity = this.qrCodeRepository.findByPublicId(publicId).orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Aucune enttité ne correspond au critères fournis"));

        String result = null;
        switch (qrCodeEntity.getType()) {
            case LINK -> result = this.linkQrCode.content(qrCodeEntity);
            case WIFI -> result = this.wifiQrCode.content(qrCodeEntity);
            case VCARD -> result = this.vcardQrCode.content(qrCodeEntity);

        }
        if (qrCodeEntity.isTrack()) {
            this.updateStatistics(qrCodeEntity, headers);
        }
        return result;

    }

    @Async
    void updateStatistics(final QRCodeEntity qrCodeEntity, final Map<String, String> headers) {
        final QRCodeStatistic.QRCodeStatisticBuilder qrCodeStatisticBuilder = QRCodeStatistic.builder().qrCode(qrCodeEntity.getId());
        qrCodeStatisticBuilder.creation(Instant.now());
        qrCodeStatisticBuilder.qrCode(qrCodeEntity.getId());
        qrCodeStatisticBuilder.language(format("%s", headers.get("accept-language")));
        headers.keySet().parallelStream().filter(QRCODE_STATISTICS_KEY::contains).forEach((key) -> {
            final String value = headers.get(key);
            if (Objects.equals(key, "host")) {
                qrCodeStatisticBuilder.host(headers.get(key));
            } else if (Objects.equals(key, "user-agent")) {
                if (value.toLowerCase().contains("android")) {
                    qrCodeStatisticBuilder.agent("android");
                } else if (value.toLowerCase().contains("ios")) {
                    qrCodeStatisticBuilder.agent("ios");
                } else {
                    qrCodeStatisticBuilder.agent("other");
                }
            } else if (Objects.equals(key, "x-forwarded-for")) {
                final Map<String, Object> result = this.ipGeolocation.ipgeo(value);
                qrCodeStatisticBuilder.ip(format("%s", result.get("ip")));
                qrCodeStatisticBuilder.city(format("%s", result.get("city")));
                qrCodeStatisticBuilder.zipcode(format("%s", result.get("zipcode")));
                qrCodeStatisticBuilder.country(format("%s", result.get("country_name")));
                qrCodeStatisticBuilder.latitude(format("%s", result.get("latitude")));
                qrCodeStatisticBuilder.longitude(format("%s", result.get("longitude")));
            }
        });
        final QRCodeStatistic qrCodeStatistic = qrCodeStatisticBuilder.build();
        this.qrCodeStatisticRepository.save(qrCodeStatistic);
    }

    public List<QRCodeEntity> search() {
        final UserAccount authenticateUser = this.profileService.getAuthenticateUser();
        final List<QRCodeEntity> byAuthor = this.qrCodeRepository.findByAuthor(authenticateUser.getId());

        return byAuthor.parallelStream().peek(qrcode -> {
            final File file = QRCode.from(qrcode.getTempContent()).to(ImageType.JPG).withSize(this.WIDTH, this.HEIGHT).file();
            try {
                qrcode.setFile(this.getFileContent(qrcode.getLocation(), file));
            } catch (final IOException e) {
                e.printStackTrace();
            }
            final long scans = this.qrCodeStatisticRepository.countByQrCode(qrcode.getId());
            qrcode.setScans(scans);
        }).collect(Collectors.toList());
    }

    public QRCodeEntity read(final String id) {
        final QRCodeEntity qrCodeEntity = this.qrCodeRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Aucune entite ne correspond aux données que vous avez transmis"));
        final long scans = this.qrCodeStatisticRepository.countByQrCode(qrCodeEntity.getId());
        qrCodeEntity.setScans(scans);
        final File file = QRCode.from(qrCodeEntity.getTempContent()).to(ImageType.JPG).withSize(this.WIDTH, this.HEIGHT).file();
        try {
            qrCodeEntity.setFile(this.getFileContent(qrCodeEntity.getLocation(), file));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return qrCodeEntity;
    }

    public Stream<QRCodeStatistic> statistics(final String id) {
        return this.qrCodeStatisticRepository.findByQrCode(id);
    }
}
