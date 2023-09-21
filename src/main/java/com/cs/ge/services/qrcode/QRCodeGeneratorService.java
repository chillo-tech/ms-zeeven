package com.cs.ge.services.qrcode;

import com.cs.ge.entites.Event;
import com.cs.ge.entites.Guest;
import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.entites.QRCodeParams;
import com.cs.ge.entites.QRCodeStatistic;
import com.cs.ge.entites.UserAccount;
import com.cs.ge.enums.QRCodeShapeType;
import com.cs.ge.enums.QRCodeType;
import com.cs.ge.feign.FeignIPGeolocation;
import com.cs.ge.repositories.QRCodeRepository;
import com.cs.ge.repositories.QRCodeStatisticRepository;
import com.cs.ge.services.ProfileService;
import com.cs.ge.services.graphics.GraphicsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import net.glxn.qrgen.javase.QRCode;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cs.ge.enums.QRCodeType.TEXT;
import static com.cs.ge.utils.Data.IMAGE_FORMAT;
import static com.cs.ge.utils.Data.QRCODE_HEIGHT;
import static com.cs.ge.utils.Data.QRCODE_STATISTICS_KEY;
import static com.cs.ge.utils.Data.QRCODE_WIDTH;
import static com.cs.ge.utils.Data.QR_CODE_SHAPE_PARAMS;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static net.glxn.qrgen.core.image.ImageType.JPG;
import static net.glxn.qrgen.core.image.ImageType.PNG;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Component
public class QRCodeGeneratorService {

    private final ProfileService profileService;
    private final String imagesHost;
    private final String imagesFolder;
    private final String imagesRootFolder;
    private final QRCodeRepository qrCodeRepository;
    private final QRCodeStatisticRepository qrCodeStatisticRepository;
    private final LinkQrCode linkQrCode;
    private final WIFIQrCode wifiQrCode;
    private final MessageQrCode messageQrCode;
    private final VcardQrCode vcardQrCode;
    private final FeignIPGeolocation ipGeolocation;
    private final GraphicsService graphicsService;

    public QRCodeGeneratorService(
            @Value("${resources.images.folder}") final String imagesFolder,
            @Value("${resources.images.host}") final String imagesHost,
            @Value("${resources.images.root}") final String imagesRootFolder,
            final QRCodeStatisticRepository qrCodeStatisticRepository,
            final ProfileService profileService,
            final QRCodeRepository qrCodeRepository,
            final LinkQrCode linkQrCode,
            final WIFIQrCode wifiQrCode,
            final MessageQrCode messageQrCode, final VcardQrCode vcardQrCode,
            final FeignIPGeolocation ipGeolocation,
            final GraphicsService graphicsService) {
        this.profileService = profileService;
        this.qrCodeRepository = qrCodeRepository;
        this.imagesHost = imagesHost;
        this.imagesFolder = imagesFolder;
        this.imagesRootFolder = imagesRootFolder;
        this.qrCodeStatisticRepository = qrCodeStatisticRepository;
        this.wifiQrCode = wifiQrCode;
        this.linkQrCode = linkQrCode;
        this.messageQrCode = messageQrCode;
        this.vcardQrCode = vcardQrCode;
        this.ipGeolocation = ipGeolocation;
        this.graphicsService = graphicsService;
    }

    public QRCodeEntity guestQRCODE(final Event event, final Guest guest) throws IOException {
        return this.generateQRCODEWithQRGen(event, guest);
    }

    private QRCodeEntity generateQRCODEWithQRGen(final Event event, final Guest guest) throws IOException {
        final String imageContent = format("%s|%s|%s", event.getName(), event.getPublicId(), guest.getPublicId());

        final QRCodeEntity qrCodeEntity = new QRCodeEntity();
        qrCodeEntity.setFinalContent(imageContent);
        qrCodeEntity.setName(imageContent);
        qrCodeEntity.setType(TEXT);
        qrCodeEntity.setData(Map.of("text", imageContent));
        final Map<String, Object> params = this.qrCodeParamsFromType(qrCodeEntity);
        final String publicId = valueOf(params.get("publicId"));
        qrCodeEntity.setPublicId(publicId);
        qrCodeEntity.setLocation(params.get("location").toString());
        qrCodeEntity.setFinalContent(valueOf(params.get("finalContent")));
        final File file = QRCode.from(valueOf(params.get("finalContent"))).to(JPG).withSize(QRCODE_WIDTH, QRCODE_HEIGHT).file();

        final String qrCodeImage = this.getFileContent(params.get("location").toString(), file);
        qrCodeEntity.setFile(qrCodeImage);
        return qrCodeEntity;
    }

    private String generateQRCODEWithQRCodeWriter(final Map<String, String> elements) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();

            final QRCodeWriter qrCodeWriter = new QRCodeWriter();
            final BitMatrix bitMatrix = qrCodeWriter.encode(objectMapper.writeValueAsString(elements), BarcodeFormat.QR_CODE, QRCODE_WIDTH, QRCODE_HEIGHT);
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream);
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (final WriterException | IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String generate(final QRCodeEntity qrCodeEntity, final boolean simulate, final Map<String, String> headers) throws IOException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        final Map<String, Object> params = this.qrCodeParamsFromType(qrCodeEntity);

        qrCodeEntity.setPublicId(valueOf(params.get("publicId")));
        qrCodeEntity.setLocation(valueOf(params.get("location")));
        qrCodeEntity.setName(valueOf(params.get("name")));
        String qrCodeContent = valueOf(params.get("finalContent"));
        qrCodeEntity.setFinalContent(valueOf(params.get("finalContent")));
        if (!simulate) {
            if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
                final UserAccount author = this.profileService.loadUser(authentication.getName()).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + authentication.getName()));
                qrCodeEntity.setTempContent(valueOf(params.get("tempContent")));
                qrCodeEntity.setTrack(true);
                qrCodeEntity.setAuthor(author.getId());
                QRCodeEntity newQRCodeEntity = qrCodeEntity;
                final String qrcodeId = headers.get("qrcode-id");
                if (!Strings.isNullOrEmpty(qrcodeId)) {
                    final Optional<QRCodeEntity> qrCodeInDatabase = this.qrCodeRepository.findById(qrcodeId);
                    newQRCodeEntity = qrCodeInDatabase.orElse(qrCodeEntity);
                    newQRCodeEntity.setName(qrCodeEntity.getName());
                    newQRCodeEntity.setLocation(qrCodeEntity.getLocation());
                    newQRCodeEntity.setFinalContent(qrCodeEntity.getFinalContent());
                    newQRCodeEntity.setTempContent(qrCodeEntity.getTempContent());
                    newQRCodeEntity.setType(qrCodeEntity.getType());
                }
                this.qrCodeRepository.save(newQRCodeEntity);
                qrCodeContent = qrCodeEntity.getTempContent();
            }
        }
        final File file = QRCode.from(qrCodeContent)
                .withColor(0xFF000000, 0xFFFFFFFF)
                .to(PNG)
                .withSize(QRCODE_WIDTH, QRCODE_HEIGHT)
                .file();
        String fileContent = this.getFileContent(valueOf(params.get("location")), file);
        final QRCodeParams qrCodeEntityParams = qrCodeEntity.getParams();
        if (qrCodeEntityParams != null && !QRCodeShapeType.NONE.equals(qrCodeEntityParams.getShape().getSelected())) {
            fileContent = this.getShapedQRCODE(fileContent, qrCodeEntityParams);
        }

        return fileContent;
    }

    private String getShapedQRCODE(String fileContent, final QRCodeParams qrCodeEntityParams) throws IOException {
        final Map<String, List<Integer>> coordinatesMap = QR_CODE_SHAPE_PARAMS.get(qrCodeEntityParams.getShape().getSelected());

        final List<Integer> shapeSize = coordinatesMap.get("shape");
        final int shapeWidth = shapeSize.get(0);
        final int shapeHeight = shapeSize.get(1);

        final BufferedImage shapedQrCode = new BufferedImage(shapeWidth, shapeHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D graphics2D = shapedQrCode.createGraphics();

        try {
            final String text = qrCodeEntityParams.getShape().getText();
            final List<Integer> textCoords = coordinatesMap.get("text");
            final byte[] bytes = Base64.getDecoder().decode(fileContent);
            final BufferedImage qrcode = ImageIO.read(new ByteArrayInputStream(bytes));
            graphics2D.setColor(Color.decode(qrCodeEntityParams.getShape().getBgColor()));
            graphics2D.fillRect(0, 0, shapeWidth, shapeHeight);

            final List<Integer> imageCoords = coordinatesMap.get("image");
            graphics2D.drawImage(qrcode, imageCoords.get(0), imageCoords.get(1), imageCoords.get(2), imageCoords.get(3), null);

            graphics2D.setColor(Color.decode(qrCodeEntityParams.getShape().getTextColor()));
            int fontSize = 40;
            if (text.length() > shapeWidth) {
                fontSize = (shapeWidth / text.length()) * 40;
            }

            final Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
            graphics2D.setFont(font);


            this.graphicsService.centerString(
                    graphics2D,
                    new Rectangle(0, 0, shapeWidth, shapeHeight),
                    qrCodeEntityParams.getShape().getText(),
                    font,
                    textCoords.get(0),
                    textCoords.get(1)
            );
            graphics2D.dispose();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(shapedQrCode, IMAGE_FORMAT, baos);
            fileContent = Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (final Exception e) {
            e.printStackTrace();
        }
        return fileContent;
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
            case LINK, TEXT, PHONE -> params = this.linkQrCode.qrCodeParamsFromType(qrCodeEntity, this.imagesHost, this.imagesRootFolder, this.imagesFolder);
            case SMS, WHATSAPP, EMAIL -> params = this.messageQrCode.qrCodeParamsFromType(qrCodeEntity, this.imagesHost, this.imagesRootFolder, this.imagesFolder);
            case WIFI -> params = this.wifiQrCode.qrCodeParamsFromType(qrCodeEntity, this.imagesHost, this.imagesRootFolder, this.imagesFolder);
            case VCARD -> params = this.vcardQrCode.qrCodeParamsFromType(qrCodeEntity, this.imagesHost, this.imagesRootFolder, this.imagesFolder);
        }
        return params;
    }

    public String content(final String publicId, final Map<String, String> headers) {
        final QRCodeEntity qrCodeEntity = this.qrCodeRepository.findByPublicId(publicId).orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "Aucune enttité ne correspond au critères fournis"));

        String result = null;
        switch (qrCodeEntity.getType()) {
            case LINK, TEXT, PHONE -> result = this.linkQrCode.content(qrCodeEntity);
            case SMS, WHATSAPP, EMAIL -> result = this.messageQrCode.content(qrCodeEntity);
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
            final File file = QRCode.from(qrcode.getTempContent()).to(JPG).withSize(QRCODE_WIDTH, QRCODE_HEIGHT).file();
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
        final File file = QRCode.from(qrCodeEntity.getTempContent()).to(JPG).withSize(QRCODE_WIDTH, QRCODE_HEIGHT).file();
        try {
            qrCodeEntity.setFile(this.getFileContent(qrCodeEntity.getLocation(), file));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return qrCodeEntity;
    }

    public void delete(final String id) {
        final QRCodeEntity qrCodeEntity = this.qrCodeRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Aucune entite ne correspond aux données que vous avez transmis"));
        this.qrCodeRepository.delete(qrCodeEntity);
    }

    public void patch(final String id, final QRCodeEntity qrCodeEntity) {
        final QRCodeEntity qrCodeEntityInBDD = this.qrCodeRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Aucune entite ne correspond aux données que vous avez transmis"));
        BeanUtils.copyProperties(qrCodeEntity, qrCodeEntityInBDD, getNullPropertyNames(qrCodeEntity));
        this.qrCodeRepository.save(qrCodeEntityInBDD);
    }

    public Stream<QRCodeStatistic> statistics(final String id) {
        return this.qrCodeStatisticRepository.findByQrCode(id);
    }

    public static String[] getNullPropertyNames(final Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        final java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        final Set<String> emptyNames = new HashSet<String>();
        for (final java.beans.PropertyDescriptor pd : pds) {
            final Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }

        final String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }
}
