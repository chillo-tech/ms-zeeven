package com.cs.ge.services.qrcode;

import com.cs.ge.entites.QRCodeEntity;
import com.cs.ge.utils.UtilitaireService;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.glxn.qrgen.core.scheme.SMS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.cs.ge.enums.QRCodeType.EMAIL;
import static com.cs.ge.enums.QRCodeType.WHATSAPP;

@Slf4j
@Component
public class MessageQrCode extends SharedQRCodeService {

    private final String waEndpoint;

    public MessageQrCode(final UtilitaireService utilitaireService, final Environment environment, @Value("${providers.whatsapp.api-endpoint}") final String waEndpoint) {
        super(utilitaireService, environment);
        this.waEndpoint = waEndpoint;
    }

    @Override
    public Map<String, Object> qrCodeParamsFromType(final QRCodeEntity qrCodeEntity, final String imagesHost, final String imagesRootfolder, final String imagesFolder) {

        final Map<String, Object> params = super.qrCodeParamsFromType(qrCodeEntity, imagesHost, imagesRootfolder, imagesFolder);
        final SMS sms = new SMS();
        final String phoneNumber = String.format("%s%s", qrCodeEntity.getData().get("phoneIndex"), qrCodeEntity.getData().get("phone"));
        sms.setNumber(phoneNumber);
        final String text = qrCodeEntity.getData().get("text");
        sms.setSubject(text);
        String message = sms.generateString();
        if (WHATSAPP.equals(qrCodeEntity.getType())) {
            message = String.format("%s/%s", this.waEndpoint, phoneNumber);
            if (!Strings.isNullOrEmpty(text)) {
                message = String.format("%s?text=%s", message, URLEncoder.encode(text, StandardCharsets.UTF_8));
            }

        }
        if (EMAIL.equals(qrCodeEntity.getType())) {
            message = String.format("mailto:%s", qrCodeEntity.getData().get("email"));
            if (!Strings.isNullOrEmpty(text)) {
                message = String.format("%s?body=%s", message, URLEncoder.encode(text, StandardCharsets.UTF_8));
            }
        }
        params.put("finalContent", message);
        params.put("tempContent", params.get("finalContent"));
        return params;
    }
}
