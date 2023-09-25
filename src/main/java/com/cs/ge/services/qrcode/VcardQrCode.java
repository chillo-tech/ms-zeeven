package com.cs.ge.services.qrcode;

import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.utils.UtilitaireService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class VcardQrCode extends SharedQRCodeService {
    final String PATTERN =
            "BEGIN:VCARD\r\n" +
                    "VERSION:3.0\r\n" +
                    "N:%s;%s;;%s;\r\n" +
                    "FN:%s\r\n" +
                    "ORG:%s\r\n" +
                    "TITLE:%s\r\n" +
                    "PHOTO;VALUE#URI;TYPE#GIF:%s\r\n" +
                    "TEL;TYPE#CELL,VOICE:%s\r\n" +
                    "TEL;TYPE#HOME,VOICE:%s\r\n" +
                    "ADR;TYPE#WORK,PREF:;;%s\r\n" +
                    "LABEL;TYPE#WORK,PREF:%s\r\n" +
                    "EMAIL:%s\r\n" +
                    "URL:%s\r\n" +
                    "END:VCARD";

    public VcardQrCode(final UtilitaireService utilitaireService, final Environment environment) {
        super(utilitaireService, environment);
    }

    @Override
    public Map<String, Object> qrCodeParamsFromType(final QRCodeEntity qrCodeEntity, final String imagesHost, final String imagesRootfolder, final String imagesFolder) {
        final Map<String, Object> params = super.qrCodeParamsFromType(qrCodeEntity, imagesHost, imagesRootfolder, imagesFolder);
        params.put(
                "name",
                String.format("%s %s", qrCodeEntity.getData().get("firstName"), qrCodeEntity.getData().get("lastName"))
        );
        final String finalContent = String.format(
                this.PATTERN,
                qrCodeEntity.getData().get("firstName"),
                qrCodeEntity.getData().get("lastName"),
                qrCodeEntity.getData().get("civility"),
                String.format("%s %s", qrCodeEntity.getData().get("firstName"), qrCodeEntity.getData().get("lastName")),
                qrCodeEntity.getData().get("company"),
                qrCodeEntity.getData().get("job"),
                "",
                qrCodeEntity.getData().get("mobile"),
                qrCodeEntity.getData().get("phone"),
                String.format("%s %s %s", qrCodeEntity.getData().get("street"), qrCodeEntity.getData().get("zipcode"), qrCodeEntity.getData().get("city")),
                String.format("%s %s %s", qrCodeEntity.getData().get("street"), qrCodeEntity.getData().get("zipcode"), qrCodeEntity.getData().get("city")),
                qrCodeEntity.getData().get("email"),
                qrCodeEntity.getData().get("website")
        );
        params.put("finalContent", finalContent);
        return params;
    }
}
