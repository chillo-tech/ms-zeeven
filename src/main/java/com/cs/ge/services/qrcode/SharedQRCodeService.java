package com.cs.ge.services.qrcode;

import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.utils.UtilitaireService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@AllArgsConstructor
public abstract class SharedQRCodeService {
    private final UtilitaireService utilitaireService;

    protected Map<String, Object> qrCodeParamsFromType(final QRCodeEntity qrCodeEntity, final String imagesHost, final String imagesRootfolder, final String imagesFolder) {
        final Map<String, Object> params = new HashMap<>();
        final String slug = this.slugFromType(qrCodeEntity);

        params.put("publicId", String.format("%s", slug));
        final String tempContent = String.format("%s/go-to/%s", imagesHost, slug);
        final String location = String.format("%s/%s/qr-code/%s.jpg", imagesRootfolder, imagesFolder, slug);
        params.put("location", location);
        params.put("name", slug.replaceAll("-", " "));
        params.put("tempContent", tempContent);
        return params;
    }

    private String slugFromType(final QRCodeEntity qrCodeEntity) {
        return switch (qrCodeEntity.getType()) {
            case LINK -> this.utilitaireService.makeSlug(
                    qrCodeEntity.getData().get("url")
                            .replaceAll("https", "")
                            .replaceAll("http", "")
                            .replaceAll("www", "")
            );
            case WIFI -> this.utilitaireService.makeSlug(
                    qrCodeEntity.getData().get("name")
            );
            case VCARD -> this.utilitaireService.makeSlug(
                    String.format("%s %s", qrCodeEntity.getData().get("firstName"), qrCodeEntity.getData().get("lastName"))
            );
            default -> null;
        };
    }

    protected String content(final QRCodeEntity qrCodeEntity) {
        log.info(
                "QRCODE {} est pour {} et est de type {}",
                qrCodeEntity.getPublicId(), qrCodeEntity.getFinalContent(), qrCodeEntity.getType());
        return qrCodeEntity.getFinalContent();

    }
}
